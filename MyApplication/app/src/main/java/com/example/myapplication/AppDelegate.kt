package com.example.myapplication

import android.app.Application
import androidx.room.Room

class AppDelegate: Application() {

    private lateinit var appDatabase: AppDatabase

    override fun onCreate() {
        super.onCreate()

        appDatabase = Room.databaseBuilder(this, AppDatabase::class.java, "app_database")
            .allowMainThreadQueries()
            .build()
    }

    fun getAppDatabase(): AppDatabase {
        return appDatabase
    }
}