package de.lmu.js.interruptionesm

import android.app.Activity
import android.app.Application
import android.content.Context
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore


object DatabaseRef {

    private val dbInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val currentUserRef: CollectionReference by lazy { dbInstance.collection("UserEvent") }

    fun pushDB(type: eventType, value: eventValue, key: String, addProp: Map<String, String> = mapOf()) {

        currentUserRef.document().set(UserEvent(type, value, key, addProp))
    }

}