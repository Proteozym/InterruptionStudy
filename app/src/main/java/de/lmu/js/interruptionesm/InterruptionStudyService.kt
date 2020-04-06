package de.lmu.js.interruptionesm

import android.R
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.aware.*
import com.aware.ui.esms.ESMFactory
import com.aware.ui.esms.ESM_Radio
import com.google.android.gms.location.DetectedActivity
import com.jakewharton.threetenabp.AndroidThreeTen
import de.lmu.js.interruptionesm.DatabaseRef.pushDBDaily
import de.lmu.js.interruptionesm.SessionState.Companion.mvmntModalityRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONException
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime

class InterruptionStudyService : AccessibilityService() {

    private val TAG = MainActivity::class.java.simpleName
    var activityReceiver: BroadcastReceiver? = null
    var sessionTimeoutRec: BroadcastReceiver? = null
    var esmReceiver: ESMReceiver? = null
    private var comReceiver: BroadcastReceiver? = null
    //IS THIS SAVE??
    lateinit var userKey: String
    var appSwitchList = mutableListOf<String>()
    val blockedAppList = mutableListOf<String>("com.android.systemui", "com.touchtype.swiftkey", "com.google.android.inputmethod.latin", "com.syntellia.fleksy.keyboard", "com.gamelounge.chroomakeyboard", "com.gingersoftware.android.keyboard", "com.boloorian.android.farsikeyboard", "com.jb.gokeyboard")

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false
    private var lastApp = ""


   /* @Nullable
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }*/

    override fun onServiceConnected() {
        super.onServiceConnected()

        //Configure these here for compatibility with API 13 and below.
        val config = AccessibilityServiceInfo()
        config.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        if (Build.VERSION.SDK_INT >= 16) //Just in case this helps
            config.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = config
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        userKey = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)

        if (intent != null) {
            val action = intent.action
            Log.d( "Ö", "using an intent with action $action")
            when (action) {
                Actions.START.name -> startService()
                Actions.STOP.name -> stopService()
                else -> Log.d( "Ö", "This should never happen. No action in the received intent")
            }
        } else {
            Log.d( "Ö",
                "with a null intent. It has been probably restarted by the system."
            )
        }

        regActivity()
        regCom()
        regSess()
        regESM()

        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo: ResolveInfo =
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        blockedAppList.add(resolveInfo.activityInfo.packageName)

        AndroidThreeTen.init(this);

        Aware.setSetting(this, Aware_Preferences.STATUS_ESM, true)
        Aware.startAWARE(this)
        //Applications.isAccessibilityServiceActive(this)

        startService()

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

        //startForeground()
        return START_STICKY;
        //return super.onStartCommand(intent, flags, startId)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Log.d("Ö", event.toString())

        //REMEMBER LAST APP

