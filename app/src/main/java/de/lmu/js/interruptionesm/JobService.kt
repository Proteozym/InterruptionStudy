package de.lmu.js.interruptionesm

import android.app.job.JobParameters
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresApi


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class JobService : android.app.job.JobService() {

    private val TAG = JobService::class.java.simpleName
    private var restartSensorServiceReceiver: RestartServiceBroadcastReceiver? = null
    private var instance: JobService? = null
    private var jobParameters: JobParameters? = null


    override fun onStartJob(jobParameters: JobParameters): Boolean {
        val bck = ProcessMainClass()
        bck.launchService(this)
        registerRestarterReceiver()
        instance = this
        var jobParameters = jobParameters
        return false
    }

    private fun registerRestarterReceiver() {

        // the context can be null if app just installed and this is called from restartsensorservice
        // https://stackoverflow.com/questions/24934260/intentreceiver-components-are-not-allowed-to-register-to-receive-intents-when
        // Final decision: in case it is called from installation of new version (i.e. from manifest, the application is
        // null. So we must use context.registerReceiver. Otherwise this will crash and we try with context.getApplicationContext
        if (restartSensorServiceReceiver == null) restartSensorServiceReceiver = RestartServiceBroadcastReceiver()
        else try {
            unregisterReceiver(restartSensorServiceReceiver)
        } catch (e: Exception) {
            // not registered
        }
        // give the time to run
        Handler().postDelayed({ // we register the  receiver that will restart the background service if it is killed
            @Override
            fun run() {
                // we register the  receiver that will restart the background service if it is killed
                // see onDestroy of Service
            val filter = IntentFilter()
            filter.addAction("restarter")
            try {
                registerReceiver(restartSensorServiceReceiver, filter)
            } catch (e: java.lang.Exception) {
                try {
                    applicationContext.registerReceiver(restartSensorServiceReceiver, filter)
                } catch (ex: java.lang.Exception) {
                }
            }
            }
        }, 1000)
    }

    /**
     * called if Android kills the job service
     * @param jobParameters
     * @return
     */
    override fun onStopJob(jobParameters: JobParameters): Boolean {
        Log.i(TAG, "Stopping job")
        val broadcastIntent = Intent("restarter")
        sendBroadcast(broadcastIntent)
        // give the time to run
        Handler().postDelayed({ unregisterReceiver(restartSensorServiceReceiver) }, 1000)
        return false
    }
    /**
     * called when the tracker is stopped for whatever reason
     * @param context
     */
    fun stopJob(context: Context?) {
        if (instance != null && jobParameters != null) {
            try {
                instance!!.unregisterReceiver(restartSensorServiceReceiver)
            } catch (e: Exception) {
                // not registered
            }
            Log.i(TAG, "Finishing job")
            instance!!.jobFinished(jobParameters, true)
        }
    }

}