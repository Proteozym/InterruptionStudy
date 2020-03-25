package de.lmu.js.interruptionesm

import android.app.Application
import android.content.Context
import com.google.android.gms.location.DetectedActivity
import java.time.LocalDate

class SessionState : Application() {

    override fun onCreate() {
        super.onCreate()
        SessionState.appContext = applicationContext

    }

    companion object {

        lateinit  var appContext: Context;
        var sessionStopped: Boolean = true
        var sessionId: Int = 0
        var interruptState: Boolean = false;
        lateinit var interruptTmstmp: org.threeten.bp.LocalDateTime
        var mvmntModalityRecord: MutableList<MovementRecord> = mutableListOf(MovementRecord(MovementRecord.Movement.NONE, 100))
        var esmCounter: Int = 0
        var esmWasDismissed: Boolean = false

    }

}

