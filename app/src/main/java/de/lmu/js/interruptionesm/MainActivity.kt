package de.lmu.js.interruptionesm


import android.Manifest
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.aware.Applications
import com.aware.Aware
import com.aware.Aware_Preferences
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.jakewharton.threetenabp.AndroidThreeTen
import de.lmu.js.interruptionesm.SessionState.Companion.interruptionObj

import org.threeten.bp.Duration
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import java.lang.Exception
import java.util.*


//import com.aware.plugin.fitbit.Plugin
//import com.aware.plugin.google.activity_recognition.Plugin.ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION


class MainActivity : AppCompatActivity() {


    private var activityTransitionList: List<ActivityTransition>? = null

    // Action fired when transitions are triggered.
        private fun toActivityString(activity: Int): String? {
        return when (activity) {
            DetectedActivity.STILL -> "STILL"
            DetectedActivity.WALKING -> "WALKING"
            else -> "UNKNOWN"
        }
    }

    private fun toTransitionType(transitionType: Int): String? {
        return when (transitionType) {
            ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "ENTER"
            ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "EXIT"
            else -> "UNKNOWN"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // List of activity transitions to track.
        activityTransitionList = ArrayList()

        (activityTransitionList as ArrayList<ActivityTransition>).add(ActivityTransition.Builder()
        .setActivityType(DetectedActivity.WALKING)
        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
        .build());
        (activityTransitionList as ArrayList<ActivityTransition>).add(ActivityTransition.Builder()
        .setActivityType(DetectedActivity.WALKING)
        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        .build());
        (activityTransitionList as ArrayList<ActivityTransition>).add(ActivityTransition.Builder()
        .setActivityType(DetectedActivity.STILL)
        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
        .build());
        (activityTransitionList as ArrayList<ActivityTransition>).add(ActivityTransition.Builder()
        .setActivityType(DetectedActivity.STILL)
        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        .build());

        AndroidThreeTen.init(this);

        Aware.startAWARE(this)
        Aware.startPlugins(this)
        Aware.startPlugin(this, "com.aware.plugin.google.activity_recognition")

        Aware.setSetting(this, Aware_Preferences.DEBUG_FLAG, false)

        // Register for checking application use
        Aware.setSetting(this, Aware_Preferences.STATUS_APPLICATIONS, true)
        Aware.setSetting(this, Aware_Preferences.STATUS_NOTIFICATIONS, true)

        Applications.isAccessibilityServiceActive(this)
        Applications.setSensorObserver(object : Applications.AWARESensorObserver {
            override fun onCrash(data: ContentValues?) {

            }

            override fun onNotification(data: ContentValues?) {
                Log.d("New Notification", "test");
            }

            override fun onBackground(data: ContentValues?) {

            }

            override fun onKeyboard(data: ContentValues?) {
                // Log.d("Application Sensor: Keyboard", "keyboard")
            }

            override fun onTouch(data: ContentValues?) {

            }

            override fun onForeground(data: ContentValues?) {
                Log.d("Ö", data!!.getAsString("package_name"))
                if (data!!.getAsString("package_name") == "de.lmu.js.interruptionesm") {
                    if (SessionState.sessionId == 0) {
                        startSession()
                        Log.d("Ö Session", "Started")
                    } else {
                        if (SessionState.interruptState) stopInterruption(); Log.d(
                            "Ö Interruption",
                            "Stopped"
                        )

                    }

                }

                if (data!!.getAsString("package_name") != "de.lmu.js.interruptionesm") {
                    if (SessionState.sessionId != 0) {
                        if (!SessionState.interruptState) {
                            //Look for trigger
                            Log.d("Ö Interruption", "Started")
                            startInterruption(InterruptType.APPLICATION_SWITCH, Trigger.NONE)
                        } else {
                            if (Duration.between(
                                    interruptionObj.startTime,
                                    LocalDate.now()
                                ).seconds > 600
                            ) {
                                stopInterruption()
                                stopSession()
                                Log.d("Ö Session", "Stopped")
                                Log.d("Ö Interruption", "Stopped")
                            }
                        }
                    }
                }


            }
        })


//do we need this or does onforeground trigger on lock as well? test on lock and unlock
        /* Screen.setSensorObserver(object: Screen.AWARESensorObserver {
            override fun onScreenLocked() {
                Log.d("Ö Screen Lock - Interruption", "lock")
                if (SessionState.sessionId !=0 && !SessionState.interruptState) {
                    Log.d("Ö Screen Lock - Interruption", "Started")
                    startInterruption(InterruptType.APPLICATION_SWITCH, Trigger.NONE)
                }
            }

            override fun onScreenOff() {
                Log.d("Ö Screen Off - Interruption", "off")
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onScreenOn() {
                Log.d("Ö Screen On - Interruption", "on")
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onScreenUnlocked() {
                Log.d("Screen Unlock", "Notice")
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }


        })*/

        //Passive Sensors
        if (SessionState.interruptState) {
            //Movement Modality Logging

            //
        }


    }

    private fun startSession() {
        Log.d("Ö ", "In startSess")
        if (SessionState.sessionId != 0) return;
        Log.d("Ö ", "In startSessY")
        SessionState.sessionId = LocalTime.now().hashCode();
        Log.d("Ö SessionID", SessionState.sessionId.toString())
        SessionState.startTime = LocalDateTime.now();
    }

    private fun stopSession() {
        if (SessionState.sessionId == 0) return;
        SessionState.endTime = LocalDateTime.now();

        //Start DB upload or start routine to wait for Wifi then DB upload
    }

    private fun startInterruption(type: InterruptType, trig: Trigger) {
        if (SessionState.interruptState) return
        SessionState.interruptState = true;
        interruptionObj = InterruptionObject(type, trig);
        interruptionObj.startTime = LocalDateTime.now();
        Log.d("Ö Int Time Start", interruptionObj.startTime.toString())
        //Session Context
        //SessionState.mvmntModality = MAYBE CALL

    }

    private fun stopInterruption() {
        if (!SessionState.interruptState) return
        SessionState.interruptState = false;
        SessionState.interruptionObj.endTime = LocalDateTime.now();
        Log.d("Ö Int Time ENd", interruptionObj.endTime.toString())

        //Start DB upload or start routine to wait for Wifi then DB upload
    }

}


