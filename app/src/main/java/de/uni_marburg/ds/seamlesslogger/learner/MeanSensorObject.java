package de.uni_marburg.ds.seamlesslogger.learner;

import java.util.List;

public class MeanSensorObject extends SensorObject<Double> {
    int currentCount = 0;

    public MeanSensorObject() {
        this.current = 0.0;
    }

    @Override
    public void add(Double reading) {
        current += reading;
        currentCount++;
    }

    @Override
    public List<Double> getReadings() {
        Double value;

        // compute value
        if (currentCount > 0) {
            value = (current / currentCount);
        } else {
            value = readings.get(0);
        }

        // reset temporary values
        current = new Double(0.0);
        currentCount = 0;

        // add value in front and drop older values if needed
        readings.add(0, value);
        if (readings.size() > OBSERVATION_WINDOW) {
            readings.removeLast();
        }

        return readings;
    }
}