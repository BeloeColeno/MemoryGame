package com.petrov.memory

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

/**
 * Application класс для инициализации Firebase
 */
class MemoryGameApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Инициализируем Firebase
        try {
            FirebaseApp.initializeApp(this)
            
            // Включаем offline persistence для лучшей работы
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
            
            android.util.Log.d("MemoryGameApp", "Firebase initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("MemoryGameApp", "Firebase initialization failed", e)
        }
    }
}
