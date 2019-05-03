package de.uni_marburg.ds.seamlesslogger;

import android.os.Build;
import android.os.PowerManager;
import com.orm.SugarRecord;
import java.util.Calendar;

class DeviceStateData extends SugarRecord {
    private long ts;
    private boolean interactive;
    private boolean idleMode;
    private boolean powerSaveMode;

    DeviceStateData(PowerManager pwr) {
        this.ts = Calendar.getInstance().getTimeInMillis();

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            this.interactive = pwr.isScreenOn();
            this.idleMode = false;
            this.powerSaveMode = false;

        } else {
            this.interactive = pwr.isInteractive();
            this.idleMode = pwr.isDeviceIdleMode();
            this.powerSaveMode = pwr.isPowerSaveMode();
        }
    }
}
