package de.lmu.js.interruptionesm

import android.R
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.database.Cursor
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.aware.Aware
import com.aware.ESM
import com.aware.Screen
import com.aware.providers.ESM_Provider.ESM_Data
import com.aware.ui.esms.ESMFactory
import com.aware.ui.esms.ESM_Radio
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.google.android.gms.location.DetectedActivity
import com.google.firebase.Timestamp
import com.jakewharton.threetenabp.AndroidThreeTen
import de.lmu.js.interruptionesm.DatabaseRef.pushDBDaily
import de.lmu.js.interruptionesm.DatabaseRef.verifyFinFromDB
import de.lmu.js.interruptionesm.SessionState.Companion.mvmntModalityRecord
import de.lmu.js.interruptionesm.SessionState.Companion.screenLock
import de.lmu.js.interruptionesm.SessionState.Companion.sessionId
import de.lmu.js.interruptionesm.SessionState.Companion.sessionStart
import de.lmu.js.interruptionesm.utilities.Encrypt.Companion.encryptKey
import de.lmu.js.interruptionesm.utilities.Notification
import de.lmu.js.interruptionesm.utilities.SessionUtil
import de.lmu.js.interruptionesm.utilities.SessionUtil.Companion.checkPermSurvey
import de.lmu.js.interruptionesm.utilities.SessionUtil.Companion.checkSurveyFin
import org.json.JSONArray
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
    var userKey: String = ""
    var appSwitchList = mutableListOf<String>()
    val blockedAppList = mutableListOf<String>("com.jb.emoji.gokeyboard", "com.google.android.inputmethod.latin", "com.google.android.", "com.sec.android.inputmethod", "com.android.systemui", "com.touchtype.swiftkey", "com.google.android.inputmethod.latin", "com.syntellia.fleksy.keyboard", "com.gamelounge.chroomakeyboard", "com.gingersoftware.android.keyboard", "com.boloorian.android.farsikeyboard", "com.jb.gokeyboard")
    private var lastApp = ""
    var permissionToTrack = false
    var surveyFin = false
    protected val NOTIFICATION_ID = 1337

    var mCurrentService: InterruptionStudyService? = null
    var counter = 0

    var packToTrack = ""

    /*@Nullable
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
        if (userKey.isNullOrEmpty()) {
            try {
                userKey = encryptKey(Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID))
            } catch (e: java.lang.Exception) {Log.e("Ö", "Error Encrypting")}
        }

        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "restarting Service !!");
        counter = 0;
        // it has been killed by Android and now it is restarted. We must make sure to have reinitialised everything
        if (intent == null) {
            val bck = ProcessMainClass()
            bck.launchService(this)
        }

        // make sure you call the startForeground on onStartCommand because otherwise
        // when we hide the notification on onScreen it will nto restart in Android 6 and 7
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            restartForeground()
        }
        //startTimer();
        DatabaseRef.verifyStartFromDB(userKey, this)
        DatabaseRef.verifyFinFromDB(userKey, this)
        permissionToTrack = checkPermSurvey(userKey, this)
        surveyFin = checkSurveyFin(userKey, this)
        regActivity()
        //regCom()
        //regSess()
        if(permissionToTrack)regESM()
        trackScreen()
        readSystemDefaultApps()
        //checkForUpdate()
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo: ResolveInfo =
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        blockedAppList.add(resolveInfo.activityInfo.packageName)
        //startForeground()

        return START_STICKY;
        //return super.onStartCommand(intent, flags, startId)
    }


    @SuppressLint("ServiceCast")
    private fun readSystemDefaultApps() {
        val localPackageManager = packageManager
        val intent = Intent("android.intent.action.MAIN")
        intent.addCategory("android.intent.category.HOME")
        val str = localPackageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        ).activityInfo.packageName
        blockedAppList.add(str)

        val kbName = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        blockedAppList.add(kbName.split("/")[0])
    }

    /**
     * it starts the process in foreground. Normally this is done when screen goes off
     * THIS IS REQUIRED IN ANDROID 8 :
     * "The system allows apps to call Context.startForegroundService()
     * even while the app is in the background.
     * However, the app must call that service's startForeground() method within five seconds
     * after the service is created."
     */
    fun restartForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.i(TAG, "restarting foreground")
            try {
                val notification = Notification()
                startForeground(
                    NOTIFICATION_ID,
                    notification.setNotification(
                        this,
                        "Service notification",
                        "Data Collection for Interruption Study",
                        R.drawable.btn_star_big_on
                    )
                )
                Log.i(TAG, "restarting foreground successful")
                DatabaseRef.verifyStartFromDB(userKey, this)
                DatabaseRef.verifyFinFromDB(userKey, this)
                permissionToTrack = checkPermSurvey(userKey, this)
                surveyFin = checkSurveyFin(userKey, this)
                SessionUtil.checkSessionId(this)
                if(permissionToTrack)regESM()
                trackScreen()
                readSystemDefaultApps()
                //checkForUpdate()
            } catch (e: Exception) {
                Log.e(TAG, "Error in notification " + e.message)
            }
        }
    }

    fun checkForUpdate() {
        AppUpdater(this)
            .setUpdateFrom(UpdateFrom.JSON)
            .setUpdateJSON("https://gitcdn.link/repo/Proteozym/InterruptionStudy/master/app/update-changelog.json")
            .start();
    }


    fun trackScreen() {
        Screen.setSensorObserver(object : Screen.AWARESensorObserver {
            override fun onScreenLocked() {
                //if (!permissionToTrack || surveyFin) return
                handleScreenInterruption("lock")
            }

            override fun onScreenOff() {
                //if (!permissionToTrack || surveyFin) return
                handleScreenInterruption("off")
            }

            override fun onScreenOn() {
                if (!permissionToTrack || surveyFin) return
                Log.d("Ö", "Screen ON")
            }

            override fun onScreenUnlocked() {
                if (!permissionToTrack || surveyFin) return
                handleScreenInterruption("on")

            }
        })
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if(!permissionToTrack) permissionToTrack = checkPermSurvey(userKey, this)
        surveyFin = checkSurveyFin(userKey, this)
        if (!permissionToTrack || surveyFin) return
        Log.d("Ö", event.toString())
        if (userKey.isNullOrEmpty()) {
            try {
                userKey = encryptKey(Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID))
            } catch (e: java.lang.Exception) {Log.e("Ö", "Error Encrypting")}
        }
        //REMEMBER LAST APP
        if(esmReceiver==null)regESM()
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
               /* Log.d("Ö", event.toString())
                Log.d("Ö package name", event!!.getPackageName().toString())
                Log.d("Ö app name", appName)*/
                var packName = event!!.packageName.toString()
                if(!userKey.isNullOrEmpty())pushDBDaily(packName, userKey)

                //get selected app to track
                val sharedPref = getSharedPreferences(getString(de.lmu.js.interruptionesm.R.string.preference_key), Context.MODE_PRIVATE)
               // Log.d("LÖL", sharedPref.getString("APP", "empty")!!.split("|")[0])
                packToTrack = sharedPref.getString("APP", "empty")!!.split("|")[0]
                if (packToTrack.equals("empty")) {
                    Log.e("Interruption ESM", "App to track selection not working!")
                }
                else {
                    if (packName.equals(packToTrack)) { //de.lmu.js.interruptionesm
                        if (SessionState.sessionStopped) {
                            AndroidThreeTen.init(this.applicationContext)
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
                verifyFinFromDB(userKey, this)
            }
            }
        }


    override fun onCreate() {
        super<AccessibilityService>.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            restartForeground();
        }
        mCurrentService = this;
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

    private fun handleScreenInterruption(trigger: String) {
        Log.d("Ö", "Locked - " + trigger)
        permissionToTrack = checkPermSurvey(userKey, this)
        surveyFin = checkSurveyFin(userKey, this)
        if (surveyFin || !permissionToTrack) return
        if (!SessionState.sessionStopped) {

            if (!SessionState.interruptState) {
                if (trigger == "on") {

                    stopInterruption()
                    return
                }
                if (trigger == "lock") {
                    screenLock = true
                    startInterruption(eventValue.SCREEN_LOCK)
                }
                else {
                    screenLock = true
                    startInterruption(eventValue.SCREEN_OFF)
                }

            }

        }
    }

    private fun handleUserActivity(type: Int, confidence: Int) {
        var label = getString(de.lmu.js.interruptionesm.R.string.activity_unknown)
        Log.d("Ö Movement", "IN")
        if (SessionState.mvmntModalityRecord.isNullOrEmpty()) mvmntModalityRecord = mutableListOf(MovementRecord(MovementRecord.Movement.NONE, 100))
        when (type) {

            DetectedActivity.IN_VEHICLE -> {
                Log.d("Ö Movement", "IN_VEHICLE")
                if(SessionState.mvmntModalityRecord.last().movement != MovementRecord.Movement.IN_VEHICLE && confidence > 80)  {
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
                if(SessionState.mvmntModalityRecord.last().movement != MovementRecord.Movement.ON_BICYCLE && confidence > 80) {
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
                if(SessionState.mvmntModalityRecord.last().movement != MovementRecord.Movement.RUNNING && confidence > 80) {
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
                if(SessionState.mvmntModalityRecord.last().movement != MovementRecord.Movement.STILL && confidence > 80) {
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
                if(SessionState.mvmntModalityRecord.last().movement != MovementRecord.Movement.WALKING && confidence > 80) {
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
        try {

        if(permissionToTrack) {
            if (esmReceiver != null) {
                unregisterReceiver(esmReceiver)
                esmReceiver = null;
            }
            if (comReceiver != null) unregisterReceiver(comReceiver); comReceiver = null;
            if (activityReceiver != null) unregisterReceiver(activityReceiver); activityReceiver =
            null;
            if (sessionTimeoutRec != null) unregisterReceiver(sessionTimeoutRec); sessionTimeoutRec =
            null;

            stopService(Intent(this, TrackerWakelock::class.java))
        }
        } catch (e: java.lang.Exception) {

            Log.e("Ö", e.toString())
        }
        Log.d("Ö", "Post Unreg")
        super<AccessibilityService>.onDestroy()

        Log.i(TAG, "onDestroy called")
        // restart the never ending service
        // restart the never ending service
        val broadcastIntent = Intent("restarter")
        sendBroadcast(broadcastIntent)
        //stoptimertask()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.i(TAG, "onTaskRemoved called")
        // restart the never ending service
        val broadcastIntent = Intent("restarter")
        sendBroadcast(broadcastIntent)
        // do not call stoptimertask because on some phones it is called asynchronously
        // after you swipe out the app and therefore sometimes
        // it will stop the timer after it was restarted
        // stoptimertask();
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
        sessionStart = Timestamp.now()
        Log.d("Ö ", "In startSessY")
        SessionState.sessionId = LocalTime.now().hashCode();
        Log.d("Ö SessionID", SessionState.sessionId.toString())

        val prefFile = getString(de.lmu.js.interruptionesm.R.string.preference_key)
        val sharedPref = this.getSharedPreferences(
            prefFile, Context.MODE_PRIVATE)
        val editor = sharedPref.edit();

        editor.putInt("SessionId", SessionState.sessionId)

        editor.apply();

        var succ = editor.commit();

        if(activityReceiver == null) regActivity()
        registerReceiver(activityReceiver, IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY))
        startTracking();

        var communicationFilter = IntentFilter();
        communicationFilter.addAction("android.intent.action.PHONE_STATE")
        communicationFilter.addAction("android.provider.Telephony.SMS_RECEIVED")
        if(comReceiver == null) regCom()
        registerReceiver(comReceiver, communicationFilter)

        DatabaseRef.pushDB(eventType.SESSION_START, eventValue.NONE, userKey, mapOf("app" to packToTrack))
        //startService(Intent(this, AppTrackerService::class.java))

    }

    private fun stopSession() {
        if (SessionState.sessionStopped) return;
        DatabaseRef.pushDB(eventType.SESSION_END, eventValue.NONE, userKey)
        Log.d("Ö", "Generate ESM")
        generateESM()

        //Reset Session
        SessionState.screenLock = false
        SessionState.sessionStopped = true
        SessionState.interruptState = false;
        SessionState.mvmntModalityRecord = mutableListOf()

        Log.d("Ö", "Pre Unreg")
        try {
            if(comReceiver != null) unregisterReceiver(comReceiver!!); comReceiver = null;
            if(activityReceiver != null) unregisterReceiver(activityReceiver!!); activityReceiver = null;
            if(sessionTimeoutRec != null) unregisterReceiver(sessionTimeoutRec!!);  sessionTimeoutRec = null;
        }
        catch (err: java.lang.Exception) {Log.e("unreg err", err.toString())}

       stopService(Intent(this, TrackerWakelock::class.java))
        Log.d("Ö", "Post Unreg")
    }

    fun generateESM() {
        if (sessionStart == null) return

        if ((Timestamp.now().seconds - sessionStart.seconds)  < 30 ) return
        if (esmReceiver == null) regESM()
        val prefFile = getString(de.lmu.js.interruptionesm.R.string.preference_key)
        val sharedPref = this.getSharedPreferences(
            prefFile, Context.MODE_PRIVATE)
        var factory = ESMFactory();
        var queue = factory.queue
        factory.rebuild(JSONArray())
        //factory.removeESM()
        try {
           try {
                clearESMQueue()
            } catch (e: Exception) {Log.e("Ö ESM Queue Remove", e.toString())}
            val timeoutInt = 10800

            for (mov in mvmntModalityRecord) {
                Log.d("Ö ESM", "movement conf: " + mov.confidence + "movement type: " + mov.movement)
                if (mov.confidence >= 90 && (mov.movement != MovementRecord.Movement.NONE)) {

                    var movAnswer = ESM_Radio()
                    movAnswer.notificationTimeout = timeoutInt
                    movAnswer.addRadio("Yes")
                        .addRadio("No")
                        .setInstructions("During your latest learning session, we detected the following activity performed by you:" + mov.movement.name +"\n Is that correct?")
                        .setSubmitButton("OK");
                    factory.addESM(movAnswer)
                }
            }
            Log.d("Ö", "In")
            //ToDo Based on number of question issued in last ESM - need to retrieve Last Index - N -> Last Index


            val editor = sharedPref.edit();



            var succ = editor.commit();

            //SessionState.esmCounter = 2 + questionCounter

            var notificationRadio = ESM_Radio();
            notificationRadio.notificationTimeout = timeoutInt
            notificationRadio.addRadio("Yes")
                .addRadio("No")
                .setInstructions("Did you receive any distracting notifications during you latest sessions?")
                .setSubmitButton("OK");
            factory.addESM(notificationRadio);

            var socialRadio = ESM_Radio();
            socialRadio.notificationTimeout = timeoutInt
            socialRadio
                .addRadio("Alone.")
                .addRadio("With one person.")
                .addRadio("With more than one person.")
                .setInstructions("Were you alone or in company during your latest session?")
                .setSubmitButton("OK");
            factory.addESM(socialRadio);


            var locationRadio = ESM_Radio();
            locationRadio.notificationTimeout = timeoutInt
            locationRadio
                .addRadio("Work")
                .addRadio("Home")
                .addRadio("Commute")
                .addRadio("Traveling")
                .addRadio("Outdoors")
                .addRadio("Other")
                .setInstructions("Were were you during your latest session?")
                .setSubmitButton("OK");
            factory.addESM(locationRadio);
            // Do we need to check for notification???

            var cond = ESM_Radio();
            cond.notificationTimeout = timeoutInt
            cond.trigger = sessionId.toString()
            cond
                .addRadio("Very important- it was urgent / time-critical")
                .addRadio("Moderate - I had to do it eventually in the near future")
                .addRadio("Not important - I could have ignored it and continued learning")
                .setInstructions("How important was it that you follow up upon the interruption?")
                .setSubmitButton("OK")

            //if (screenLock) {
            var screenRadio = ESM_Radio();
            screenRadio.notificationTimeout = timeoutInt
            screenRadio.trigger = sessionId.toString()
            screenRadio.addRadio("I was done using the app.")
                .addRadio("I was interrupted by something on my phone (e.g. a notification, call, sms, email etc.)")
                .addRadio("I was distracted by something external to myself or the phone (e.g. doorbell, other people, having to get off of train etc.)")
                .addRadio("I was distracted internally (e.g. tiredness, couldn't concentrate, thinking of something else, mind-wandering etc.)")
                .setInstructions("Why did you end your learning session?")
                .setSubmitButton("OK")
                .addFlow("I was interrupted by something on my phone (e.g. a notification, call, sms, email etc.)", cond.build())
                .addFlow("I was distracted by something external to myself or the phone (e.g. doorbell, other people, having to get off of train etc.)", cond.build())
                .addFlow("I was distracted internally (e.g. tiredness, couldn't concentrate, thinking of something else, mind-wandering etc.)", cond.build())

            factory.addESM(screenRadio);
            var questionNmbr = factory.queue.length()

            //}

            editor.putInt("esmCounter", questionNmbr)

            editor.apply();

            //Queue them
            ESM.queueESM(this, factory.build()); } catch (e: JSONException) { Log.e("ESM ERROR", e.toString()) }


            DatabaseRef.pushDB(eventType.ESM_SENT, eventValue.NONE, userKey)
    }

    private fun clearESMQueue() {
        val esm = getContentResolver().query(
            ESM_Data.CONTENT_URI,
            null,
            ESM_Data.STATUS + " IN (" + ESM.STATUS_NEW + "," + ESM.STATUS_VISIBLE + "," + ESM.STATUS_DISMISSED +"," + ESM.STATUS_ANSWERED +"," + ESM.STATUS_EXPIRED +"," + ESM.STATUS_REPLACED +"," + ESM.STATUS_BRANCHED +")",
            null,
            null
        )

        if (esm != null && esm.moveToFirst()) {
            do {
                /*val rowData = ContentValues()
                rowData.put(
                    ESM_Data.ANSWER_TIMESTAMP,
                    System.currentTimeMillis()
                )
                rowData.put(ESM_Data.STATUS, ESM.STATUS_EXPIRED)*/
                getContentResolver().delete(
                    ESM_Data.CONTENT_URI,
                    ESM_Data._ID + "=" + esm.getInt(esm.getColumnIndex(ESM_Data._ID)),
                    null
                )
            } while (esm.moveToNext())
        }
        if (esm != null && !esm.isClosed) esm.close()

        if (Aware.DEBUG) Log.d("Ö", "Rest of ESM Queue is expired!")
       // val esm_done = Intent(ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE)
        //sendBroadcast(esm_done)
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
        if(sessionTimeoutRec == null) regSess()
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
                    DatabaseRef.pushDB(eventType.NOTIFICATION, eventValue.SMS, userKey)
                }
                if (intent.action.equals("android.intent.action.PHONE_STATE")) {
                    var state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    if (state == "RINGING") {
                        DatabaseRef.pushDB(eventType.NOTIFICATION, eventValue.CALL, userKey)
                    }
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
        //if (esmReceiver != null) {
            var esmFilter = IntentFilter();
            esmFilter.addAction(ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE)
            esmFilter.addAction(ESM.ACTION_AWARE_ESM_DISMISSED)
            esmFilter.addAction(ESM.ACTION_AWARE_ESM_EXPIRED)
            esmReceiver = ESMReceiver()
            registerReceiver(esmReceiver, esmFilter)
        //}
    }




    fun getmCurrentService(): InterruptionStudyService? {
        return mCurrentService
    }

    fun setmCurrentService(mCurrentService: InterruptionStudyService?) {
        this.mCurrentService = mCurrentService
    }

}