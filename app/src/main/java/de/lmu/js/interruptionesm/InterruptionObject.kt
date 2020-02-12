package de.lmu.js.interruptionesm

enum class InterruptType {
    SCREENLOCK, INACTIVITY, APPLICATION_SWITCH, NONE
}

enum class Trigger {
    CALL, MESSAGE, NOTFICATION, NONE
}

class InterruptionObject(type: InterruptType, trig: Trigger) {

    var interruptType: Enum<InterruptType> = InterruptType.NONE;
    var lastLong: Long = 0;
    var lastLat: Long = 0;
    var trigger: Enum<Trigger> = Trigger.NONE;

    lateinit var startTime: org.threeten.bp.LocalDateTime;
    lateinit var endTime: org.threeten.bp.LocalDateTime;

    init {
        interruptType = type;
        trigger = trig;
    }


}