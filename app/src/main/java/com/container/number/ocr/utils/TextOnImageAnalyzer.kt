package com.container.number.ocr.utils

import android.graphics.Bitmap
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognizer

class TextOnImageAnalyzer {
    companion object {
        fun analyzeAnImage(
            recognizer: TextRecognizer,
            inputImage: InputImage,
            croppedBitmap: Bitmap?,
            listener: TextRecognizedListener?
        ): Task<Text> {
            return recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    listener?.onRecognized(visionText, croppedBitmap)
                }
                .addOnFailureListener { e ->
                    listener?.onRecognizedError(e)
                }
        }
    }

    interface TextRecognizedListener {
        fun onRecognized(text: Text, croppedBitmap: Bitmap?)
        fun onRecognizedError(e: Exception)
    }
}