        if (event.getEventType() === AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null && event.getClassName() != null) {

                val pm = applicationContext.packageManager
                val ai: ApplicationInfo?
                ai = try {
                    pm.getApplicationInfo(event.getPackageName().toString(), 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
                val appName =
                    (if (ai != null) pm.getApplicationLabel(ai) else "(unknown)") as String
                Log.d("Foreground App", event.getPackageName().toString())
                if (appName.equals(lastApp)) return
                lastApp = appName
                Log.d("Ö", event.toString())
                Log.d("Ö package name", event!!.getPackageName().toString())
                Log.d("Ö app name", appName)
                var packName = event!!.packageName.toString()

                pushDBDaily(packName, userKey)

                //get selected app to track
                val sharedPref = getSharedPreferences(getString(de.lmu.js.interruptionesm.R.string.preference_key), Context.MODE_PRIVATE)
                Log.d("LÖL", sharedPref.getString("APP", "empty")!!.split("|")[0])
                val packToTrack = sharedPref.getString("APP", "empty")!!.split("|")[0]
                if (packToTrack.equals("empty")) {
                    Log.e("Interruption ESM", "App to track selection not working!")
                }
                else {
                    if (packName.equals(packToTrack)) { //de.lmu.js.interruptionesm
                        if (SessionState.sessionStopped) {
                            startSession()
                            //generateESM()
                            Log.d("Ö Session", "Started")
                        } else {
                            if (SessionState.interruptState) {
                                stopInterruption(mapOf("switchedTo" to appSwitchList.joinToString()))
                                Log.d(
                                    "Ö Interruption",
                                    "Stopped"
                                )
                            }

                        }

                    } else {
                        if (!SessionState.sessionStopped && !blockedAppList.contains(packName)) {
                            Log.d("Ö NO", "IIN")

                            Log.d("Ö NO", appSwitchList.toString())
                            if (!SessionState.interruptState) {
                                startInterruption(eventValue.APP_SWITCH)
                                appSwitchList.add(appName)
                            } else {
                                appSwitchList.add(appName)
                                /* if (Duration.between(
                                        SessionState.interruptTmstmp,
                                        LocalDateTime.now()
                                    ).seconds > 25 //600
                                ) {
                                    Log.d("Ö", "Time Out")
                                    stopInterruption(mapOf("switchedTo" to appSwitchList.joinToString()))
                                    stopSession()
                                    stopTracking()
                                    Log.d("Ö Session", "Stopped")
                                    Log.d("Ö Interruption", "Stopped")
                                }
                            */
                            }
                        }
                        Log.d("Ö ss", appSwitchList.joinToString())
                    }
                }
            }
            }
        }


    override fun onCreate() {
        super<AccessibilityService>.onCreate()

        var notification = createNotification()
        startForeground(1, notification)
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

    private fun startService() {
        if (isServiceStarted) return

        Toast.makeText(this, "Service starting its task", Toast.LENGTH_SHORT).show()
        isServiceStarted = true
        setServiceState(this, de.lmu.js.interruptionesm.ServiceState.STARTED)

        // we need this lock so our service gets not affected by Doze Mode
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock").apply {
                    acquire()
                }
            }

