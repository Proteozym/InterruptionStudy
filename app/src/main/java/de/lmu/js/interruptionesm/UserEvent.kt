package de.lmu.js.interruptionesm

import org.threeten.bp.LocalDateTime

//connect to database fun?
//how to handle user ident

class UserEvent(type: eventType, eValue: eventValue) {
    var eventType: eventType
    var eventValue: eventValue
    var timeStamp: LocalDateTime

    init {
        eventType = type
        eventValue = eValue
        timeStamp = LocalDateTime.now()
    }

}

enum class eventType {
    SESSION_START, SESSION_END, INTERRUPTION_START, INTERRUPTION_END, MOVEMENT, COMMUNICATION
}

enum class eventValue {
    SCREEN_LOCK, APP_SWITCH,
    WALKING, RUNNING, STILL, IN_VEHICLE, BYCICLE,
    CALL, SMS, NOTIFICATION,
    NONE
}