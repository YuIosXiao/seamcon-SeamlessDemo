package de.uni_marburg.ds.seamlesslogger.learner;

import java.util.List;

public class FfillSensorObject<T> extends SensorObject<T> {

    @Override
    public void add(T reading) {
        current = reading;
    }

    @Override
    public List<T> getReadings() {
        // add value and drop older values if needed
        readings.add(0, current);
        if (readings.size() > OBSERVATION_WINDOW) {
            readings.removeLast();
        }

        return readings;
    }
}