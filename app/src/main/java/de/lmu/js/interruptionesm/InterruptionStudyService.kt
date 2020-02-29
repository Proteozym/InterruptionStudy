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




//TODO IMPL MESSAGE INC
        comReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                var state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                Log.d("TTT", state)
                if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                }

            }
        }

        var communicationFilter = IntentFilter();
        communicationFilter.addAction("android.intent.action.PHONE_STATE")
        registerReceiver(comReceiver, communicationFilter)

        AndroidThreeTen.init(this);

        Aware.startAWARE(this)
        //Aware.startPlugins(this)
        Aware.startScreen(this)
        //Aware.startCommunication(this)

        Aware.setSetting(this, Aware_Preferences.DEBUG_FLAG, false)

        // Register for checking application use
        Aware.setSetting(this, Aware_Preferences.STATUS_COMMUNICATION_EVENTS, false)
        Aware.setSetting(this, Aware_Preferences.STATUS_APPLICATIONS, false)
        Aware.setSetting(this, Aware_Preferences.STATUS_NOTIFICATIONS, false)
        Aware.setSetting(this, Aware_Preferences.STATUS_ESM, false)

        Applications.isAccessibilityServiceActive(this)


        Communication.setSensorObserver(object: Communication.AWARESensorObserver {
            override fun onCall(data: ContentValues?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                Log.d("TTT", "etsdtsf")
            }

            override fun onMessage(data: ContentValues?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onBusy(number: String?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onRinging(number: String?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onFree(number: String?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
                    if (SessionState.sessionId == 0) {
                        startSession()
                        generateESM()
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

                            if(packName != "com.android.systemui")  {
                                //startInterruption(eventValue.SCREEN_LOCK)
                                Log.d("Ö Interruption", "Started")
                                startInterruption(eventValue.APP_SWITCH, mapOf("receivedCall" to receivedCall.toString(), "receivedMessage" to receivedMessage.toString()))
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
        var label = getString(de.lmu.js.interruptionesm.R.string.activity_unknown)
        var icon = de.lmu.js.interruptionesm.R.drawable.ic_still

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
                    icon = de.lmu.js.interruptionesm.R.drawable.ic_driving
                }
            }
            DetectedActivity.ON_BICYCLE -> {
                if(SessionState.mvmntModalityRecord.last().movement != MovementRecord.Movement.ON_BICYCLE) {
                    label = getString(de.lmu.js.interruptionesm.R.string.activity_on_bicycle)
                    SessionState.mvmntModalityRecord.add(MovementRecord(MovementRecord.Movement.ON_BICYCLE, confidence))
                    DatabaseRef.pushDB(
                        eventType.MOVEMENT,
                        eventValue.BYCICLE,
                        userKey,
                        mapOf("Confidence" to confidence.toString())
                    )
                    icon = de.lmu.js.interruptionesm.R.drawable.ic_on_bicycle
                }
            }
            DetectedActivity.ON_FOOT -> {

                //DO WE NEED THIS?

                label = getString(de.lmu.js.interruptionesm.R.string.activity_on_foot)
                //SessionState.mvmntModality.add(Movement_Object(Movement_Mod.ON_FOOT, LocalDateTime.now(), confidence))
                icon = de.lmu.js.interruptionesm.R.drawable.ic_walking
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
                    icon = de.lmu.js.interruptionesm.R.drawable.ic_running
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
            DetectedActivity.TILTING -> {

                //DO WE NEED THIS?

                label = getString(de.lmu.js.interruptionesm.R.string.activity_tilting)
                //SessionState.mvmntModality.add(Movement_Object(Movement_Mod.ON_BICYCLE, LocalDateTime.now(), confidence))
                icon = de.lmu.js.interruptionesm.R.drawable.ic_tilting
            }
            DetectedActivity.WALKING -> {
                if(SessionState.mvmntModalityRecord.last().movement != MovementRecord.Movement.WALKING) {
                    label = getString(de.lmu.js.interruptionesm.R.string.activity_walking)
                    //SessionState.mvmntModality.add(Movement_Object(Movement_Mod.WALKING, LocalDateTime.now(), confidence))
                    DatabaseRef.pushDB(
                        eventType.MOVEMENT,
                        eventValue.WALKING,
                        userKey,
                        mapOf("Confidence" to confidence.toString())
                    )
                    icon = de.lmu.js.interruptionesm.R.drawable.ic_walking
                }
            }
            DetectedActivity.UNKNOWN -> {
                label = getString(de.lmu.js.interruptionesm.R.string.activity_unknown)
                //SessionState.mvmntModality.add(Movement_Object(Movement_Mod.UNKNOWN, LocalDateTime.now(), confidence))
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
        if (SessionState.sessionId != 0) return;
        Log.d("Ö ", "In startSessY")
        SessionState.sessionId = LocalTime.now().hashCode();
        Log.d("Ö SessionID", SessionState.sessionId.toString())
        LocalBroadcastManager.getInstance(this).registerReceiver(activityReceiver!!, IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY))
        startTracking();
        DatabaseRef.pushDB(eventType.SESSION_START, eventValue.NONE, userKey)
        startService(Intent(this, AppTrackerService::class.java))
    }

    private fun stopSession() {
        if (SessionState.sessionId == 0) return;
        DatabaseRef.pushDB(eventType.SESSION_END, eventValue.NONE, userKey)
        generateESM()

        //Reset Session
        SessionState.sessionId = 0
        SessionState.interruptState = false;
        SessionState.mvmntModalityRecord = mutableListOf(MovementRecord(MovementRecord.Movement.NONE, 100))
        SessionState.esmCounter = 0
    }

    private fun generateESM() {
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
            //ToDo Based on number of question issued in last ESM - need to retrieve Last Index - N -> Last Index
            SessionState.esmCounter = questionCounter

          /*  var esmFreetext = ESM_Freetext();
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

            //Queue them*/
            ESM.queueESM(this, factory.build()); } catch (e: JSONException) { Log.e("ESM ERROR", e.toString()) }

            DatabaseRef.pushDB(eventType.ESM_SENT, eventValue.NONE, userKey)
    }

    private fun startInterruption(eVal: eventValue, addProp: Map<String, String> = mapOf()) {
        if (SessionState.interruptState) return
        SessionState.interruptState = true;
        SessionState.interruptTmstmp = LocalDateTime.now()
        DatabaseRef.pushDB(eventType.INTERRUPTION_START, eVal, userKey, addProp)

    }

    private fun stopInterruption() {
        if (!SessionState.interruptState) return
        SessionState.interruptState = false;
        DatabaseRef.pushDB(eventType.INTERRUPTION_END, eventValue.NONE, userKey)

    }

}