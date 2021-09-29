package com.container.number.ocr.ui.main.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.view.View
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.container.number.ocr.db.AppDatabase
import com.container.number.ocr.model.data.Event
import com.container.number.ocr.model.data.Resource
import com.container.number.ocr.model.entity.PhotoOcr
import com.container.number.ocr.model.type.Evaluate
import com.container.number.ocr.model.type.OcrAlgorithm
import com.container.number.ocr.utils.BitmapUtils
import com.container.number.ocr.utils.ContainerNumberUtils
import com.google.mlkit.vision.text.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PhotoVM : ViewModel() {

    var originalBitmap: Bitmap? = null
        private set

    private val _originalBitmapEvent = MutableLiveData<Event<Resource<Boolean>>>()
    val originalBitmapEvent: LiveData<Event<Resource<Boolean>>> get() = _originalBitmapEvent

    private var _photoBitmap = MutableLiveData<Bitmap>()
    val photoBitmap: LiveData<Bitmap> get() = _photoBitmap

    private val _startOcrOnPhoto = MutableLiveData<Event<Boolean>>()
    val startOcrOnPhoto: LiveData<Event<Boolean>> get() = _startOcrOnPhoto

    private val _updatePhotoOcrToUI = MutableLiveData<Event<PhotoOcr>>()
    val updatePhotoOcrToUI: LiveData<Event<PhotoOcr>> get() = _updatePhotoOcrToUI

    private var _containerNumber: String = ""
    private var _containerNumberLiveData = MutableLiveData<Event<String>>()
    val containerNumberLiveData: LiveData<Event<String>> get() = _containerNumberLiveData

    private var savedDrawRect: Rect? = null

    var currentAlgorithm: OcrAlgorithm = OcrAlgorithm.OneLine

    val saveDbSuccess: MutableLiveData<Event<Boolean>> = MutableLiveData()

    fun loadPhotoFromUri(context: Context, photoUri: Uri) {
        viewModelScope.launch {
            _originalBitmapEvent.postValue(Event(Resource.loading()))
            originalBitmap =
                BitmapUtils.getBitmapFromUri(
                    context,
                    photoUri,
                    10 * 1024 * 1024
                )
            if (originalBitmap != null)
                _originalBitmapEvent.postValue(Event(Resource.success(true)))
            else _originalBitmapEvent.postValue(Event(Resource.error(Exception())))
        }
    }

    fun checkPhotoOcrExist(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val photoOcr = AppDatabase.buildDatabase(context).photoOcrDao().getByUriStr(uriStr = uri.toString())
            if (photoOcr != null){
                _updatePhotoOcrToUI.postValue(Event(photoOcr))
                _containerNumber = photoOcr.containerNumber
                savedDrawRect = photoOcr.boundingRect
            }
            else{
                _startOcrOnPhoto.postValue(Event(true))
            }
        }

    }

    fun onRecognized(text: Text, ocrAlgorithm: OcrAlgorithm) {
        viewModelScope.launch(Dispatchers.IO) {
            if (originalBitmap != null) {
                //recognize container number
                val (containerNumber, rect) =
                    when(ocrAlgorithm){
                        OcrAlgorithm.OneLine -> ContainerNumberUtils.getContainerNumber(text, ContainerNumberUtils.BoundRectType.CORRECT_BOUND)
                        OcrAlgorithm.TwoLine -> ContainerNumberUtils.getContainerNumber2Line(text, ContainerNumberUtils.BoundRectType.CORRECT_BOUND)
                        OcrAlgorithm.Vertical -> ContainerNumberUtils.getContainerNumberVertical(text, ContainerNumberUtils.BoundRectType.CORRECT_BOUND)
                    }
                if (containerNumber.isNotEmpty()){
                    _containerNumberLiveData.postValue(Event(containerNumber))
                    _containerNumber = containerNumber
                    savedDrawRect = rect
                    val boundingBitmap =
                        BitmapUtils.drawBoundingBox(originalBitmap!!, rect, containerNumber, "")
                    _photoBitmap.postValue(boundingBitmap)
                }
            }
        }
    }

    fun drawRectOnly(rect: Rect, containerNumber: String, evaluate: String){
        viewModelScope.launch {
            savedDrawRect = rect
            val boundingBitmap =
                BitmapUtils.drawBoundingBox(originalBitmap!!, rect, containerNumber, evaluate)
            _photoBitmap.postValue(boundingBitmap)
        }
    }

    fun saveEvaluate(context: Context, photoUri: Uri, evaluate: Evaluate, algorithm: OcrAlgorithm) {
        viewModelScope.launch(Dispatchers.IO) {
            val photoOcr = PhotoOcr().apply {
                this.uriStr = photoUri.toString()
                this.boundingRect = savedDrawRect ?: Rect()
                this.containerNumber = _containerNumber
                this.evaluate = evaluate
                this.algorithm = algorithm
            }
            AppDatabase.buildDatabase(context).photoOcrDao().insert(photoOcr)
            saveDbSuccess.postValue(Event(true))
        }

    }
}