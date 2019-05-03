package de.uni_marburg.ds.seamlesslogger;

import android.util.Log;

import com.orm.SugarRecord;
import java.util.Calendar;
import java.util.Locale;

import de.uni_marburg.ds.seamlesslogger.learner.Classifier;

abstract class ThreeAxisData extends SugarRecord {
    private long ts;
    private float x;
    private float y;
    private float z;

    ThreeAxisData(float[] data) {
        ts = Calendar.getInstance().getTimeInMillis();
        x = data[0];
        y = data[1];
        z = data[2];
    }

    public String toString() {
        return String.format(Locale.getDefault(), "%s: %f, %f, %f", this.getClass().toString(), x, y, z);
    }
}

class AccelerometerData extends ThreeAxisData {
    AccelerometerData(float[] data){ super(data); }
}

class GyroscopeData extends ThreeAxisData {
    GyroscopeData(float[] data){
        super(data);
        Classifier.lengthDiffSensor("GYROSCOPE_DATA_LENGTH").length_add(data);
    }
}

class MagneticFieldData extends ThreeAxisData {
    MagneticFieldData(float[] data){
        super(data);
        Classifier.meanfulSensor("MAGNETIC_FIELD_DATA_X").add((double) data[0]);
        Classifier.meanfulSensor("MAGNETIC_FIELD_DATA_Y").add((double) data[1]);
        Classifier.meanfulSensor("MAGNETIC_FIELD_DATA_Z").add((double) data[2]);
    }
}

class GravityData extends ThreeAxisData {
    GravityData(float[] data){
        super(data);
        Classifier.meanfulSensor("GRAVITY_DATA_X").add((double) data[0]);
        Classifier.meanfulSensor("GRAVITY_DATA_Y").add((double) data[1]);
        Classifier.meanfulSensor("GRAVITY_DATA_Z").add((double) data[2]);
    }
}

class LinearAccelerationData extends ThreeAxisData {
    LinearAccelerationData(float[] data){
        super(data);
        Classifier.lengthDiffSensor("LINEAR_ACCELERATION_DATA_LENGTH").length_add(data);
    }
}

class RotationData extends ThreeAxisData {
    RotationData(float[] data){
        super(data);
        Classifier.meanfulSensor("ROTATION_DATA_X").add((double) data[0]);
        Classifier.meanfulSensor("ROTATION_DATA_Y").add((double) data[1]);
        Classifier.meanfulSensor("ROTATION_DATA_Z").add((double) data[2]);
    }
}

class OrientationData extends ThreeAxisData {
    OrientationData(float[] data){
        super(data);
        Classifier.meanfulSensor("ORIENTATION_DATA_X").add((double) data[0]);
        Classifier.meanfulSensor("ORIENTATION_DATA_Y").add((double) data[1]);
        Classifier.meanfulSensor("ORIENTATION_DATA_Z").add((double) data[2]);
    }
}