package de.lmu.js.interruptionesm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import de.lmu.js.interruptionesm.utilities.Encrypt
import de.lmu.js.interruptionesm.utilities.SessionUtil


class NotificationLister: NotificationListenerService() {

    var isDoublicateNotificationPrevention = ""
    val blockedAppList = mutableListOf<String>("com.google.android.apps.messaging", "android")
    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {

        if (!blockedAppList.contains(sbn?.packageName).equals(!SessionState.sessionStopped)) {
            if (sbn?.packageName != isDoublicateNotificationPrevention) {
              isDoublicateNotificationPrevention = sbn!!.packageName
                SessionUtil.checkSessionId(this)

                DatabaseRef.pushDB(
                    eventType.NOTIFICATION,
                    eventValue.PUSH,
                    Encrypt.encryptKey(Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)),
                    mapOf(
                        "app" to sbn?.packageName!!,
                        "notification_tags" to mapOf(
                            "priority" to sbn?.notification!!.priority.toString(),
                            "vibrate" to sbn?.notification!!.vibrate,
                            "sound" to sbn?.notification!!.sound,
                            "group" to sbn?.notification!!.group,
                            "tag" to sbn?.tag
                        ).toString()
                    )
                )
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Implement what you want here
    }


}