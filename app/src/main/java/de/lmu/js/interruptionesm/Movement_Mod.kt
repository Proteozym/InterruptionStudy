package de.lmu.js.interruptionesm

import org.threeten.bp.LocalDateTime


enum class Movement_Mod {
    WALKING, STANDING, IN_VEHICLE, ON_BICYCLE, RUNNING, STILL, UNKNOWN
}

class Movement_Object(paramMovementMod: Movement_Mod, date: LocalDateTime, conf: Int) {
    lateinit var mvmnt: Movement_Mod
    lateinit var dateStmp: LocalDateTime
    var confidenceVal: Int = 0

        init {
            mvmnt = paramMovementMod
            dateStmp = date
            confidenceVal = conf
        }

}