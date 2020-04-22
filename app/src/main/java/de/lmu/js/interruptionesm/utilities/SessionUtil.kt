package de.lmu.js.interruptionesm.utilities

import android.content.Context
import android.content.SharedPreferences
import de.lmu.js.interruptionesm.SessionState


class SessionUtil {

    companion object {
        private var context: Context? = null
        private var sharedPrefs: SharedPreferences? = null

        fun checkSessionId(cont: Context) {
            this.context = cont
            sharedPrefs = context!!.getSharedPreferences("interruption_esm_pref", 0)

            val sessionId = sharedPrefs!!.getInt("SessionId", 1);
            SessionState.sessionId = sessionId
        }

        fun checkESMCount(cont: Context): Int {
            this.context = cont
            sharedPrefs = context!!.getSharedPreferences("interruption_esm_pref", 0)

            val esmCounter = sharedPrefs!!.getInt("esmCounter", 1);
            return esmCounter
        }

    }

}