package com.container.number.ocr.extension

import android.util.Log

fun logcat(msg: String) {
    Log.i("log_logigram", msg)
}

fun logcatService(msg: String) {
    Log.i("logigram_service", msg)
}

fun logcat(ex: Throwable?) {
    ex?.apply {
        ex.printStackTrace()
    }
}

fun logcat(tag: String, msg: String) {
    Log.i(tag, msg)
}