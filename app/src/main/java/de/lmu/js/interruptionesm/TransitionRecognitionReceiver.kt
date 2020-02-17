package de.lmu.js.interruptionesm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity


class TransitionRecognitionReceiver(listenInt: TransitionListener?) : BroadcastReceiver() {



    lateinit var mContext: Context

    private var listener: TransitionListener? = null

    init {
        this.listener = listenInt
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        mContext = context!!
        Log.d("DOESIT", "rec" )
        if (ActivityTransitionResult.hasResult(intent)) {
            var result: ActivityTransitionResult?  = ActivityTransitionResult.extractResult(intent)

            val type = intent?.getIntExtra("type", -1)
            val confidence = intent?.getIntExtra("confidence", 0)

            val resultProb = ActivityRecognitionResult.extractResult(intent)


            listener?.callMainActivity("type:___"+type)
            listener?.callMainActivity("confidence:___"+confidence)
            listener?.callMainActivity("resultProb:___"+resultProb)

           // val mostProbableActivity = resultProb.mostProbableActivity
            Log.d("DOESIT", "PROBABLY:"+result)

            processTransitionResult(result)
        }
    }

    fun processTransitionResult(result: ActivityTransitionResult?) {
        Log.d("DOESIT", "proc" )
        for (event in result!!.transitionEvents) {
            onDetectedTransitionEvent(event)
        }
    }

    private fun onDetectedTransitionEvent(activity: ActivityTransitionEvent) {
        Log.d("DOESIT", "bl" )
        when (activity.activityType) {
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.RUNNING,
            DetectedActivity.WALKING -> {
                // Make whatever you want with the activity
                listener?.callMainActivity("activity.activityType:___"+activity.activityType)
                Log.d("DOESIT", "onDetectedTransitionEvent" +"____" +  activity.activityType +"____" +  activity.transitionType)
            }
            else -> {
            }
        }
    }
}