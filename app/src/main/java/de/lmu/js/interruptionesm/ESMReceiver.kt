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
import org.json.JSONObject

class ESMReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        Log.d("TTT", "Queue")
        if (intent?.action == ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE) {
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

            if (pending_esms != null && pending_esms.moveToFirst()) {

                pending_esms.moveToPosition(pending_esms.count - 2 - 1) //ToDo Based on number of question issued in last ESM - need to retrieve Last Index - N (+1) -> Last Index
                val esmAnswerObj = JSONObject()
                var i = 1
                while (pending_esms.moveToNext()) {
                    Log.d("TTT", pending_esms.getString(5))
                    esmAnswerObj.put("Question #" + i, pending_esms.getString(5))
                    esmAnswerObj.put("Answer #" + i, pending_esms.getString(7))
                    //2020-02-25 22:26:51.362 24310-24310/de.lmu.js.interruptionesm D/TTT: {"esm_type":1,"esm_title":"What is on your mind?","esm_submit":"Next","esm_instructions":"Tell us how you feel"}
                    //2020-02-25 22:26:51.362 24310-24310/de.lmu.js.interruptionesm D/TTT: {"esm_type":2,"esm_radios":["Bored","Fantastic"],"esm_title":"Are you...","esm_instructions":"Pick one!","esm_submit":"OK"}
                     // GET ANSWERS
                    i++
                }
                DatabaseRef.pushDB(eventType.ESM_ANSWER, eventValue.NONE, Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID), mapOf("Answer" to esmAnswerObj.toString()))
                Log.d("TTT", esmAnswerObj.toString())
            }
                //var result = pending_esms?.getString(pending_esms?.getColumnIndex("esm_answers"));
        }

        if (intent?.action == ESM.ACTION_AWARE_ESM_DISMISSED) {
            Log.d("ESM___", "Dismissed")
        }

        if (intent?.action == ESM.ACTION_AWARE_ESM_EXPIRED) {
            Log.d("ESM___", "Expired")
        }
    }


}