package de.lmu.js.interruptionesm

import android.app.*
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings.Global.getString
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.annotation.NonNull
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.auth.User
import de.lmu.js.interruptionesm.utilities.Notification
import de.lmu.js.interruptionesm.utilities.SessionUtil
import de.lmu.js.interruptionesm.utilities.SessionUtil.Companion.checkPermSurvey
import kotlinx.coroutines.tasks.await
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter


object DatabaseRef {

    private val dbInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val currentUserRef: CollectionReference by lazy { dbInstance.collection("UserEvent") }
    private val dailyRef: CollectionReference by lazy { dbInstance.collection("DailyLog") }
    private val userSurveyReg: CollectionReference by lazy { dbInstance.collection("SurveyRegister") }

    fun pushDB(type: eventType, value: eventValue, key: String, addProp: Map<String, String> = mapOf()) {

        currentUserRef.document().set(UserEvent(type, value, key, addProp, Timestamp.now()))
    }

    fun pushDBDaily(packageName: String, key: String) {
        if (packageName.equals("com.android.systemui") || packageName.equals("is.shortcut") || packageName.equals("android") || packageName.equals("is.com.google.android.apps.nexuslauncher")) return

        dailyRef.document().set(object{

            val userKey: String = key
            val packageName:String = packageName
            val timestamp: Timestamp = Timestamp.now()
        })
    }

    fun addUserToSurvey(key: String, cont: Context) {
        val sharedPrefs = cont.getSharedPreferences("interruption_esm_pref", 0)
        if (!sharedPrefs.getBoolean("ENTRY", false)) {
            userSurveyReg.document(key).set(object {
                val userKey: String = key
                val hasCompletedInitSurvey: Boolean = false
                val hasCompletedFinSurvey: Boolean = false
                val timestamp: Timestamp = Timestamp.now()
            }).addOnSuccessListener {

                val editor = sharedPrefs.edit()
                editor.putBoolean("ENTRY", true)
                editor.apply();
                var succ = editor.commit();
            }
        }
    }


    fun confirmUserSurveyStart(key: String) {
        userSurveyReg.document(key).update("hasCompletedInitSurvey", true)
    }

    fun confirmUserSurveyFin(key: String) {
        userSurveyReg.document(key).update("hasCompletedFinSurvey", true)
    }


    fun verifyStartFromDB(key: String, context: Context) {
        val sharedPrefs = context.getSharedPreferences("interruption_esm_pref", 0)
        if (!sharedPrefs.getBoolean("ENTRY", false)) return
        val docRef = userSurveyReg.document(key)
        docRef.get()
            .addOnCompleteListener { document ->
                //if (document != null) {
                val sharedPrefs = context.getSharedPreferences("interruption_esm_pref", 0)
                val editor = sharedPrefs.edit()
                if (document.result?.get("hasCompletedInitSurvey") == true) {

                    editor.putBoolean("PERM", true)

                }
                else {
                    editor.putBoolean("PERM", false)
                }
                editor.apply();
                var succ = editor.commit();
            //}
            }
            .addOnFailureListener { exception ->
                Log.d("Ö", "get failed with ", exception)
            }
    }

    fun verifyFinFromDB(key: String, context: Context) {
        if(!checkPermSurvey(key, context)) return
        val docRef = userSurveyReg.document(key)

        //TIME CHECK
        docRef.get()
            .addOnCompleteListener { document ->
                val sharedPrefs = context.getSharedPreferences("interruption_esm_pref", 0)
                val editor = sharedPrefs.edit()
                if (document.result?.get("hasCompletedFinSurvey") == true) {

                    editor.putBoolean("FIN", true)
                    editor.putBoolean("FINREADY", false)

                }
                else {
                    var startTmp: Timestamp = document.result?.get("timestamp") as Timestamp
                    //2419200 = 4 Weeks
                    if ((Timestamp.now().seconds - startTmp.seconds) >= 2419200 && checkPermSurvey(key, context)) {
                        editor.putBoolean("FINREADY", true)
                        editor.putBoolean("FIN", false)
                        sendFinSurveyNotification(context)
                    }
                    else {
                        editor.putBoolean("FINREADY", false)
                        editor.putBoolean("FIN", false)
                    }
                }
                editor.apply();
                var succ = editor.commit();
                //}
            }
            .addOnFailureListener { exception ->
                Log.d("Ö", "get failed with ", exception)
            }
    }

    private fun sendFinSurveyNotification(cont: Context) {
        createNotificationChannel(cont)
// Create an explicit intent for an Activity in your app
        val intent = Intent(cont, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(cont, 0, intent, 0)

        val builder = NotificationCompat.Builder(cont, "lmu.channel2")
            .setSmallIcon(R.drawable.ic_assignment_black_24dp)
            .setContentTitle("LMU Study: Survey")
            .setContentText("Your final survey is available. Please complete it, to finish the study. You might need to restart the Activity Recognition App!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(cont)) {
            // notificationId is a unique int for each notification that you must define
            notify(1, builder.build())
        }
    }

    private fun createNotificationChannel(cont: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "lmu.channel"
            val descriptionText = "LMU Study Push Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("lmu.channel2", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                cont.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}
