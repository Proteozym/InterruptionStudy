package de.lmu.js.interruptionesm

import android.app.Application
import android.content.Context
import java.time.LocalDate

class SessionState : Application() {

    override fun onCreate() {
        super.onCreate()
        SessionState.appContext = applicationContext


    }

    companion object {

        lateinit  var appContext: Context;

        var sessionId: Int = 0
        lateinit var startTime: org.threeten.bp.LocalDateTime;
        lateinit var endTime: org.threeten.bp.LocalDateTime;

        var interruptState: Boolean = false;
        lateinit var interruptionObj: InterruptionObject;

        var mvmntModality: Movement_Mod = Movement_Mod.NONE;
    }

}