package de.uni_marburg.ds.seamlesslogger;

import com.orm.SugarRecord;

import java.util.Calendar;
import java.util.Locale;

import de.uni_marburg.ds.seamlesslogger.learner.Classifier;

abstract class ScalarData extends SugarRecord {
    private long ts;
    private float x;

    ScalarData(float data) {
        ts = Calendar.getInstance().getTimeInMillis();
        x = data;
    }

    public String toString() {
        return String.format(Locale.getDefault(), "%s: %f", this.getClass().toString(), x);
    }
}

class AmbientTemperatureData extends ScalarData {
    AmbientTemperatureData(float data) { super(data); }
}

class LightData extends ScalarData {
    LightData(float data) { super(data); }
}

class PressureData extends ScalarData {
    PressureData(float data) {
        super(data);
        Classifier.meanfulSensor("PRESSURE_DATA_X").add(new Double(data));
        Classifier.diffSensor("PRESSURE_DATA_DIFF").add(new Double(data));
    }
}

class RelativeHumidityData extends ScalarData {
    RelativeHumidityData(float data) { super(data); }
}

class SignificantMotionData extends ScalarData {
    SignificantMotionData(float data) { super(data); }
}

class StationaryDetectData extends ScalarData {
    StationaryDetectData(float data) { super(data); }
}

class StepDetectData extends ScalarData {
    StepDetectData(float data) { super(data); }
}

class StepCounterData extends ScalarData {
    StepCounterData(float data) {
        super(data);
        Classifier.diffSensor("STEP_COUNTER_DATA_DIFF").add(new Double(data));
    }
}

class PredictionData extends ScalarData {
    PredictionData(float data) { super(data); }
}