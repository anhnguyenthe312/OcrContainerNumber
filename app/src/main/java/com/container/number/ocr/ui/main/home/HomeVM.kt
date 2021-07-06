package com.container.number.ocr.ui.main.home

import android.R.string
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.container.number.ocr.db.AppDatabase
import com.container.number.ocr.extension.logcat
import com.container.number.ocr.model.data.Resource
import com.container.number.ocr.model.entity.PhotoOcr
import com.container.number.ocr.model.type.Evaluate
import com.container.number.ocr.utils.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.FileOutputStream


class HomeVM : ViewModel() {
    private val _ocrProgress = MutableLiveData<OcrProgress>()
    val ocrProgress: LiveData<OcrProgress> get() = _ocrProgress

    private var _folderUri: Uri = Uri.EMPTY
    fun checkOrcProgress(context: Context, folderUri: Uri, numberOfItems: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _folderUri = folderUri
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


    private val _exportProgress = MutableLiveData<Resource<Boolean>>()
    val exportProgress: LiveData<Resource<Boolean>> get() = _exportProgress

    fun export(context: Context, exportUriFolder: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _exportProgress.postValue(Resource.loading())
            val listOrcPhoto = AppDatabase.buildDatabase(context)
                .photoOcrDao()
                .getAllPhotoOcrInFolderNotFlow(_folderUri.toString())
            listOrcPhoto.forEach {
                cloneAndDrawContainerPhoto(context, it, exportUriFolder)
            }
            _exportProgress.postValue(Resource.success(true))
        }
    }

    private fun cloneAndDrawContainerPhoto(
        context: Context,
        photoOcr: PhotoOcr,
        exportUriFolder: Uri
    ) {
        val uri = Uri.parse(photoOcr.uriStr)
        DocumentFile.fromSingleUri(context, uri)?.let { documentFile ->
            DocumentFile.fromTreeUri(context, exportUriFolder)?.let { exportUri ->
                val cloneFile = exportUri.createFile("image/png", documentFile.name!!)
                val outputStream = context.contentResolver.openOutputStream(cloneFile!!.uri)
                BitmapUtils.getBitmapFromUri(context, uri, 10 * 1024 * 1024)?.let { bitmap ->
                    if (photoOcr.containerNumber.isEmpty()) {

                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    } else {
                        val bitmapNew = BitmapUtils.drawBoundingBox(
                            bitmap, photoOcr.boundingRect, photoOcr.containerNumber,
                            context.getString(photoOcr.evaluate.resId)
                        )
                        bitmapNew.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                    outputStream?.flush()
                    outputStream?.close()
                }
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