package com.container.number.ocr.ui.main

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.container.number.ocr.model.data.Event

class MainActivityVM : ViewModel() {
    val startScreenShot: MutableLiveData<Event<Uri>> = MutableLiveData()
    val endScreenShot: MutableLiveData<Boolean> = MutableLiveData()
}