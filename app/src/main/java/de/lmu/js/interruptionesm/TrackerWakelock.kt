package de.lmu.js.interruptionesm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.os.SystemClock
import android.provider.Settings
import android.util.Log

class TrackerWakelock : Service() {
    var pm: PowerManager? = null
    var wl: WakeLock? = null
    var handler: Handler = Handler()
    private val periodicUpdate: Runnable = object : Runnable {
        //IMMEDIATELY executes the script?
        override fun run() {
            handler.postDelayed(
                this,
                100 * 1000 - SystemClock.elapsedRealtime() % 1000
            )

        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //handler.post(periodicUpdate)
        Log.d("WakeLock", "IN1")
        handler.postDelayed({
                    sendBroadcast(Intent("SESSION_TIMED_OUT"))
                    Log.d("WakeLock", "IN")
                },
            100 * 1000 - SystemClock.elapsedRealtime() % 1000
        )
        return START_STICKY
    }

    override fun onCreate() {
        pm = getSystemService(Context.POWER_SERVICE) as PowerManager?
        wl = pm!!.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "interruptionEsm::TrackerWakelock")
        wl?.acquire()
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDestroy() {
        super.onDestroy()
        wl!!.release()
    }
}