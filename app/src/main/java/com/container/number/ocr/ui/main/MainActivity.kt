package com.container.number.ocr.ui.main

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.container.number.ocr.R
import com.container.number.ocr.extension.logcat
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return  when(item.itemId){
//            R.id.export -> {
//                logcat("asdasdadasdasd")
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//
//    }
}