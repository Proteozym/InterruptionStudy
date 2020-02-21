package de.lmu.js.interruptionesm

import android.app.Activity
import android.app.Application
import android.content.Context
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class DatabaseRef: Application() {

    companion object {
        var dbInstance: FirebaseDatabase = FirebaseDatabase.getInstance()
        var dbRef: DatabaseReference = dbInstance.getReference()

        fun pushDB(type: eventType, value: eventValue, key: String) {
            dbRef.child("UserEvent").child(key).setValue(UserEvent(type, value))
        }
    }



}