package de.lmu.js.interruptionesm


import android.content.*
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.aware.Applications
import com.aware.Aware
import com.aware.Aware_Preferences
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.jakewharton.threetenabp.AndroidThreeTen
import de.lmu.js.interruptionesm.SessionState.Companion.interruptionObj
import kotlinx.android.synthetic.main.activity_main.*
import org.threeten.bp.Duration
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import java.util.*


//import com.aware.plugin.fitbit.Plugin
//import com.aware.plugin.google.activity_recognition.Plugin.ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION


class MainActivity : AppCompatActivity() {


    private val TAG = MainActivity::class.java.simpleName
    var broadcastReceiver: BroadcastReceiver? = null

    private var txtActivity: TextView? = null
    private  var txtConfidence:TextView? = null
    private var imgActivity: ImageView? = null
    private var btnStartTrcking: Button? = null
    private  var btnStopTracking:android.widget.Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtActivity = findViewById(R.id.txt_activity)
        txtConfidence = findViewById(R.id.txt_confidence)
        imgActivity = findViewById(R.id.img_activity)
        btnStartTrcking = findViewById(R.id.btn_start_tracking)
        btnStopTracking = findViewById(R.id.btn_stop_tracking)

        //btn_start_tracking.setOnClickListener(startTracking)

        //btn_stop_tracking.setOnClickListener (stopTracking)

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Constants.BROADCAST_DETECTED_ACTIVITY) {
                    val type = intent.getIntExtra("type", -1)
                    val confidence = intent.getIntExtra("confidence", 0)
                    handleUserActivity(type, confidence)
                }
            }
        }

        AndroidThreeTen.init(this);

        Aware.startAWARE(this)
        Aware.startPlugins(this)

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
                Log.d("Ö BG", data!!.getAsString("package_name"))
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
                                stopTracking()
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

    private fun handleUserActivity(type: Int, confidence: Int) {
        var label = getString(R.string.activity_unknown)
        var icon = R.drawable.ic_still
        when (type) {
            DetectedActivity.IN_VEHICLE -> {
                label = getString(R.string.activity_in_vehicle)
                SessionState.mvmntModality.add(Movement_Object(Movement_Mod.IN_VEHICLE, LocalDateTime.now(), confidence))
                icon = R.drawable.ic_driving
            }
            DetectedActivity.ON_BICYCLE -> {
                label = getString(R.string.activity_on_bicycle)
                SessionState.mvmntModality.add(Movement_Object(Movement_Mod.ON_BICYCLE, LocalDateTime.now(), confidence))
                icon = R.drawable.ic_on_bicycle
            }
            DetectedActivity.ON_FOOT -> {

                //DO WE NEED THIS?

                label = getString(R.string.activity_on_foot)
                //SessionState.mvmntModality.add(Movement_Object(Movement_Mod.ON_FOOT, LocalDateTime.now(), confidence))
                icon = R.drawable.ic_walking
            }
            DetectedActivity.RUNNING -> {
                label = getString(R.string.activity_running)
                SessionState.mvmntModality.add(Movement_Object(Movement_Mod.RUNNING, LocalDateTime.now(), confidence))
                icon = R.drawable.ic_running
            }
            DetectedActivity.STILL -> {
                label = getString(R.string.activity_still)
                SessionState.mvmntModality.add(Movement_Object(Movement_Mod.STILL, LocalDateTime.now(), confidence))
            }
            DetectedActivity.TILTING -> {

                //DO WE NEED THIS?

                label = getString(R.string.activity_tilting)
                //SessionState.mvmntModality.add(Movement_Object(Movement_Mod.ON_BICYCLE, LocalDateTime.now(), confidence))
                icon = R.drawable.ic_tilting
            }
            DetectedActivity.WALKING -> {
                label = getString(R.string.activity_walking)
                SessionState.mvmntModality.add(Movement_Object(Movement_Mod.WALKING, LocalDateTime.now(), confidence))
                icon = R.drawable.ic_walking
            }
            DetectedActivity.UNKNOWN -> {
                label = getString(R.string.activity_unknown)
                SessionState.mvmntModality.add(Movement_Object(Movement_Mod.UNKNOWN, LocalDateTime.now(), confidence))
            }
        }
        Log.e(TAG, "User activity: $label, Confidence: $confidence")
        if (confidence > Constants.CONFIDENCE) {
            txtActivity!!.text = label
            txtConfidence!!.text = "Confidence: $confidence"
            imgActivity!!.setImageResource(icon)
        }
    }

    override fun onResume() {
        super.onResume()
        //LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver!!, IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY))
    }

    override fun onPause() {
       super.onPause()
        Log.d("Ö switch", "Is this switch?")
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver!!)
    }

    override fun onStop() {
        super.onStop();

    }

    override fun onDestroy() {
        super.onDestroy()
        stopInterruption()
        stopSession()
        stopTracking()
        Log.d("Ö Session", "Closed App")
        Log.d("Ö Interruption", "Closed App")
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver!!)
    }

    fun startTracking(){
        Log.d("Ö", "Started Tracking")
        val intent = Intent(this@MainActivity, BackgroundDetectedActivitiesService::class.java)
        startService(intent)

    }

    fun stopTracking() {
        Log.d("Ö", "Stop Tracking")
        val intent = Intent(this@MainActivity, BackgroundDetectedActivitiesService::class.java)
        stopService(intent)
    }


    private fun startSession() {
        Log.d("Ö ", "In startSess")
        if (SessionState.sessionId != 0) return;
        Log.d("Ö ", "In startSessY")
        SessionState.sessionId = LocalTime.now().hashCode();
        Log.d("Ö SessionID", SessionState.sessionId.toString())
        SessionState.startTime = LocalDateTime.now();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver!!, IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY))
        startTracking();
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



}


