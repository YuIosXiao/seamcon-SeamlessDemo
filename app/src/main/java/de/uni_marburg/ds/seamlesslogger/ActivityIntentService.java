package de.uni_marburg.ds.seamlesslogger;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

import static de.uni_marburg.ds.seamlesslogger.DatabaseHelper.addRecord;


/**
 * Created by dstohr on 05.02.18.
 */

public class ActivityIntentService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */

    private static final String TAG = "ActivityIntentService";

    public ActivityIntentService() {
        super(TAG);
    }

    public void onHandleIntent(Intent intent) {
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

        // Get the list of the probable activities associated with the current state of the
        // device. Each activity is associated with a confidence level, which is an int between
        // 0 and 100.
        ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();
        // Log each activity.
        Log.i(TAG, "activities detected");
        for (DetectedActivity da: detectedActivities) {
            ActivityData ac = new ActivityData(da);
            Log.i(TAG, ac.toString());
            addRecord(ac);
        }
    }
}
