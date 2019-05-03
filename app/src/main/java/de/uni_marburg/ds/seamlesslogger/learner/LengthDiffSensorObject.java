package de.uni_marburg.ds.seamlesslogger.learner;

import java.util.List;

public class LengthDiffSensorObject extends SensorObject<Double> {
    int currentCount = 0;
    double cur[];
    double last[];

    public LengthDiffSensorObject() {
        this.cur = new double[] {0.0, 0.0, 0.0};
    }

    @Override
    public void add(Double reading) {
        current += reading;
        currentCount++;
    }

    // mean over time (3); diff (3), length (3 -> 1)
    public void length_add(float[] data) {
        cur[0] += data[0];
        cur[1] += data[1];
        cur[2] += data[2];
        currentCount++;
    }

    @Override
    public List<Double> getReadings() {
        Double value;

        // compute mean (only needed if more than 1 value was added)
        if (currentCount > 1) {
            cur[0] /= currentCount;
            cur[1] /= currentCount;
            cur[2] /= currentCount;
        }

        if (this.last != null) {
            // compute diff & length value
            if (currentCount > 0) {
                value = Math.sqrt(Math.pow(cur[0] - last[0], 2) + Math.pow(cur[1] - last[1], 2) + Math.pow(cur[2] - last[2], 2));

            } else { // no new values are added -> diff is zero
                value = 0.0;
            }

            // add value in front and drop older values if needed
            readings.add(0, value);
            if (readings.size() > OBSERVATION_WINDOW) {
                readings.removeLast();
            }
        }

        // set next iteration values
        this.last = cur;
        this.cur = new double[] {0.0, 0.0, 0.0};
        this.currentCount = 0;

        return readings;
    }
}