package de.uni_marburg.ds.seamlesslogger.learner;

import java.util.List;

public class DiffSensorObject extends SensorObject<Double> {
    Double last;

    @Override
    public void add(Double reading) {
        current = reading;
    }

    @Override
    public List<Double> getReadings() {
        if (last != null) {
            Double diff = current - last;

            // add value and drop older values if needed
            readings.add(0, diff);
            if (readings.size() > OBSERVATION_WINDOW) {
                readings.removeLast();
            }
        }

        last = current;
        return readings;
    }
}