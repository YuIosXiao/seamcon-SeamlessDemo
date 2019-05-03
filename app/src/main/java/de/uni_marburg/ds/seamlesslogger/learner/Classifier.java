package de.uni_marburg.ds.seamlesslogger.learner;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.uni_marburg.ds.seamlesslogger.MPTCPHelper;

abstract class SensorObject<T> {
    static int OBSERVATION_WINDOW = 60;
    LinkedList<T> readings = new LinkedList<T>();
    T current;

    public abstract void add(T reading);
    public abstract List<T> getReadings();
}



public class Classifier {
    public static class ObservationWindowException extends Exception {
        int ob_missing;
        public ObservationWindowException(String sensor, int observations_missing) {
            super(sensor+" is missing "+observations_missing+" observations.");
        }
    }
    public static class ClassifierUninitializedException extends Exception { }
    public static class SensorMissingException extends Exception {
        public SensorMissingException(String sensor) {
            super("The "+sensor+" sensor is missing on this device.");
        }
    }

    static Map<String, SensorObject> sensors = new HashMap();
    static MLPClassifier clf = null;
    public static List<Double> predictions = new LinkedList<Double>();
    public static double prediction = -1;

    // minimum positive classifications to disable MPTCP again.
    public static final int positiveThreshold = 5;

    public static void reset() {
        sensors = new HashMap();
    }

    public static MeanSensorObject meanfulSensor(String name){
        SensorObject sensorObject = sensors.get(name);

        if (sensorObject == null) {
            sensorObject = new MeanSensorObject();
            sensors.put(name, sensorObject);

            return (MeanSensorObject) sensorObject;
        }

        if (sensorObject instanceof MeanSensorObject) {
            return (MeanSensorObject) sensorObject;
        }

        return null;
    }

    public static FfillSensorObject ffillSensor(String name){
        SensorObject sensorObject = sensors.get(name);

        if (sensorObject == null) {
            sensorObject = new FfillSensorObject();
            sensors.put(name, sensorObject);

            return (FfillSensorObject) sensorObject;
        }

        if (sensorObject instanceof FfillSensorObject) {
            return (FfillSensorObject) sensorObject;
        }

        return null;
    }

    public static DiffSensorObject diffSensor(String name){
        SensorObject sensorObject = sensors.get(name);

        if (sensorObject == null) {
            sensorObject = new DiffSensorObject();
            sensors.put(name, sensorObject);

            return (DiffSensorObject) sensorObject;
        }

        if (sensorObject instanceof DiffSensorObject) {
            return (DiffSensorObject) sensorObject;
        }

        return null;
    }

    public static LengthDiffSensorObject lengthDiffSensor(String name){
        SensorObject sensorObject = sensors.get(name);

        if (sensorObject == null) {
            sensorObject = new LengthDiffSensorObject();
            sensors.put(name, sensorObject);

            return (LengthDiffSensorObject) sensorObject;
        }

        if (sensorObject instanceof LengthDiffSensorObject) {
            return (LengthDiffSensorObject) sensorObject;
        }

        return null;
    }

    public static void initClassifier(InputStream mlp_data) {
        clf = new MLPClassifier(mlp_data);
    }

    public static double runClassification() throws ObservationWindowException, SensorMissingException, ClassifierUninitializedException {
        double[] features = DataMapper.map(sensors);

        if (clf == null) throw new ClassifierUninitializedException();

        prediction = clf.predict(features);
        predictions.add(0, prediction);

        return prediction;
    }

    public static MPTCPHelper.MPTCPAction proposeAction() {
        MPTCPHelper.MPTCPState newState;

        if (MPTCPHelper.currentState == MPTCPHelper.MPTCPState.UNKNOWN || MPTCPHelper.currentState == MPTCPHelper.MPTCPState.MPTCP_DISABLED) {
            if (prediction < 0.5)
                newState = MPTCPHelper.MPTCPState.MPTCP_ENABLED;
            else
                newState = MPTCPHelper.MPTCPState.MPTCP_DISABLED;

        } else if (predictions.size() >= positiveThreshold) {
            newState = MPTCPHelper.MPTCPState.MPTCP_DISABLED;

            for (double pred : predictions.subList(0, positiveThreshold))
                if (pred < 0.5) newState = MPTCPHelper.MPTCPState.MPTCP_ENABLED;

        } else {
            newState = MPTCPHelper.currentState;
        }

        if (newState != MPTCPHelper.currentState) {
            MPTCPHelper.currentState = newState;

            if (newState == MPTCPHelper.MPTCPState.MPTCP_DISABLED) {
                return MPTCPHelper.MPTCPAction.MPTCP_DISABLE;
            }
            else {
                return MPTCPHelper.MPTCPAction.MPTCP_ENABLE;
            }
        }

        return MPTCPHelper.MPTCPAction.NONE;
    }
}
