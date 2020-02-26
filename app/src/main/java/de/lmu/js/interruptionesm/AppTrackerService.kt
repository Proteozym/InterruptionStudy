package de.lmu.js.interruptionesm

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log


class AppTrackerService: Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {



        val am =
            getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (processes in am.runningAppProcesses) {
            Log.d("ACT___", processes.processName) //only returns 2020-02-26 09:25:10.793 30551-30551/de.lmu.js.interruptionesm D/ACT___: de.lmu.js.interruptionesm
            if (processes.processName.equals("com.android.calculator2")) {
                Log.d("ACT___", "Sending Intent")
                sendBroadcast(Intent("LEARNING_APP_CLOSED"))
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }


}