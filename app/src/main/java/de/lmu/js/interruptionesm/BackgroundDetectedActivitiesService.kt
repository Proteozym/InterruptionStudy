package de.lmu.js.interruptionesm

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.widget.Toast
import com.google.android.gms.location.ActivityRecognitionClient

class BackgroundDetectedActivitiesService: Service() {

    private val TAG = BackgroundDetectedActivitiesService::class.java.simpleName

    private var mIntentService: Intent? = null
    private var mPendingIntent: PendingIntent? = null
    private var mActivityRecognitionClient: ActivityRecognitionClient? = null

    var mBinder: IBinder = LocalBinder()

    class LocalBinder : Binder() {
        fun getServerInstance(): BackgroundDetectedActivitiesService? {
            return this.getServerInstance()
        }
    }

    fun BackgroundDetectedActivitiesService() {}

    override fun onCreate() {
        super.onCreate()
        mActivityRecognitionClient = ActivityRecognitionClient(this)
        mIntentService = Intent(this, DetectedActivitiesIntentService::class.java)
        mPendingIntent =
            PendingIntent.getService(this, 1, mIntentService!!, PendingIntent.FLAG_UPDATE_CURRENT)
        requestActivityUpdatesButtonHandler()
    }


    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return Service.START_STICKY
    }

    fun requestActivityUpdatesButtonHandler() {
        val task =
            mActivityRecognitionClient!!.requestActivityUpdates(
                Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                mPendingIntent
            )
        task.addOnSuccessListener {
            /*Toast.makeText(
                getApplicationContext(),
                "Successfully requested activity updates",
                Toast.LENGTH_SHORT
            )
                .show()*/
        }
        task.addOnFailureListener {
            /*Toast.makeText(
                getApplicationContext(),
                "Requesting activity updates failed to start",
                Toast.LENGTH_SHORT
            )
                .show()*/
        }
    }

    fun removeActivityUpdatesButtonHandler() {
        val task =
            mActivityRecognitionClient!!.removeActivityUpdates(
                mPendingIntent
            )
        task.addOnSuccessListener {
            /*Toast.makeText(
                getApplicationContext(),
                "Removed activity updates successfully!",
                Toast.LENGTH_SHORT
            )
                .show()*/
        }
        task.addOnFailureListener {
            /*Toast.makeText(
                getApplicationContext(), "Failed to remove activity updates!",
                Toast.LENGTH_SHORT
            ).show()*/
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeActivityUpdatesButtonHandler()
    }
}
