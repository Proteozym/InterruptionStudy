package de.lmu.js.interruptionesm

import android.app.IntentService
import android.app.Service
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

class DetectedActivitiesIntentService : IntentService(TAG) {
    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return Service.START_STICKY
    }

    override fun onHandleIntent(intent: Intent?) {
        val result = ActivityRecognitionResult.extractResult(intent)
        // Get the list of the probable activities associated with the current state of the
// device. Each activity is associated with a confidence level, which is an int between
// 0 and 100.
        val detectedActivities = result.probableActivities
        for (activity in detectedActivities) {
            Log.i(
                TAG,
                "Detected activity: " + activity.type + ", " + activity.confidence
            )
            broadcastActivity(activity)
        }
    }

    private fun broadcastActivity(activity: DetectedActivity) {
        val intent = Intent(Constants.BROADCAST_DETECTED_ACTIVITY)
        intent.putExtra("type", activity.type)
        intent.putExtra("confidence", activity.confidence)
        sendBroadcast(intent)
    }

    companion object {
        protected val TAG =
            DetectedActivitiesIntentService::class.java.simpleName
    }
}
