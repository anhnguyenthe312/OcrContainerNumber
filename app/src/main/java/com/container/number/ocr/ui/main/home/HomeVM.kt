package com.container.number.ocr.ui.main.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.container.number.ocr.db.AppDatabase
import com.container.number.ocr.extension.logcat
import com.container.number.ocr.model.type.Evaluate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class HomeVM : ViewModel() {
    private val _ocrProgress = MutableLiveData<OcrProgress>()
    val ocrProgress: LiveData<OcrProgress> get() = _ocrProgress

    fun checkOrcProgress(context: Context, folderUri: Uri, numberOfItems: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.buildDatabase(context)
                .photoOcrDao()
                .getAllPhotoOcrInFolder(folderUri.toString())
                .distinctUntilChanged()
                .flowOn(Dispatchers.IO)

                .collect { list ->
                    var readOk = 0
                    var readNotGood = 0
                    var readNotStable = 0
                    logcat("list size = ${list.size}")
                    list.forEach {
                        when (it.evaluate) {
                            Evaluate.NOT_READ -> {
                            }
                            Evaluate.READ_OK -> readOk += 1
                            Evaluate.INCORRECT -> readNotStable += 1
                            Evaluate.READ_NOT_OK -> readNotGood += 1
                        }
                    }
                    val remaining = numberOfItems - readOk - readNotGood - readNotStable
                    _ocrProgress.postValue(
                        OcrProgress(
                            readOk,
                            readNotGood,
                            readNotStable,
                            remaining
                        )
                    )
                }
        }
    }

    data class OcrProgress(
        var readOk: Int,
        var readNotGood: Int,
        var readNotStable: Int,
        var remaining: Int
    )
}