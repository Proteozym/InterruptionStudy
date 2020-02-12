package de.lmu.js.interruptionesm

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log


import com.aware.Applications
import com.aware.Aware
import com.aware.Aware_Preferences
import com.aware.Screen
import com.jakewharton.threetenabp.AndroidThreeTen
import de.lmu.js.interruptionesm.SessionState.Companion.interruptionObj
import org.threeten.bp.Duration
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime


//import com.aware.plugin.fitbit.Plugin
//import com.aware.plugin.google.activity_recognition.Plugin.ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION


class MainActivity : AppCompatActivity() {

    var receiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AndroidThreeTen.init(this);

        Aware.startAWARE(this)
        Aware.startPlugins(this)
       // Aware.startPlugin(this, "com.aware.plugin.google.activity_recognition")
        configureReceiver()

        Aware.setSetting(this, Aware_Preferences.DEBUG_FLAG, false)

        // Register for checking application use
        Aware.setSetting(this, Aware_Preferences.STATUS_APPLICATIONS, true)
        Aware.setSetting(this, Aware_Preferences.STATUS_NOTIFICATIONS, true)

        /*Aware.setSetting(applicationContext, Aware_Preferences.LOCATION_GEOFENCE, true)
        Aware.setSetting(applicationContext, Aware_Preferences.STATUS_LOCATION_NETWORK, true)
        Aware.setSetting(applicationContext, Aware_Preferences.STATUS_LOCATION_GPS, true)
        Aware.setSetting(applicationContext, Aware_Preferences.FREQUENCY_LOCATION_GPS, 0)
        Aware.setSetting(applicationContext, Aware_Preferences.FREQUENCY_LOCATION_NETWORK, 0)
        Aware.setSetting(applicationContext, Aware_Preferences.MIN_LOCATION_GPS_ACCURACY, 5)*/

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
                    }
                    else {
                        if(SessionState.interruptState) stopInterruption(); Log.d("Ö Interruption", "Stopped")

                    }

                }

                if (data!!.getAsString("package_name") != "de.lmu.js.interruptionesm") {
                    if (SessionState.sessionId != 0) {
                        if (!SessionState.interruptState) {
                            //Look for trigger
                            Log.d("Ö Interruption", "Started")
                            startInterruption(InterruptType.APPLICATION_SWITCH, Trigger.NONE)
                        } else {
                            if (Duration.between(interruptionObj.startTime, LocalDate.now()).seconds > 600) {
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
        Screen.setSensorObserver(object: Screen.AWARESensorObserver {
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


        })

        //Passive Sensors
        if(SessionState.interruptState) {
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

    private fun configureReceiver() {
        val filter = IntentFilter()
        //filter.addAction(ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION)
        receiver = ActivityListener()
        registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}

