package de.uni_marburg.ds.seamlesslogger;

import com.google.android.gms.location.DetectedActivity;
import com.orm.SugarRecord;

import java.util.Calendar;

/**
 * Created by dstohr on 29.01.18.
 */

public class ActivityData extends SugarRecord {

    private final int activityconf;
    private final long ts;
    private String activity;

    ActivityData(DetectedActivity res)
    {
        this.ts = Calendar.getInstance().getTimeInMillis();
        this.activity = ActivityData.getActivityString(res.getType());
        this.activityconf = res.getConfidence();
    }

    @Override
    public String toString() {
        return "ActivityData{" +
                "activityconf=" + activityconf +
                ", activity='" + activity + '\'' +
                '}';
    }

    static String getActivityString(int detectedActivityType) {
        switch(detectedActivityType) {
            case DetectedActivity.IN_VEHICLE:
                return "IN_VEHICLE";
            case DetectedActivity.ON_BICYCLE:
                return "ON_BICYCLE";
            case DetectedActivity.ON_FOOT:
                return "ON_FOOT";
            case DetectedActivity.RUNNING:
                return "RUNNING";
            case DetectedActivity.STILL:
                return "STILL";
            case DetectedActivity.TILTING:
                return "TILTING";
            case DetectedActivity.UNKNOWN:
                return "UNKNOWN";
            case DetectedActivity.WALKING:
                return "WALKING";
            default:
                return "UNIDENTIFIED";
        }
    }
}
