package de.lmu.js.interruptionesm

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.*
import android.content.Context.JOB_SCHEDULER_SERVICE
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresApi


class RestartServiceBroadcastReceiver: BroadcastReceiver() {
    val TAG: String = RestartServiceBroadcastReceiver ::class.java.getSimpleName()

    fun getVersionCode(context: Context): Long {
        val pInfo: PackageInfo
        try {
            pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e(TAG, e.message)
        }
        return 0
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "about to start timer " + context.toString())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scheduleJob(context)
        } else {
            registerRestarterReceiver(context)
            val bck = ProcessMainClass()
            bck.launchService(context)
        }
    }
    companion object {

        private var jobScheduler: JobScheduler? = null
        private var restartSensorServiceReceiver: RestartServiceBroadcastReceiver? = null
        val TAG: String = RestartServiceBroadcastReceiver::class.java.getSimpleName()

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public fun scheduleJob(context: Context) {
            if (jobScheduler == null) {
                jobScheduler = context
                    .getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
            }
            val componentName = ComponentName(
                context,
                JobService::class.java
            )
            val jobInfo = JobInfo.Builder(
                1,
                componentName
            ) // setOverrideDeadline runs it immediately - you must have at least one constraint
                // https://stackoverflow.com/questions/51064731/firing-jobservice-without-constraints
                .setOverrideDeadline(0)
                .setPersisted(true).build()
            try {jobScheduler!!.schedule(jobInfo)} catch (e: java.lang.Exception) {}
        }

        fun reStartTracker(context: Context) {
            // restart the never ending service
            Log.i(TAG, "Restarting tracker")
            val broadcastIntent = Intent("restarter")
            context.sendBroadcast(broadcastIntent)
        }

        private fun registerRestarterReceiver(context: Context) {

            // the context can be null if app just installed and this is called from restartsensorservice
            // https://stackoverflow.com/questions/24934260/intentreceiver-components-are-not-allowed-to-register-to-receive-intents-when
            // Final decision: in case it is called from installation of new version (i.e. from manifest, the application is
            // null. So we must use context.registerReceiver. Otherwise this will crash and we try with context.getApplicationContext
            if (restartSensorServiceReceiver == null) restartSensorServiceReceiver =
                RestartServiceBroadcastReceiver() else try {
                context.unregisterReceiver(restartSensorServiceReceiver)
            } catch (e: java.lang.Exception) {
                // not registered
            }
            // give the time to run
            Handler().postDelayed(Runnable { // we register the  receiver that will restart the background service if it is killed
                // see onDestroy of Service
                val filter = IntentFilter()
                filter.addAction("restarter")
                try {
                    context.registerReceiver(restartSensorServiceReceiver, filter)
                } catch (e: java.lang.Exception) {
                    try {
                        context.applicationContext
                            .registerReceiver(restartSensorServiceReceiver, filter)
                    } catch (ex: java.lang.Exception) {
                    }
                }
            }, 1000)
        }
    }


}