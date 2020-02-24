package de.lmu.js.interruptionesm


import android.content.*
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Advanceable
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.ads.identifier.AdvertisingIdClient
import androidx.ads.identifier.AdvertisingIdInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.aware.*
import com.aware.ui.esms.ESMFactory
import com.aware.ui.esms.ESM_Freetext
import com.aware.ui.esms.ESM_Radio
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures.addCallback
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import org.threeten.bp.Duration
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import java.util.*
import java.util.concurrent.Executors


//import com.aware.plugin.fitbit.Plugin
//import com.aware.plugin.google.activity_recognition.Plugin.ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION


class MainActivity : AppCompatActivity() {


    private val TAG = MainActivity::class.java.simpleName
    var activityReceiver: BroadcastReceiver? = null
    var esmReceiver: ESMReceiver? = ESMReceiver()

    //IS THIS SAVE??
    lateinit var userKey: String


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


        userKey = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
        activityReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Constants.BROADCAST_DETECTED_ACTIVITY) {
                    val type = intent.getIntExtra("type", -1)
                    val confidence = intent.getIntExtra("confidence", 0)
                    handleUserActivity(type, confidence)
                }
            }
        }

        var esmFilter = IntentFilter();
        esmFilter.addAction(ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE)
        esmFilter.addAction(ESM.ACTION_AWARE_ESM_DISMISSED)
        esmFilter.addAction(ESM.ACTION_AWARE_ESM_EXPIRED)
        registerReceiver(esmReceiver, esmFilter)

        AndroidThreeTen.init(this);

        Aware.startAWARE(this)
        Aware.startPlugins(this)
        Aware.startScreen(this)

        Aware.setSetting(this, Aware_Preferences.DEBUG_FLAG, true)

        // Register for checking application use
        Aware.setSetting(this, Aware_Preferences.STATUS_APPLICATIONS, true)
        Aware.setSetting(this, Aware_Preferences.STATUS_NOTIFICATIONS, true)
        Aware.setSetting(this, Aware_Preferences.STATUS_ESM, true)

        Applications.isAccessibilityServiceActive(this)


        //LocalBroadcastManager.getInstance(this).registerReceiver(esmReceiver!!, esmFilter)


        Screen.setSensorObserver(object : Screen.AWARESensorObserver {
            override fun onScreenLocked() {
                handleScreenInterruption("lock")
            }

            override fun onScreenOff() {
                handleScreenInterruption("off")
            }

            override fun onScreenOn() {
                Log.d("Ö", "Screen ON")
            }

            override fun onScreenUnlocked() {
                Log.d("Ö", "Screen Unlocked")

            }


        })

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
                var packName = data!!.getAsString("package_name")
                if (packName == "de.lmu.js.interruptionesm") {
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

                else {
                    if (SessionState.sessionId != 0) {
                        if (!SessionState.interruptState) {
                            //Look for trigger

                            if(packName == "com.android.systemui")  {
                                //startInterruption(eventValue.SCREEN_LOCK)
                            }
                            else {
                                Log.d("Ö Interruption", "Started")
                                startInterruption(eventValue.APP_SWITCH)
                            }
                        } else {
                            if (Duration.between(
                                    SessionState.interruptTmstmp,
                                    LocalDateTime.now()
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

    private fun handleScreenInterruption(trigger: String) {
        Log.d("Ö", "Locked - " + trigger)
        if (SessionState.sessionId != 0) {
            // TODO : IS SCREEN OFF - SLEEP? APPEARS SO YES
            if (!SessionState.interruptState) {
                if (trigger == "lock") {
                    startInterruption(eventValue.SCREEN_LOCK)
                }
                else {
                    startInterruption(eventValue.SCREEN_OFF)
                }
            }
        }
    }

    private fun handleUserActivity(type: Int, confidence: Int) {
        var label = getString(R.string.activity_unknown)
        var icon = R.drawable.ic_still

        when (type) {
            DetectedActivity.IN_VEHICLE -> {
                if(SessionState.mvmntModalityRecord.last().movement != MovementRecord.Movement.IN_VEHICLE) {
                    label = getString(R.string.activity_in_vehicle)
                    SessionState.mvmntModalityRecord.add(MovementRecord(MovementRecord.Movement.IN_VEHICLE, confidence))
                    DatabaseRef.pushDB(
                        eventType.MOVEMENT,
                        eventValue.IN_VEHICLE,
                        userKey,
                        mapOf("Confidence" to confidence.toString())
                    )
                    icon = R.drawable.ic_driving
                }
            }
            DetectedActivity.ON_BICYCLE -> {
                if(SessionState.mvmntModalityRecord.last().movement != MovementRecord.Movement.ON_BICYCLE) {
                    label = getString(R.string.activity_on_bicycle)
                    SessionState.mvmntModalityRecord.add(MovementRecord(MovementRecord.Movement.ON_BICYCLE, confidence))
                    DatabaseRef.pushDB(
                        eventType.MOVEMENT,
                        eventValue.BYCICLE,
                        userKey,
                        mapOf("Confidence" to confidence.toString())
                    )
                    icon = R.drawable.ic_on_bicycle
                }
            }
            DetectedActivity.ON_FOOT -> {

                //DO WE NEED THIS?

                label = getString(R.string.activity_on_foot)
                //SessionState.mvmntModality.add(Movement_Object(Movement_Mod.ON_FOOT, LocalDateTime.now(), confidence))
                icon = R.drawable.ic_walking
            }
            DetectedActivity.RUNNING -> {
                if(SessionState.mvmntModalityRecord.last().movement != MovementRecord.Movement.RUNNING) {
                    label = getString(R.string.activity_running)
                    SessionState.mvmntModalityRecord.add(MovementRecord(MovementRecord.Movement.RUNNING, confidence))
                    DatabaseRef.pushDB(
                        eventType.MOVEMENT,
                        eventValue.RUNNING,
                        userKey,
                        mapOf("Confidence" to confidence.toString())
                    )
                    icon = R.drawable.ic_running
                }
            }
            DetectedActivity.STILL -> {
                if(SessionState.mvmntModalityRecord.last().movement != MovementRecord.Movement.STILL) {
                    label = getString(R.string.activity_still)
                    SessionState.mvmntModalityRecord.add(MovementRecord(MovementRecord.Movement.STILL, confidence))
                    DatabaseRef.pushDB(
                        eventType.MOVEMENT,
                        eventValue.STILL,
                        userKey,
                        mapOf("Confidence" to confidence.toString())
                    )
                }
            }
            DetectedActivity.TILTING -> {

                //DO WE NEED THIS?

                label = getString(R.string.activity_tilting)
                //SessionState.mvmntModality.add(Movement_Object(Movement_Mod.ON_BICYCLE, LocalDateTime.now(), confidence))
                icon = R.drawable.ic_tilting
            }
            DetectedActivity.WALKING -> {
                if(SessionState.mvmntModalityRecord.last().movement != MovementRecord.Movement.WALKING) {
                    label = getString(R.string.activity_walking)
                    //SessionState.mvmntModality.add(Movement_Object(Movement_Mod.WALKING, LocalDateTime.now(), confidence))
                    DatabaseRef.pushDB(
                        eventType.MOVEMENT,
                        eventValue.WALKING,
                        userKey,
                        mapOf("Confidence" to confidence.toString())
                    )
                    icon = R.drawable.ic_walking
                }
            }
            DetectedActivity.UNKNOWN -> {
                label = getString(R.string.activity_unknown)
                //SessionState.mvmntModality.add(Movement_Object(Movement_Mod.UNKNOWN, LocalDateTime.now(), confidence))
                DatabaseRef.pushDB(eventType.MOVEMENT, eventValue.NONE, userKey, mapOf("Confidence" to confidence.toString()))
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
        //LocalBroadcastManager.getInstance(this).registerReceiver(activityReceiver!!, IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY))
    }

    override fun onPause() {
       super.onPause()
        Log.d("Ö switch", "Is this switch?")
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(activityReceiver!!)
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(activityReceiver!!)
        //TODO MAYBE WE CANT DO THAT HERE
        unregisterReceiver(esmReceiver!!)
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
        LocalBroadcastManager.getInstance(this).registerReceiver(activityReceiver!!, IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY))
        startTracking();
        DatabaseRef.pushDB(eventType.SESSION_START, eventValue.NONE, userKey)
        generateESM()
    }

    private fun stopSession() {
        if (SessionState.sessionId == 0) return;
        DatabaseRef.pushDB(eventType.SESSION_END, eventValue.NONE, userKey)
        generateESM()
    }

    private fun generateESM() {
        try {
        var factory = ESMFactory();

        var esmFreetext = ESM_Freetext();
        esmFreetext.setTitle("What is on your mind?")
        .setSubmitButton("Next")
        .setInstructions("Tell us how you feel");

        var esmRadio = ESM_Radio();
        esmRadio.addRadio("Bored")
        .addRadio("Fantastic")
        .setTitle("Are you...")
        .setInstructions("Pick one!")
        .setSubmitButton("OK");

        //add them to the factory
        factory.addESM(esmFreetext);
        factory.addESM(esmRadio);

        //Queue them
        ESM.queueESM(this, factory.build()); } catch (e: JSONException) { Log.e("ESM ERROR", e.toString()) }

        DatabaseRef.pushDB(eventType.ESM_SENT, eventValue.NONE, userKey)
    }

    private fun startInterruption(eVal: eventValue) {
        if (SessionState.interruptState) return
        SessionState.interruptState = true;
        SessionState.interruptTmstmp = LocalDateTime.now()
        DatabaseRef.pushDB(eventType.INTERRUPTION_START, eVal, userKey)

    }

    private fun stopInterruption() {
        if (!SessionState.interruptState) return
        SessionState.interruptState = false;
        DatabaseRef.pushDB(eventType.INTERRUPTION_END, eventValue.NONE, userKey)

    }


}


