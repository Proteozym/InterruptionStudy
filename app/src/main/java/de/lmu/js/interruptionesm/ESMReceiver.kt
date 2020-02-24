package de.lmu.js.interruptionesm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aware.ESM
import com.aware.providers.ESM_Provider

class ESMReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        Log.d("TTT", "Queue")
        if (intent?.action == ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE) {
            //ESM_Provider().query(ESM_Provider.ESM_Data.CONTENT_URI, )
            //Check if we already had something waiting but the participant has not answered
//If we do, set the old as expired
            val pending_esms = ESM_Provider().query(//context!!.contentResolver.query(
                ESM_Provider.ESM_Data.CONTENT_URI,
                null,
                ESM_Provider.ESM_Data.STATUS + "=" + ESM.STATUS_ANSWERED,
                null,
                null
            )

            Log.d("TTT", pending_esms.toString())
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