package de.uni_marburg.ds.seamlesslogger;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

public class MPTCPHelper {

    public enum MPTCPState {
        MPTCP_ENABLED,
        MPTCP_DISABLED,
        UNKNOWN,
    }

    public enum MPTCPAction {
        MPTCP_ENABLE,
        MPTCP_DISABLE,
        NONE,
    }

    static boolean mptcpTransitionEnabled = true;
    static boolean mptcpEnabled = true;


    static MPTCPAction action = MPTCPAction.NONE;
    public static MPTCPState currentState = MPTCPState.UNKNOWN;

    private static void toastHelper(final String msg) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {

                Toast.makeText(MainActivity.getMainContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    static void handleTransition() {
        if (mptcpTransitionEnabled) {
            switchMPTCP(action);
        } else {
            switch (action) {
                case MPTCP_ENABLE:
                    toastHelper("Enabling MPTCP suggested, but transitions are disabled.");
                    break;
                case MPTCP_DISABLE:
                    toastHelper("Disabling MPTCP suggested, but transitions are disabled.");
                    break;
            }

            Log.d("seamless", "MPTCP transition suggested, transitions are disabled.");
        }
    }

    public static void switchMPTCP(MPTCPAction action) {
        String cmd[] = {"su", "-c", ""};

        switch (action) {
            case MPTCP_ENABLE:
                cmd[2] = "svc data enable";
                break;
            case MPTCP_DISABLE:
                cmd[2] = "svc data disable";
                break;
        }

        if (!cmd[2].equals("")) {
            try {
                Runtime runtime = Runtime.getRuntime();
                Process switch_process = runtime.exec(cmd);
                switch_process.waitFor();
                Log.d("seamless", cmd + ": " + switch_process.exitValue());

            } catch (IOException e) {
                Log.w("seamless", "MPTCP switch failed with IOException");
            } catch (InterruptedException e) {
                Log.w("seamless", "MPTCP switch failed with InterruptedException");
            }
        }
    }
}
