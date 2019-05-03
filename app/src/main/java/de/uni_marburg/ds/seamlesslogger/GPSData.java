package de.uni_marburg.ds.seamlesslogger;

import com.orm.SugarRecord;
import java.util.Calendar;
import java.util.Locale;

import android.location.Location;

class GPSData extends SugarRecord {
    private long ts;
    private double latitude;
    private double longtitude;
    private double altitude;
    private float accuracy;
    private float speed;

    GPSData(Location loc) {
        ts = Calendar.getInstance().getTimeInMillis();
        latitude = loc.getLatitude();
        longtitude = loc.getLongitude();
        altitude = loc.getAltitude();
        accuracy = loc.getAccuracy();
        speed = loc.getSpeed();
    }

    public String toString() {
        return String.format(Locale.getDefault(), "GPS: %d, %d, %f, %f ", longtitude, latitude, accuracy, speed);
    }
}
