package com.container.number.ocr.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.container.number.ocr.R
import com.container.number.ocr.model.type.OcrAlgorithm
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

    var currentAlgorithm: OcrAlgorithm = OcrAlgorithm.OneLine
}