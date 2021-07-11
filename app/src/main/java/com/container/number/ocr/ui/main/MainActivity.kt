package com.container.number.ocr.ui.main

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.container.number.ocr.R
import com.container.number.ocr.extension.logcat
import com.container.number.ocr.model.type.OcrAlgorithm
import com.container.number.ocr.ui.main.home.HomeFragment
import com.container.number.ocr.ui.main.start.StartFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, StartFragment.newInstance())
                .commitNow()
        }
    }

    fun saveScreenShot(photoUri: Uri) {
        supportFragmentManager.findFragmentById(R.id.container)?.let {
            if (it is HomeFragment){
                it.takeScreenShot(photoUri)
            }
        }
    }

    var currentAlgorithm: OcrAlgorithm = OcrAlgorithm.OneLine
}