        // we're starting a loop in a coroutine
        GlobalScope.launch(Dispatchers.IO) {
            while (isServiceStarted) {
                launch(Dispatchers.IO) {
                    //pingFakeServer()
                }
                delay(1 * 60 * 1000)
            }

        }
    }

    private fun stopService() {

        Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show()
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            Log.d("Ö", "Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
        setServiceState(this, de.lmu.js.interruptionesm.ServiceState.STOPPED)
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "ENDLESS SERVICE CHANNEL"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
            val channel = NotificationChannel(
                notificationChannelId,
                "Endless Service notifications channel",
                NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = "Endless Service channel"
                it.enableLights(true)
                it.lightColor = Color.RED
                it.enableVibration(true)
                it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }

        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
            this,
            notificationChannelId
        ) else Notification.Builder(this)

        return builder
            .setContentTitle("Interruption Study")
            .setContentText("Data collection active")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.edit_text)
            //.setTicker("Ticker text")
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
            .build()
    }

    //TODO: Remove
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
        Log.d("Ö Movement", "IN")
        when (type) {

            DetectedActivity.IN_VEHICLE -> {
                Log.d("Ö Movement", "IN_VEHICLE")
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
                Log.d("Ö Movement", "ON_BICYCLE")
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
                Log.d("Ö Movement", "ON_FOOT")
                //DO WE NEED THIS?

                label = getString(de.lmu.js.interruptionesm.R.string.activity_on_foot)
                //SessionState.mvmntModality.add(Movement_Object(Movement_Mod.ON_FOOT, LocalDateTime.now(), confidence))
            }
            DetectedActivity.RUNNING -> {
                Log.d("Ö Movement", "RUNNING")
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
                Log.d("Ö Movement", "STILL")
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
                Log.d("Ö Movement", "WALKING")
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
            /*DetectedActivity.UNKNOWN -> {
                Log.d("Ö Movement", "UNKNOWN")
                label = getString(de.lmu.js.interruptionesm.R.string.activity_unknown)
                SessionState.mvmntModalityRecord.add(MovementRecord(MovementRecord.Movement.NONE, confidence))
                DatabaseRef.pushDB(eventType.MOVEMENT, eventValue.NONE, userKey, mapOf("Confidence" to confidence.toString()))
            }*/
        }
        Log.e("Interr Study", "User activity: $label, Confidence: $confidence")
        if (confidence > Constants.CONFIDENCE) {
           /* txtActivity!!.text = label
            txtConfidence!!.text = "Confidence: $confidence"
            imgActivity!!.setImageResource(icon)*/
        }
    }

    override fun onDestroy() {
        stopInterruption()
        stopSession()
        stopTracking()
        Log.d("Ö Session", "Closed App")
        Log.d("Ö Interruption", "Closed App")
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(activityReceiver!!)
        //Aware.stopAWARE(this)
        //TODO KILL APP WATCHER SERVCE?
        Log.d("Ö", "Pre Unreg")
        if(esmReceiver != null) unregisterReceiver(esmReceiver!!); esmReceiver = null;
        if(comReceiver != null) unregisterReceiver(comReceiver!!); comReceiver = null;
        if(activityReceiver != null) unregisterReceiver(activityReceiver!!); activityReceiver = null;
        if(sessionTimeoutRec != null) unregisterReceiver(sessionTimeoutRec!!); sessionTimeoutRec = null;
        stopService(Intent(this, TrackerWakelock::class.java))
        Log.d("Ö", "Post Unreg")
        super<AccessibilityService>.onDestroy()
    }


    fun startTracking(){
        Log.d("Ö", "Started Tracking")
        val intent = Intent(this, BackgroundDetectedActivitiesService::class.java)
        startService(intent)
        val intentNotification = Intent(this, NotificationLister::class.java)
        startService(intentNotification)

    }

    fun stopTracking() {
        Log.d("Ö", "Stop Tracking")
        //this@MainActivity
        val intent = Intent(this, BackgroundDetectedActivitiesService::class.java)
        stopService(intent)
        val intentNotification = Intent(this, NotificationLister::class.java)
        startService(intentNotification)
    }


    private fun startSession() {
        Log.d("Ö ", "In startSess")
        if (!SessionState.sessionStopped) return;
        SessionState.sessionStopped = false
        Log.d("Ö ", "In startSessY")
        SessionState.sessionId = LocalTime.now().hashCode();
        Log.d("Ö SessionID", SessionState.sessionId.toString())
        if(activityReceiver != null) regActivity()
        registerReceiver(activityReceiver, IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY))
        startTracking();


        var communicationFilter = IntentFilter();
        communicationFilter.addAction("android.intent.action.PHONE_STATE")
        communicationFilter.addAction("android.provider.Telephony.SMS_RECEIVED")
        if(comReceiver != null) regCom()
        registerReceiver(comReceiver, communicationFilter)

        DatabaseRef.pushDB(eventType.SESSION_START, eventValue.NONE, userKey)
        //startService(Intent(this, AppTrackerService::class.java))



    }

    private fun stopSession() {
        if (SessionState.sessionStopped) return;
        DatabaseRef.pushDB(eventType.SESSION_END, eventValue.NONE, userKey)
        Log.d("Ö", "Generate ESM")
        generateESM()

        //Reset Session
        SessionState.sessionStopped = true
        SessionState.interruptState = false;
        SessionState.mvmntModalityRecord = mutableListOf()

        Log.d("Ö", "Pre Unreg")
        if(comReceiver != null) unregisterReceiver(comReceiver!!); comReceiver = null;
        if(activityReceiver != null) unregisterReceiver(activityReceiver!!); activityReceiver = null;
        if(sessionTimeoutRec != null) unregisterReceiver(sessionTimeoutRec!!);  sessionTimeoutRec = null;

       stopService(Intent(this, TrackerWakelock::class.java))
        Log.d("Ö", "Post Unreg")
    }

    fun generateESM() {
        try {
            var factory = ESMFactory();
            var questionCounter = 0
            for (mov in mvmntModalityRecord) {
                Log.d("Ö ESM", "movement conf: " + mov.confidence + "movement type: " + mov.movement)
                if (mov.confidence >= 90 && (mov.movement != MovementRecord.Movement.NONE)) {

                    var movAnswer = ESM_Radio()
                    movAnswer.addRadio("Yes")
                        .addRadio("No")
                        .setInstructions("During your latest learning session, we detected the following activity performed by you:" + mov.movement.name +"\n Is that correct?")
                        .setSubmitButton("OK");
                    factory.addESM(movAnswer)

                    questionCounter++
                }
            }
            Log.d("Ö", "In")
            //ToDo Based on number of question issued in last ESM - need to retrieve Last Index - N -> Last Index
            SessionState.esmCounter = 2 + questionCounter


            var socialRadio = ESM_Radio();
            socialRadio.addRadio("Yes")
                .addRadio("No")
                .setInstructions("Were you alone during your latest session?")
                .setSubmitButton("OK");
            factory.addESM(socialRadio);

            var locationRadio = ESM_Radio();
            locationRadio.addRadio("Work")
                .addRadio("Home")
                .addRadio("Commute")
                .setInstructions("Were were you during your latest session?")
                .setSubmitButton("OK");
            factory.addESM(locationRadio);
            // Do we need to check for notification???

            //Queue them
            ESM.queueESM(this, factory.build()); } catch (e: JSONException) { Log.e("ESM ERROR", e.toString()) }

            DatabaseRef.pushDB(eventType.ESM_SENT, eventValue.NONE, userKey)
    }

    private fun startInterruption(eVal: eventValue, addProp: Map<String, String> = mapOf()) {
        if (SessionState.interruptState) return
        //check if duolingo?
        appSwitchList = mutableListOf<String>()
        SessionState.interruptState = true
        SessionState.interruptTmstmp = LocalDateTime.now()
        DatabaseRef.pushDB(eventType.INTERRUPTION_START, eVal, userKey, addProp)
        var wakeFilter = IntentFilter();
        wakeFilter.addAction("SESSION_TIMED_OUT")
        if(sessionTimeoutRec != null) regSess()
        registerReceiver(sessionTimeoutRec, wakeFilter)
        startService(Intent(this, TrackerWakelock::class.java))
    }

    private fun stopInterruption(addProp: Map<String, String> = mapOf()) {
        if (!SessionState.interruptState) return
Log.d("Ö this", addProp.toString())
        DatabaseRef.pushDB(eventType.INTERRUPTION_END, eventValue.NONE, userKey, addProp)
        SessionState.interruptState = false;

        stopService(Intent(this, TrackerWakelock::class.java))
    }

    private fun regActivity() {
        activityReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Constants.BROADCAST_DETECTED_ACTIVITY) {
                    val type = intent.getIntExtra("type", -1)
                    val confidence = intent.getIntExtra("confidence", 0)
                    handleUserActivity(type, confidence)
                }
            }
        }
    }
    private fun regCom() {
        comReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action.equals("android.provider.Telephony.SMS_RECEIVED")) {
                    DatabaseRef.pushDB(eventType.NOTIFICATION, eventValue.CALL, userKey)
                }
                if (intent.action.equals("android.intent.action.PHONE_STATE")) {
                    var state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    DatabaseRef.pushDB(eventType.NOTIFICATION, eventValue.SMS, userKey)
                }
                Log.d("Ö TTT", intent.toString())/*

                Log.d("TTT", state)
                if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                }*/

            }
        }
    }
    private fun regSess() {
        sessionTimeoutRec = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d("WakeLock", "OUT")
                if (SessionState.interruptState) {
                    stopInterruption(mapOf("switchedTo" to appSwitchList.joinToString()))
                    stopSession()
                    stopTracking()
                }
            }
        }
    }
    private fun regESM() {
        var esmFilter = IntentFilter();
        esmFilter.addAction(ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE)
        esmFilter.addAction(ESM.ACTION_AWARE_ESM_DISMISSED)
        esmFilter.addAction(ESM.ACTION_AWARE_ESM_EXPIRED)
        if(esmReceiver != null) regESM()
        registerReceiver(esmReceiver, esmFilter)
    }

}