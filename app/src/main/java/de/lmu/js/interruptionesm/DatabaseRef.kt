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
import com.google.firebase.Timestamp
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

        currentUserRef.document().set(UserEvent(type, value, key, addProp, Timestamp.now()))
    }





}
