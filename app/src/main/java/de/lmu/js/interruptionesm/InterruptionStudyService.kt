package de.lmu.js.interruptionesm

import android.Manifest
import android.R
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.aware.*
import com.aware.ui.esms.*
import com.google.android.gms.location.DetectedActivity
import com.jakewharton.threetenabp.AndroidThreeTen
import de.lmu.js.interruptionesm.SessionState.Companion.mvmntModalityRecord
import org.json.JSONException
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime


class InterruptionStudyService : Service() {

    private val TAG = MainActivity::class.java.simpleName
    var activityReceiver: BroadcastReceiver? = null
    var sessionTimeoutRec: BroadcastReceiver? = null
    var esmReceiver: ESMReceiver? = ESMReceiver()
    private var comReceiver: BroadcastReceiver? = null
    private var receivedMessage: Boolean = false
    private var receivedCall: Boolean = false
    //IS THIS SAVE??
    lateinit var userKey: String


    @Nullable
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

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

        comReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action.equals("android.provider.Telephony.SMS_RECEIVED")) {
                    receivedMessage = true;
                }
                if (intent.action.equals("android.intent.action.PHONE_STATE")) {
                    var state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    receivedCall = true;
                }
                Log.d("TTT", intent.toString())/*

                Log.d("TTT", state)
                if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                }*/

            }
        }

        sessionTimeoutRec = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d("WakeLock", "OUT")
                stopInterruption()
                stopSession()
                stopTracking()
            }
        }


        AndroidThreeTen.init(this);

        Aware.startAWARE(this)
        Aware.startPlugins(this)
        Aware.startScreen(this)
        Aware.setSetting(this, Aware_Preferences.DEBUG_FLAG, false)

        // Register for checking application use
        Aware.setSetting(this, Aware_Preferences.STATUS_APPLICATIONS, true)
        Aware.setSetting(this, Aware_Preferences.STATUS_NOTIFICATIONS, true)
        Aware.setSetting(this, Aware_Preferences.STATUS_ESM, true)

        Applications.isAccessibilityServiceActive(this)

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
                handleScreenInterruption("on")

            }
        })


        Applications.setSensorObserver(object : Applications.AWARESensorObserver {
            override fun onCrash(data: ContentValues?) {

            }

            override fun onNotification(data: ContentValues?) {
                Log.d("New Notification", "test");
            }

            override fun onBackground(data: ContentValues?) {
                Log.d("CHECKTHIS", data!!.getAsString("package_name"))
            }

            override fun onKeyboard(data: ContentValues?) {
                // Log.d("Application Sensor: Keyboard", "keyboard")
            }

            override fun onTouch(data: ContentValues?) {

            }

            override fun onForeground(data: ContentValues?) {
                Log.d("Ö", data!!.getAsString("package_name"))
                var packName = data!!.getAsString("package_name")
                if (packName == "com.android.calculator2") { //de.lmu.js.interruptionesm
                    if (SessionState.sessionStopped) {
                        startSession()
                        //generateESM()
                        Log.d("Ö Session", "Started")
                    } else {
                        if (SessionState.interruptState) stopInterruption(); Log.d(
                            "Ö Interruption",
                            "Stopped"
                        )

                    }

                }

                else {
                    if (!SessionState.sessionStopped) {
                        if (!SessionState.interruptState) {
                            //Look for trigger
                            if(packName == "com.android.systemui")  {
                                //Do we want to handle this differently?
                                startInterruption(eventValue.APP_SWITCH, mapOf("receivedCall" to receivedCall.toString(), "receivedMessage" to receivedMessage.toString(), "switchedTo" to packName))
                            }
                            else {
                                startInterruption(eventValue.APP_SWITCH, mapOf("receivedCall" to receivedCall.toString(), "receivedMessage" to receivedMessage.toString(), "switchedTo" to packName))
                            }
                        } else {
                            if (Duration.between(
                                    SessionState.interruptTmstmp,
                                    LocalDateTime.now()
                                ).seconds > 100 //600
                            ) {
                                Log.d("Ö", "Time Out")
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
        startForeground()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForeground() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, 0
        )

        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("Interrupt_Study", "My Background Service")
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }

        startForeground(
            101, NotificationCompat.Builder(
                this,
                channelId
            )
                .setOngoing(true)
                .setSmallIcon(R.drawable.btn_dialog)
                .setContentTitle("Interruption Study")
                .setContentText("Service is running background")
                .setContentIntent(pendingIntent)
                .build()
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    private fun handleScreenInterruption(trigger: String) {
        Log.d("Ö", "Locked - " + trigger)
        if (!SessionState.sessionStopped) {
            // TODO : IS SCREEN OFF - SLEEP? APPEARS SO YES
            if (!SessionState.interruptState) {
                if (trigger == "on") {
                    stopInterruption()
                    return
                }
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
        var label = getString(de.lmu.js.interruptionesm.R.string.activity_unknown)

        when (type) {
            DetectedActivity.IN_VEHICLE -> {
                if(SessionState.mvmntModalityRecord.last().movement != MovementRecord.Movement.IN_VEHICLE) {
                    label = getString(de.lmu.js.interruptionesm.R.string.activity_in_vehicle)
                    SessionState.mvmntModalityRecord.add(MovementRecord(MovementRecord.Movement.IN_VEHICLE, confidence))
                    DatabaseRef.pushDB(
                        eventType.MOVEMENT,
                        eventValue.IN_VEHICLE,
                        userKey,
                        mapOf("Confidence" to confidence.toString())
                    )
                }
            }
            DetectedActivity.ON_BICYCLE -> {
                if(SessionState.mvmntModalityRecord.last().movement != MovementRecord.Movement.ON_BICYCLE) {
                    label = getString(de.lmu.js.interruptionesm.R.string.activity_on_bicycle)
                    SessionState.mvmntModalityRecord.add(MovementRecord(MovementRecord.Movement.ON_BICYCLE, confidence))
                    DatabaseRef.pushDB(
                        eventType.MOVEMENT,
                        eventValue.BICYCLE,
                        userKey,
                        mapOf("Confidence" to confidence.toString())
                    )
                }
            }
            DetectedActivity.ON_FOOT -> {

                //DO WE NEED THIS?

                label = getString(de.lmu.js.interruptionesm.R.string.activity_on_foot)
                //SessionState.mvmntModality.add(Movement_Object(Movement_Mod.ON_FOOT, LocalDateTime.now(), confidence))
            }
            DetectedActivity.RUNNING -> {
                if(SessionState.mvmntModalityRecord.last().movement != MovementRecord.Movement.RUNNING) {
                    label = getString(de.lmu.js.interruptionesm.R.string.activity_running)
                    SessionState.mvmntModalityRecord.add(MovementRecord(MovementRecord.Movement.RUNNING, confidence))
                    DatabaseRef.pushDB(
                        eventType.MOVEMENT,
                        eventValue.RUNNING,
                        userKey,
                        mapOf("Confidence" to confidence.toString())
                    )
                }
            }
            DetectedActivity.STILL -> {
                if(SessionState.mvmntModalityRecord.last().movement != MovementRecord.Movement.STILL) {
                    label = getString(de.lmu.js.interruptionesm.R.string.activity_still)
                    SessionState.mvmntModalityRecord.add(MovementRecord(MovementRecord.Movement.STILL, confidence))
                    DatabaseRef.pushDB(
                        eventType.MOVEMENT,
                        eventValue.STILL,
                        userKey,
                        mapOf("Confidence" to confidence.toString())
                    )
                }
            }
            DetectedActivity.WALKING -> {
                if(SessionState.mvmntModalityRecord.last().movement != MovementRecord.Movement.WALKING) {
                    label = getString(de.lmu.js.interruptionesm.R.string.activity_walking)
                    SessionState.mvmntModalityRecord.add(MovementRecord(MovementRecord.Movement.WALKING, confidence))
                    DatabaseRef.pushDB(
                        eventType.MOVEMENT,
                        eventValue.WALKING,
                        userKey,
                        mapOf("Confidence" to confidence.toString())
                    )
                }
            }
            DetectedActivity.UNKNOWN -> {
                label = getString(de.lmu.js.interruptionesm.R.string.activity_unknown)
                SessionState.mvmntModalityRecord.add(MovementRecord(MovementRecord.Movement.NONE, confidence))
                DatabaseRef.pushDB(eventType.MOVEMENT, eventValue.NONE, userKey, mapOf("Confidence" to confidence.toString()))
            }
        }
        Log.e("Interr Study", "User activity: $label, Confidence: $confidence")
        if (confidence > Constants.CONFIDENCE) {
           /* txtActivity!!.text = label
            txtConfidence!!.text = "Confidence: $confidence"
            imgActivity!!.setImageResource(icon)*/
        }
    }

    fun onClose() {
        stopInterruption()
        stopSession()
        stopTracking()
        Log.d("Ö Session", "Closed App")
        Log.d("Ö Interruption", "Closed App")
        LocalBroadcastManager.getInstance(this).unregisterReceiver(activityReceiver!!)

        //TODO KILL APP WATCHER SERVCE?
        unregisterReceiver(esmReceiver!!)
    }


    fun startTracking(){
        Log.d("Ö", "Started Tracking")
        val intent = Intent(this, BackgroundDetectedActivitiesService::class.java)
        startService(intent)

    }

    fun stopTracking() {
        Log.d("Ö", "Stop Tracking")
        //this@MainActivity
        val intent = Intent(this, BackgroundDetectedActivitiesService::class.java)
        stopService(intent)
    }


    private fun startSession() {
        Log.d("Ö ", "In startSess")
        if (!SessionState.sessionStopped) return;
        SessionState.sessionStopped = false
        Log.d("Ö ", "In startSessY")
        SessionState.sessionId = LocalTime.now().hashCode();
        Log.d("Ö SessionID", SessionState.sessionId.toString())
        registerReceiver(activityReceiver!!, IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY))
        startTracking();


        var communicationFilter = IntentFilter();
        communicationFilter.addAction("android.intent.action.PHONE_STATE")
        communicationFilter.addAction("android.provider.Telephony.SMS_RECEIVED")
        registerReceiver(comReceiver, communicationFilter)

        DatabaseRef.pushDB(eventType.SESSION_START, eventValue.NONE, userKey)
        //startService(Intent(this, AppTrackerService::class.java))
        receivedMessage = false;
        receivedCall = false;

        registerReceiver(sessionTimeoutRec!!, IntentFilter("SESSION_TIMED_OUT"))

    }

    private fun stopSession() {
        if (SessionState.sessionStopped) return;
        DatabaseRef.pushDB(eventType.SESSION_END, eventValue.NONE, userKey)
        Log.d("Ö", "Generate ESM")
        generateESM()

        //Reset Session
        SessionState.sessionStopped = true
        SessionState.interruptState = false;
        SessionState.mvmntModalityRecord = mutableListOf(MovementRecord(MovementRecord.Movement.NONE, 100))
        SessionState.esmCounter = 0
        Log.d("Ö", "Pre Unreg")
        unregisterReceiver(comReceiver!!)
        unregisterReceiver(activityReceiver!!)
        unregisterReceiver(sessionTimeoutRec!!)
        stopService(Intent(this, TrackerWakelock::class.java))
        Log.d("Ö", "Post Unreg")
    }

    fun generateESM() {
        try {
            var factory = ESMFactory();
            var questionCounter = 0
            for (mov in mvmntModalityRecord) {
                if (mov.confidence < 90) {

                    var quickAnswer = ESM_QuickAnswer()
                    quickAnswer.addQuickAnswer("Yes")
                        .addQuickAnswer("No")
                        .setInstructions("During your latest language session, were you " + mov.movement)
                    factory.addESM(quickAnswer)

                    questionCounter++
                }
            }
            Log.d("Ö", "In")
            //ToDo Based on number of question issued in last ESM - need to retrieve Last Index - N -> Last Index
            SessionState.esmCounter = questionCounter

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

    private fun startInterruption(eVal: eventValue, addProp: Map<String, String> = mapOf()) {
        if (SessionState.interruptState) return
        SessionState.interruptState = true;
        SessionState.interruptTmstmp = LocalDateTime.now()
        DatabaseRef.pushDB(eventType.INTERRUPTION_START, eVal, userKey, addProp)
        startService(Intent(this, TrackerWakelock::class.java))
    }

    private fun stopInterruption() {
        if (!SessionState.interruptState) return

        DatabaseRef.pushDB(eventType.INTERRUPTION_END, eventValue.NONE, userKey)
        SessionState.interruptState = false;
        receivedMessage = false;
        receivedCall = false;
        stopService(Intent(this, TrackerWakelock::class.java))
    }

}