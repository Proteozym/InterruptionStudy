package de.lmu.js.interruptionesm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult.extractResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener

class TransitionRecognition : TransitionRecognitionAbstract() {
    private val TAG = TransitionRecognition::class.java!!.getSimpleName()
    lateinit var mContext: Context
    lateinit var mPendingIntent: PendingIntent

    override fun startTracking(context: Context) {
        mContext = context
        launchTransitionsTracker()
    }

    override fun stopTracking() {
        if (mContext != null && mPendingIntent != null) {
            ActivityRecognition.getClient(mContext).removeActivityTransitionUpdates(mPendingIntent)
                .addOnSuccessListener(OnSuccessListener<Void> {
                    mPendingIntent.cancel()
                })
                .addOnFailureListener(OnFailureListener { e -> Log.e(TAG, "Transitions could not be unregistered: $e") })
        }
    }

    /***********************************************************************************************
     * LAUNCH TRANSITIONS TRACKER
     **********************************************************************************************/
    private fun launchTransitionsTracker() {
        val transitions = ArrayList<ActivityTransition>()

        transitions.add(ActivityTransition.Builder()
            .setActivityType(DetectedActivity.STILL)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build())

        transitions.add(ActivityTransition.Builder()
            .setActivityType(DetectedActivity.STILL)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build())

        transitions.add(ActivityTransition.Builder()
            .setActivityType(DetectedActivity.WALKING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build())

        transitions.add(ActivityTransition.Builder()
            .setActivityType(DetectedActivity.WALKING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build())


        val request = ActivityTransitionRequest(transitions)
        val activityRecognitionClient = ActivityRecognition.getClient(mContext)

        val intent = Intent(mContext, TransitionRecognitionReceiver::class.java)
        intent.action = "de.lmu.js.interruptionesm.TRANSITION_RECOGNITION"
        mPendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0)

        val task = activityRecognitionClient.requestActivityTransitionUpdates(request, mPendingIntent)
        task.addOnSuccessListener(
            object : OnSuccessListener<Void> {
                override fun onSuccess(p0: Void?) {
                    Log.d("DOESIT NOW?", "Successfully registered")
                }
            })

        task.addOnFailureListener(
            OnFailureListener { })
    }

}

