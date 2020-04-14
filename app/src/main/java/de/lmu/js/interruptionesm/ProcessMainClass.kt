package de.lmu.js.interruptionesm

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log


class ProcessMainClass {
    private fun setServiceIntent(context: Context) {
        if (serviceIntent == null) {
            serviceIntent = Intent(context, InterruptionStudyService::class.java)
        }
    }

    /**
     * launching the service
     */
    fun launchService(context: Context?) {
        if (context == null) {
            return
        }
        setServiceIntent(context)
        // depending on the version of Android we eitehr launch the simple service (version<O)
        // or we start a foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        Log.d(TAG, "ProcessMainClass: start service go!!!!")
    }

    companion object {
        val TAG = ProcessMainClass::class.java.simpleName
        private var serviceIntent: Intent? = null
    }
}