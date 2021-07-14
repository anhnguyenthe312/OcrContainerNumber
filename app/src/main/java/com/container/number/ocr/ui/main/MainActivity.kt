package com.container.number.ocr.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.container.number.ocr.R
import com.container.number.ocr.ui.main.start.StartFragment

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainActivityVM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainActivityVM::class.java)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, StartFragment.newInstance())
                .commitNow()
        }
    }
}