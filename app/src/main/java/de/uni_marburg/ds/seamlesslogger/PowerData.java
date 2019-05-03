package de.uni_marburg.ds.seamlesslogger;

import com.orm.SugarRecord;

import java.util.Calendar;

import de.uni_marburg.ds.seamlesslogger.learner.Classifier;

class PowerData extends SugarRecord {
    private long ts;
    private boolean isCharging;
    private boolean usbCharging;
    private boolean acCharching;
    private float batteryPercentage;

    PowerData(boolean charging, boolean usb, boolean ac, float percentage) {
        ts = Calendar.getInstance().getTimeInMillis();
        isCharging = charging;
        usbCharging = usb;
        acCharching = ac;
        batteryPercentage = percentage;

        Classifier.ffillSensor("POWER_DATA_IS_CHARGING").add(new Double( charging? 1:0));
        Classifier.ffillSensor("POWER_DATA_BATTERY_PERCENTAGE").add((double) percentage);
    }
}
