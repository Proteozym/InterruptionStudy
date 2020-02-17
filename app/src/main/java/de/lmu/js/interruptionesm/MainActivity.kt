package de.lmu.js.interruptionesm


import android.Manifest
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.aware.Applications
import com.aware.Aware
import com.aware.Aware_Preferences
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.jakewharton.threetenabp.AndroidThreeTen
import de.lmu.js.interruptionesm.SessionState.Companion.interruptionObj
import kotlinx.android.synthetic.main.activity_main.*

import org.threeten.bp.Duration
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import java.lang.Exception
import java.util.*


//import com.aware.plugin.fitbit.Plugin
//import com.aware.plugin.google.activity_recognition.Plugin.ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION


class MainActivity : AppCompatActivity(), TransitionListener {

    private lateinit var mTransitionRecognition: TransitionRecognition
    private lateinit var actReceiver: TransitionRecognitionReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        initTransitionRecognition()

        actReceiver = TransitionRecognitionReceiver(this);
        registerReceiver(actReceiver, IntentFilter("de.lmu.js.interruptionesm.TRANSITION_RECOGNITION"))

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


        //Passive Sensors
        if (SessionState.interruptState) {
            //Movement Modality Logging

            //
        }


    }

    override fun onResume() {
        super.onResume()
        //WHATS THE INTENT?
        registerReceiver(actReceiver, IntentFilter("de.lmu.js.interruptionesm.TRANSITION_RECOGNITION"))
    }

    override fun onPause() {
      // mTransitionRecognition.stopTracking()
       super.onPause()
    }

    override fun onStop() {
        super.onStop();
        unregisterReceiver(actReceiver);
    }

      fun updateUI(msg: String?) {
          main_activity_tv.text = main_activity_tv.text.toString() + "\n" + msg;
      }

    /**
     * INIT TRANSITION RECOGNITION
     */
    fun initTransitionRecognition(){
        mTransitionRecognition = TransitionRecognition()
        mTransitionRecognition.startTracking(this)
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

    fun startMockActivity(view: View) {
        var intent = Intent()//this, TransitionRecognitionReceiver::class.java)
        // Your broadcast receiver action

        intent.action = "de.lmu.js.interruptionesm.TRANSITION_RECOGNITION"
        var events: ArrayList<ActivityTransitionEvent> = arrayListOf()

        // You can set desired events with their corresponding state

        var transitionExitEvent = ActivityTransitionEvent(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_EXIT, SystemClock.elapsedRealtimeNanos())
        events.add(transitionExitEvent)
        var transitionEvent = ActivityTransitionEvent(DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_ENTER, SystemClock.elapsedRealtimeNanos())
        events.add(transitionEvent)

        var result = ActivityTransitionResult(events)
        SafeParcelableSerializer.serializeToIntentExtra(result, intent, "com.google.android.location.internal.EXTRA_ACTIVITY_TRANSITION_RESULT")

        sendBroadcast(intent);

        //LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun callMainActivity(value: String?) {
            updateUI(value);
    }


}


