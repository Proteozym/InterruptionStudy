package de.lmu.js.interruptionesm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import com.aware.ESM
import com.aware.providers.ESM_Provider
import com.google.gson.Gson
import de.lmu.js.interruptionesm.SessionState.Companion.esmWasDismissed
import org.json.JSONObject

class ESMReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        Log.d("TTT", intent?.action)
        if (intent?.action == ESM.ACTION_AWARE_ESM_DISMISSED) esmWasDismissed = true
        if (intent?.action == ESM.ACTION_AWARE_ESM_ANSWERED) esmWasDismissed = false
        if (intent?.action == ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE) {
            if (esmWasDismissed) {
                DatabaseRef.pushDB(eventType.ESM_DISMISSED, eventValue.NONE, Settings.Secure.getString(context?.contentResolver, Settings.Secure.ANDROID_ID))
            }
            else {
                Log.d("TTTin", "Queue")
                //ESM_Provider().query(ESM_Provider.ESM_Data.CONTENT_URI, )
                //Check if we already had something waiting but the participant has not answered
                //If we do, set the old as expired
                val pending_esms = context!!.contentResolver.query(
                    ESM_Provider.ESM_Data.CONTENT_URI,
                    null,
                    ESM_Provider.ESM_Data.STATUS + "=" + ESM.STATUS_ANSWERED,
                    null,
                    null
                )
                Log.d("ESMA", "pending_esms.getString(7)")
                if (pending_esms != null && pending_esms.moveToFirst()) {
                    Log.d("ESMA", pending_esms.getString(7))
                    Log.d("ESMA", SessionState.esmCounter.toString())
                    pending_esms.moveToPosition(pending_esms.count - SessionState.esmCounter - 1)
                    val esmAnswerObj = JSONObject()
                    var i = 1
                    var gson = Gson()
                    while (pending_esms.moveToNext()) {
                        Log.d("ESMA", pending_esms.getString(7))
                        esmAnswerObj.put(
                            "Question #" + i,
                            gson?.fromJson(
                                pending_esms.getString(5).dropLast(1)
                                    .plus(",\"esm_answer\":\"" + pending_esms.getString(7) + "\"}"),
                                ESM_Answer.ESM_Answer_Data::class.java
                            )
                        )
                        i++
                    }
                    DatabaseRef.pushDB(
                        eventType.ESM_ANSWER,
                        eventValue.NONE,
                        Settings.Secure.getString(
                            context.contentResolver,
                            Settings.Secure.ANDROID_ID
                        ),
                        mapOf("Answer" to esmAnswerObj.toString())
                    )

                }
            }
        }

        if (intent?.action == ESM.ACTION_AWARE_ESM_DISMISSED) {
            Log.d("ESM___", "Dismissed")
        }

        if (intent?.action == ESM.ACTION_AWARE_ESM_EXPIRED) {
            Log.d("ESM___", "Expired")
        }
    }


}