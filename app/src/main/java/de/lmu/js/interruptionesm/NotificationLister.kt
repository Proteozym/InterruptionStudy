package de.lmu.js.interruptionesm

import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log


class NotificationLister: NotificationListenerService() {

    var isDoublicateNotificationPrevention = false
    val blockedAppList = mutableListOf<String>("com.google.android.apps.messaging", "android")
    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {

        if (!blockedAppList.contains(sbn?.packageName) && !SessionState.sessionStopped) {
            if (isDoublicateNotificationPrevention) {
                isDoublicateNotificationPrevention = false
            }
            else {
                isDoublicateNotificationPrevention = true
                DatabaseRef.pushDB(
                    eventType.NOTIFICATION,
                    eventValue.PUSH,
                    Settings.Secure.getString(this?.contentResolver, Settings.Secure.ANDROID_ID),
                    mapOf("app" to sbn?.packageName!!,
                        "notification_tags" to mapOf("priority" to sbn?.notification!!.priority.toString(),
                            "vibrate" to sbn?.notification!!.vibrate,
                            "sound" to sbn?.notification!!.sound,
                            "group" to sbn?.notification!!.group,
                            "tag" to sbn?.tag).toString())
                )
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Implement what you want here
    }
}