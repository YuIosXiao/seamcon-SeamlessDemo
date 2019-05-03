package de.uni_marburg.ds.seamlesslogger;


import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Switch;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;

import de.uni_marburg.ds.seamlesslogger.learner.Classifier;

import static de.uni_marburg.ds.seamlesslogger.DatabaseHelper.addRecord;


public class MainActivity extends AppCompatActivity {
    static Context mainContext;
    Intent loggerService;
    ArrayAdapter<String> listItemAdapter;

    Switch loggingServiceSwitch;
    ProgressBar progressBar;

    long chartTS = -1;
    LinkedList<Entry> predictionData;
    LineDataSet predictionDataSet;
    LineData predictionLineData;
    LineChart chart;

    LayoutInflater sensorListLayoutInflater;
    View sensorListPopupView;
    PopupWindow sensorListPopupWindow;
    Button sensorListPopupDismiss;

    GeckoSession geckoSession;
    String defaultUrl;

    String experimentTimestamp;
    String deviceId;

    DatabaseHelper databaseHelper;

    void loadClassifierThreaded() {
        Thread loadThread = new Thread() {
            @Override
            public void run() {
                try {
                    InputStream mlp_data = getAssets().open("data.json");
                    Log.d("seamless", getAssets().toString());
                    Classifier.initClassifier(mlp_data);
                } catch (IOException e) {
                    Log.w("seamless", "Classifier couldn't be initialized: "+e.getClass().getName()+", "+e.getMessage());
                }
            }
        };

        loadThread.start();
    }

    static Context getMainContext() {
        return MainActivity.mainContext;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        MainActivity.mainContext = getApplicationContext();
        MPTCPHelper.switchMPTCP(MPTCPHelper.MPTCPAction.MPTCP_ENABLE);

        setContentView(R.layout.activity_main);

        loadClassifierThreaded();

        //remove android doze battery optimization messing with the measurements
        //code shamelessly copied from https://stackoverflow.com/questions/32627342/how-to-whitelist-app-in-doze-mode-android-6-0
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }

