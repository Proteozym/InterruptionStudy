package de.lmu.js.interruptionesm

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.annotation.NonNull
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.auth.User
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter


object DatabaseRef {

    private val dbInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val currentUserRef: CollectionReference by lazy { dbInstance.collection("UserEvent") }

    fun pushDB(type: eventType, value: eventValue, key: String, addProp: Map<String, String> = mapOf()) {

        currentUserRef.document().set(UserEvent(type, value, key, addProp, LocalDateTime.now()))
    }

    fun populateListWithSessionsForUserKey(key: String, listView: ListView, context: Context) {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        currentUserRef
            .whereEqualTo("userKey", key).
            get().addOnCompleteListener {
                @Override
                fun onComplete(@NonNull task: Task<QuerySnapshot>) {
                    var eventList: MutableList<UserEvent> = mutableListOf<UserEvent>()
                    var listArray = arrayOfNulls<String>(task.result!!.size())
                    var i = 0
                    Log.d("IIIIN", "In")
                    if (task.isSuccessful()) {
                        for (document in task.result!!) {

                            var event = document.toObject(UserEvent::class.java)
                            Log.d("IIIIN", event.toString())
                            listArray[i] = formatter.format(event.timestamp)
                            eventList.add(event);
                            i++
                        }

                        val adapter = ArrayAdapter(
                            context,
                            R.layout.listview_item, listArray
                        )
                    } else {
                        Log.d("MissionActivity", "Error getting documents: ", task.getException());
                    }
                }
            }
        /*.addOnSuccessListener { documents ->
            if (documents != null) {

                for (i in 0 until documents.size()) {
                    documents.
                    listArray[i] = formatter.format(documents[i].timestamp)
                    eventList.add(document.toObject(UserEvent::class.java))
                }

            } else {
                Log.d("PULL", "No such document")
            }
        }
        .addOnFailureListener { exception ->
            Log.d("PULL", "get failed with ", exception)
        }*/
    }


}
