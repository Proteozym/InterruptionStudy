package de.lmu.js.interruptionesm
import com.google.firebase.Timestamp
import org.threeten.bp.LocalDateTime


data class UserEvent(
    val eventType: eventType? = null,
    val eventValue: eventValue? = null,
    val userKey: String = "",
    val additionalProps: Map<String, String> = mapOf(),
    val timestamp: Timestamp = Timestamp.now(),
    val sessionId: Int = SessionState.sessionId
)

enum class eventType {
    SESSION_START, SESSION_END, INTERRUPTION_START, INTERRUPTION_END, MOVEMENT, COMMUNICATION, ESM_SENT, ESM_ANSWER, ESM_EXPIRED, NONE
}

enum class eventValue {
    SCREEN_LOCK, SCREEN_OFF, APP_SWITCH,
    WALKING, RUNNING, STILL, IN_VEHICLE, BYCICLE,
    CALL, SMS, NOTIFICATION,
    NONE
}