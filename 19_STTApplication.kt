package com.sam.stt

import android.app.Application
import com.google.firebase.FirebaseApp
import com.sam.stt.data.STTDatabase

class STTApplication : Application() {

    val database by lazy { STTDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()

        // Initialiser Firebase
        FirebaseApp.initializeApp(this)

        // La base Room est initialisée en lazy (au premier accès)
    }
}
