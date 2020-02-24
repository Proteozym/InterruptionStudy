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

        startService(Intent(this@MainActivity, InterruptionStudyService::class.java))

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

    }


}


