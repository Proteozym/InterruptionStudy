package de.lmu.js.interruptionesm

//connect to database fun?
//how to handle user ident

class UserEvent(type: eventType, ident: eventIdentifier, eValue: eventValue) {
    lateinit var eventType: eventType
    lateinit var eventIdentifier: eventIdentifier
    lateinit var eventValue: eventValue

    init {
        eventType = type
        eventIdentifier = ident
        eventValue = eValue
    }

    fun pushEvent() {

    }


}

enum class eventType {
    SESSION, INTERRUPTION, MOVEMENT, COMMUNICATION
}

enum class eventIdentifier {
    START, END, CHANGE
}

enum class eventValue {
    SCREEN_LOCK, APP_SWITCH,
    WALKING, RUNNING, STILL, IN_VEHICLE, BYCICLE,
    CALL, SMS, NOTIFICATION
}