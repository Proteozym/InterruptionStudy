package de.lmu.js.interruptionesm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aware.ESM

class ESMReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        Log.d("TTT", "Queue")
        if (intent?.action == ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE) {
            Log.d("ESM___", "Queue")
        }

        if (intent?.action == ESM.ACTION_AWARE_ESM_DISMISSED) {
            Log.d("ESM___", "Dismissed")
        }

        if (intent?.action == ESM.ACTION_AWARE_ESM_EXPIRED) {
            Log.d("ESM___", "Expired")
        }
    }
}