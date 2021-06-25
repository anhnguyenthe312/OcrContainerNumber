package com.container.number.ocr

import android.app.Application
import com.container.number.ocr.db.AppDatabase

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        AppDatabase.buildDatabase(this)
    }
}