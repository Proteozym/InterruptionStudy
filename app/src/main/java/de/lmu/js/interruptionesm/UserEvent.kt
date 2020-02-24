package de.lmu.js.interruptionesm

import org.threeten.bp.LocalDateTime
import java.net.IDN
import java.util.*

//connect to database fun?
//how to handle user ident

class UserEvent(type: eventType, eValue: eventValue, key: String, addProp: Map<String, String>) {
    var eventType: eventType
    var eventValue: eventValue
    var timeStamp: LocalDateTime
    var userKey: String
    var sessionId: Int = SessionState.sessionId
    var additionalProps: Map<String, String>

    init {
        eventType = type
        eventValue = eValue
        timeStamp = LocalDateTime.now()
        userKey = key

        additionalProps = addProp
    }

}

enum class eventType {
    SESSION_START, SESSION_END, INTERRUPTION_START, INTERRUPTION_END, MOVEMENT, COMMUNICATION, ESM_SENT, ESM_ANSWER, ESM_EXPIRED
}

enum class eventValue {
    SCREEN_LOCK, SCREEN_OFF, APP_SWITCH,
    WALKING, RUNNING, STILL, IN_VEHICLE, BYCICLE,
    CALL, SMS, NOTIFICATION,
    NONE
}