        // request permissions (required in higher api levels)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String[] permissionList = new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.INTERNET
            };
            this.requestPermissions(permissionList, 1);
        }


        sensorListLayoutInflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        sensorListPopupView = sensorListLayoutInflater.inflate(R.layout.popup, null);
        sensorListPopupWindow = new PopupWindow(sensorListPopupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sensorListPopupDismiss = sensorListPopupView.findViewById(R.id.dismiss);

        // Set sensorStringList to log list view
        listItemAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_2, android.R.id.text1, new ArrayList<String>(0));
        ListView listView = sensorListPopupView.findViewById(R.id.sensor_list);
        listView.setAdapter(listItemAdapter);

        // On/Off switch for logging
        loggerService = new Intent(this, EventLoggerService.class);

        // Setting up the web view for video
        Display display = getWindowManager().getDefaultDisplay();
        Point displaySize = new Point();
        display.getSize(displaySize);

        int adjustedHeight = ((displaySize.x * 9) / 16) + 1;

        GeckoView geckoView = findViewById(R.id.player_view);
        geckoView.getLayoutParams().height = adjustedHeight;
        geckoView.requestLayout();
        final GeckoRuntimeSettings.Builder runtimeSettingsBuilder = new GeckoRuntimeSettings.Builder();
        runtimeSettingsBuilder.useContentProcessHint(true);
        GeckoRuntime geckoRuntime = GeckoRuntime.create(this.getApplicationContext(), runtimeSettingsBuilder.build());
        geckoRuntime.getSettings().setJavaScriptEnabled(true);
        geckoRuntime.getSettings().setRemoteDebuggingEnabled(true);

        geckoSession = new GeckoSession();
        geckoSession.getSettings().setBoolean(GeckoSessionSettings.USE_PRIVATE_MODE, true);
        geckoSession.getSettings().setBoolean(GeckoSessionSettings.USE_TRACKING_PROTECTION, true);

        geckoView.setSession(geckoSession, geckoRuntime);

        // loading the default html page.
        URI apkURI = (new File(MainActivity.this.getPackageResourcePath())).toURI();
        String assetsURL = "jar:" + apkURI + "!/assets/";
        defaultUrl = assetsURL + "default_webview.html";
        loadDefaultPage();

        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private void loadDefaultPage() {
        geckoSession.loadUri(defaultUrl);
    }

    private void loadPage(String url) {
        String encodedUrl;
        try {
            encodedUrl = URLEncoder.encode(url, "utf-8");
        } catch (UnsupportedEncodingException e) {
            encodedUrl = "";
        }
        geckoSession.loadUri("http://dsgw.mathematik.uni-marburg.de:5000/?experimentid=" + encodedUrl);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem showSensorListItem = menu.findItem(R.id.sensor_list_item);
        MenuItem addEventItem = menu.findItem(R.id.add_event_item);
        if (loggingServiceIsRunning()) {
            showSensorListItem.setEnabled(true);
            addEventItem.setEnabled(true);
        } else {
            showSensorListItem.setEnabled(false);
            addEventItem.setEnabled(false);
        }


        MenuItem transitionToggleItem = menu.findItem(R.id.transition_toggle_item);
        if (MPTCPHelper.mptcpTransitionEnabled) {
            transitionToggleItem.setTitle(R.string.disable_transition_text);
        } else {
            transitionToggleItem.setTitle(R.string.enable_transition_text);
        }

        MenuItem mptcpToggleItem = menu.findItem(R.id.mptcp_toggle_item);
        if (MPTCPHelper.mptcpEnabled) {
            mptcpToggleItem.setTitle(R.string.disable_mptcp_text);
        } else {
            mptcpToggleItem.setTitle(R.string.enable_mptcp_text);
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.menu, menu);

        Toolbar switchBar = findViewById(R.id.switch_toolbar);
        switchBar.setTitle("Start Experiment");
        switchBar.inflateMenu(R.menu.switch_menu);

        Menu switchMenu = switchBar.getMenu();
        MenuItem item = switchMenu.findItem(R.id.menu_switch_item);
        MenuItemCompat.setActionView(item, R.layout.switch_layout);

        chartTS = -1;

        progressBar = findViewById(R.id.prediction_progress_bar);
        progressBar.setMax(100);
        progressBar.getProgressDrawable().setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
        progressBar.setProgress(0);

        loggingServiceSwitch = findViewById(R.id.logging_switch);

        if (loggingServiceIsRunning()) {
            loggingServiceSwitch.setChecked(true);
            // startService on already running service will just update the list.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                startService(loggerService);
            } else {
                startForegroundService(loggerService);
            }

            progressBar.getProgressDrawable().setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
            progressBar.setIndeterminate(true);
        }

        loggingServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    databaseHelper = new DatabaseHelper(MainActivity.this);

                    IntentFilter eventLoggerServiceFilter = new IntentFilter(String.valueOf(EventLoggerService.class));
                    registerReceiver(eventLoggerServiceReceiver, eventLoggerServiceFilter);

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        startService(loggerService);
                    } else {
                        startForegroundService(loggerService);
                    }

                    experimentTimestamp = Calendar.getInstance().getTimeInMillis() + "";
                    loadPage(deviceId + "_" + experimentTimestamp);
                    progressBar.getProgressDrawable().setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
                    progressBar.setIndeterminate(true);

                    chartTS = -1;

                    predictionData = new LinkedList<>();
                    predictionDataSet = new LineDataSet(predictionData, "Prediction");

                    predictionDataSet.setColor(Color.GREEN);
                    predictionDataSet.setCircleColor(Color.GREEN);
                    predictionDataSet.setLineWidth(2f);
                    predictionDataSet.setCircleRadius(3f);
                    predictionDataSet.setDrawCircleHole(false);
                    predictionDataSet.setValueTextSize(0);
                    predictionDataSet.setDrawFilled(true);

                    predictionLineData = new LineData(predictionDataSet);
                    chart.setData(predictionLineData);
                    chart.getDescription().setEnabled(false);
                    chart.getLegend().setEnabled(false);
                } else {
                    stopService(loggerService);
                    unregisterReceiver(eventLoggerServiceReceiver);
                    progressBar.setIndeterminate(false);
                    progressBar.getProgressDrawable().setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
                    progressBar.setProgress(0);
                    loadDefaultPage();

                    databaseHelper.execute();
                }
            }
        });

        chart = findViewById(R.id.chart);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.add_event_item) {
            final EditText eventField = new EditText(this);
            eventField.setHint("Event ");

            new AlertDialog.Builder(this)
                    .setTitle("Add a new text event:")
                    .setView(eventField)
                    .setPositiveButton("Add Event", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            String event = eventField.getText().toString();
                            addRecord(new TextEventData(event));
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {}
                    })
                    .show();
        }

        if (id == R.id.sensor_list_item) {
            sensorListPopupDismiss.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sensorListPopupWindow.dismiss();
                }
            });

            sensorListPopupWindow.showAtLocation(sensorListPopupView, Gravity.CENTER, 0, 0);
        }

        if (id == R.id.transition_toggle_item) {
            MPTCPHelper.mptcpTransitionEnabled = !MPTCPHelper.mptcpTransitionEnabled;
        }

        if (id == R.id.mptcp_toggle_item) {
            MPTCPHelper.mptcpEnabled = !MPTCPHelper.mptcpEnabled;
            if (MPTCPHelper.mptcpEnabled) {
                MPTCPHelper.switchMPTCP(MPTCPHelper.MPTCPAction.MPTCP_ENABLE);
            } else {
                MPTCPHelper.switchMPTCP(MPTCPHelper.MPTCPAction.MPTCP_DISABLE);
            }
        }



        return super.onOptionsItemSelected(item);
    }

    boolean loggingServiceIsRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (EventLoggerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    private BroadcastReceiver eventLoggerServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("SensorStringList")) {
                ArrayList<String> in = intent.getStringArrayListExtra("SensorStringList");

                listItemAdapter.clear();
                listItemAdapter.addAll(in);
            }

            if (intent.hasExtra("Prediction")) {
                double prediction = intent.getDoubleExtra("Prediction", -1);
                if (prediction >= 0 && prediction <= 1) {
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress((int) (prediction * 100));

                    if (prediction < 0.5) {
                        progressBar.getProgressDrawable().setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);
                    } else {
                        progressBar.getProgressDrawable().setColorFilter(Color.GREEN, android.graphics.PorterDuff.Mode.SRC_IN);
                    }

                    if (chartTS == -1)
                        chartTS = Calendar.getInstance().getTimeInMillis();

                    long ts = Calendar.getInstance().getTimeInMillis() - chartTS;

                    predictionData.add(new Entry(ts, (float) prediction));
                    if (predictionData.getFirst().getX() < ts - 60*1000)
                        predictionData.removeFirst();

                    predictionDataSet.notifyDataSetChanged();
                    predictionLineData.notifyDataChanged();
                    chart.notifyDataSetChanged();
                    chart.invalidate();

                } else {
                    progressBar.getProgressDrawable().setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
                    progressBar.setIndeterminate(true);
                }
            }
        }
    };

}
