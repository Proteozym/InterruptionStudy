package de.lmu.js.interruptionesm.utilities

import android.content.Context
import android.content.SharedPreferences
import de.lmu.js.interruptionesm.DatabaseRef
import de.lmu.js.interruptionesm.DatabaseRef.addUserToSurvey
import de.lmu.js.interruptionesm.DatabaseRef.confirmUserSurveyFin
import de.lmu.js.interruptionesm.DatabaseRef.verifyFinFromDB
import de.lmu.js.interruptionesm.DatabaseRef.verifyStartFromDB
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

        fun checkPermSurvey(userKey: String, cont: Context): Boolean {
            var permissionToTrack = false
            this.context = cont

            //First check shared pref
            val sharedPrefs = context!!.getSharedPreferences("interruption_esm_pref", 0)
            permissionToTrack = sharedPrefs.getBoolean("PERM", false)
            verifyStartFromDB(userKey, cont)
            return permissionToTrack
        }

        fun setPermSurvey(cont: Context) {
            this.context = cont

            val sharedPrefs = cont.getSharedPreferences("interruption_esm_pref", 0)
            val editor = sharedPrefs.edit()
            editor.putBoolean("PERM", true)
            editor.putBoolean("FINREADY", false)
            editor.apply();
            var succ = editor.commit();
        }

        fun checkSurveyFin(userKey: String, cont: Context): Boolean {
            //First check shared pref
            this.context = cont
            var surveyFin = false
            val sharedPrefs = context!!.getSharedPreferences("interruption_esm_pref", 0)
            surveyFin = sharedPrefs.getBoolean("FINREADY", false)

            verifyFinFromDB(userKey, cont)
            return surveyFin
        }

        fun setSurveyFin(cont: Context) {
            //First check shared pref
            this.context = cont

            val sharedPrefs = cont.getSharedPreferences("interruption_esm_pref", 0)
            val editor = sharedPrefs.edit()
            editor.putBoolean("FIN", true)
            editor.putBoolean("FINREADY", false)
            editor.apply();
            var succ = editor.commit();
        }
        fun checkKey(text: String, userKey: String, cont: Context): Boolean {
            if (text.toLowerCase().equals("startlmu20")) {
                setPermSurvey(cont)
                addUserToSurvey(userKey)
                return true
            }
            if (text.toLowerCase().equals("lmu20fin")) {
                setSurveyFin(cont)
                confirmUserSurveyFin(userKey)
                return true
            }
            return false
        }

    }

}