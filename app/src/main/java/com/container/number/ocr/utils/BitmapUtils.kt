package com.container.number.ocr.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.Bitmap.CompressFormat
import android.media.Image
import android.net.Uri
import android.os.Environment
import android.view.View
import android.view.View.MeasureSpec
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.Glide
import com.container.number.ocr.R
import com.container.number.ocr.constant.Constants
import com.container.number.ocr.extension.logcat
import com.container.number.ocr.model.data.TextOnBitmap
import com.container.number.ocr.model.type.BoundingBoxType
import com.google.mlkit.vision.text.Text
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.roundToInt
import kotlin.math.sqrt


object BitmapUtils {
    const val JPEG_COMPRESS_RATIO = 3.3F

    fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
        var fileOutputStream: FileOutputStream? = null
        return try {
            if (Environment.getExternalStorageState(file) == Environment.MEDIA_MOUNTED) {
                fileOutputStream = FileOutputStream(file)
                bitmap.compress(CompressFormat.JPEG, 100, fileOutputStream)
                fileOutputStream.close()
                true
            } else false
        } catch (ex: Exception) {
            fileOutputStream?.close()
            logcat(ex)
            false
        }
    }

    fun getBitmapFromUri(context: Context, uri: Uri, maxSizeInByte: Long): Bitmap? {
        return try {
            val rotationDegree = getRotateDegree(context, uri)
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            options.inJustDecodeBounds = true
            var imageStream = context.contentResolver.openInputStream(uri)
            return if (imageStream != null) {
                BitmapFactory.decodeStream(imageStream, null, options)
                // calculate height and width
                val allByte = options.outWidth * options.outHeight * Constants.BYTE_PER_PIXEL
                if (allByte > maxSizeInByte) {
                    val widthxHeight = maxSizeInByte / Constants.BYTE_PER_PIXEL * JPEG_COMPRESS_RATIO
                    val aspectRatio = options.outWidth * 1.0F / options.outHeight
                    val newWidth = sqrt(widthxHeight * aspectRatio).toInt()
                    val newHeight = (widthxHeight / newWidth).toInt()
                    options.inSampleSize = calculateInSampleSize(options, newWidth, newHeight)
                }
                imageStream.close()
                imageStream = context.contentResolver.openInputStream(uri)
                options.inJustDecodeBounds = false
                var img = BitmapFactory.decodeStream(imageStream, null, options)
                if (img != null)
                    img = rotateImage(img, rotationDegree)
                imageStream?.close()
                img
            } else null
        } catch (ex: Exception) {
            logcat(ex)
            null
        }
    }

    private fun getRotateDegree(context: Context, uri: Uri): Float {
        var rotationDegree = 0F
        try {
            val inputStream: InputStream =
                context.contentResolver.openInputStream(uri) ?: return rotationDegree
            val exif = ExifInterface(inputStream)
            inputStream.close()
            rotationDegree = getRotationDegree(exif)
        } catch (ex: Exception) {
            logcat(ex)
        }
        return rotationDegree
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        val stretchWidth = (width.toFloat() / reqWidth.toFloat()).roundToInt()
        val stretchHeight = (height.toFloat() / reqHeight.toFloat()).roundToInt()
        return if (stretchWidth <= stretchHeight) stretchHeight else stretchWidth
    }

    fun getRotationDegree(exifInterface: ExifInterface): Float {
        val orientation: Int = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_270 -> 270F
            ExifInterface.ORIENTATION_ROTATE_180 -> 180F
            ExifInterface.ORIENTATION_ROTATE_90 -> 90F
            else -> 0F
        }
    }

    fun rotateImage(bitmap: Bitmap, degree: Float): Bitmap {
        return if (degree != 0F) {
            val matrix = Matrix()
            matrix.postRotate(degree)
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else bitmap
    }

    fun drawBoundingBox(bitmap: Bitmap, text: Text, boundingBoxType: BoundingBoxType): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
            strokeJoin = Paint.Join.ROUND
            style = Paint.Style.STROKE
            color = Color.parseColor("#C6FF00")
            strokeWidth = bitmap.width / 400F
        }

        val tempBitmap: Bitmap = bitmap.copy(bitmap.config, true)
        val canvas = Canvas(tempBitmap)
        canvas.drawBitmap(bitmap, 0F, 0F, null)
        when (boundingBoxType) {
            BoundingBoxType.TextBlock -> {
                text.textBlocks.forEach { block ->
                    val rect = RectF(block.boundingBox)
                    canvas.drawRect(rect, paint)
                }
            }
            BoundingBoxType.Line -> {
                text.textBlocks.forEach { block ->
                    block.lines.forEach { line ->
                        val rect = RectF(line.boundingBox)
                        canvas.drawRect(rect, paint)
                    }
                }
            }
            BoundingBoxType.Word -> {
                text.textBlocks.forEach { block ->
                    block.lines.forEach { line ->
                        line.elements.forEach { word ->
                            val rect = RectF(word.boundingBox)
                            canvas.drawRect(rect, paint)
                        }
                    }
                }
            }
        }
        return tempBitmap
    }

    fun getCropBitmap(rect: Rect, bitmap: Bitmap): Bitmap? {
        if (rect.left >= rect.right || rect.top >= rect.bottom) return null
        rect.left = if (rect.left < 0) 0 else rect.left
        rect.top = if (rect.top < 0) 0 else rect.top
        rect.right = if (rect.right > bitmap.width) bitmap.width else rect.right
        rect.bottom = if (rect.bottom > bitmap.height) bitmap.height else rect.bottom
        return Bitmap.createBitmap(
            bitmap,
            rect.left,
            rect.top,
            rect.right - rect.left,
            rect.bottom - rect.top
        )
    }

    fun cropAllTextOnBitmap(
        bitmap: Bitmap,
        text: Text,
        boundingBoxType: BoundingBoxType
    ): List<TextOnBitmap> {
        val list = arrayListOf<TextOnBitmap>()
        when (boundingBoxType) {
            BoundingBoxType.TextBlock -> {
                text.textBlocks.forEach { block ->
                    block.boundingBox?.let {
                        getCropBitmap(it, bitmap)?.let { croppedBitmap ->
                            list.add(TextOnBitmap(block.text, croppedBitmap))
                        }
                    }
                }
            }
            BoundingBoxType.Line -> {
                text.textBlocks.forEach { block ->
                    block.lines.forEach { line ->
                        line.boundingBox?.let {
                            getCropBitmap(it, bitmap)?.let { croppedBitmap ->
                                list.add(TextOnBitmap(line.text, croppedBitmap))
                            }
                        }
                    }
                }
            }
            BoundingBoxType.Word -> {
                text.textBlocks.forEach { block ->
                    block.lines.forEach { line ->
                        line.elements.forEach { word ->
                            word.boundingBox?.let {
                                getCropBitmap(it, bitmap)?.let { croppedBitmap ->
                                    list.add(TextOnBitmap(word.text, croppedBitmap))
                                }
                            }
                        }
                    }
                }
            }
        }
        return list
    }

    fun captureView(view: View): Bitmap {
        val b = if (view.measuredHeight <= 0) {
            val specWidth = MeasureSpec.makeMeasureSpec(0 /* any */, MeasureSpec.UNSPECIFIED)
            view.measure(specWidth, specWidth)
            Bitmap.createBitmap(
                view.measuredWidth,
                view.measuredHeight,
                Bitmap.Config.ARGB_8888
            )
        } else {
            Bitmap.createBitmap(
                view.width,
                view.height,
                Bitmap.Config.ARGB_8888
            )
        }
        val c = Canvas(b)
        view.layout(view.left, view.top, view.right, view.bottom)
        view.draw(c)
        return b
    }

    fun loadBitmapEfficientlyToImageView(
        bitmap: Bitmap,
        imageView: ImageView,
        context: Context,
        destinationWidth: Int
    ) {
        val ratio: Float = destinationWidth * 1.0F / bitmap.width

        val oldDrawable = if (imageView.drawable == null) {
            ContextCompat.getDrawable(context, R.drawable.no_image)
        } else imageView.drawable

        Glide.with(context)
            .asBitmap()
            .load(bitmap)
            .placeholder(oldDrawable)
            .override(destinationWidth, (ratio * bitmap.height).toInt())
            .into(imageView)
    }

    fun convertImageToBitmapJPEG(image: Image): Bitmap {
        val buffer = image.planes[0].buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    fun getCropImageJPEGByRect(
        rect: Rect,
        image: Image,
        rotationDegrees: Int
    ): Bitmap? {
        var bitmap = convertImageToBitmapJPEG(image)
        bitmap = rotateImage(bitmap, rotationDegrees.toFloat())
        return getCropBitmap(rect, bitmap)
    }

    fun convertYuv420888ImageToBitmap(image: Image): Bitmap {
        require(image.format == ImageFormat.YUV_420_888) {
            "Unsupported image format $(image.format)"
        }

        val planes = image.planes

        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        val yuvBytes = planes.map { plane ->
            val buffer = plane.buffer
            val yuvBytes = ByteArray(buffer.capacity())
            buffer[yuvBytes]
            buffer.rewind()  // Be kind…
            yuvBytes
        }

        val yRowStride = planes[0].rowStride
        val uvRowStride = planes[1].rowStride
        val uvPixelStride = planes[1].pixelStride
        val width = image.width
        val height = image.height
        @ColorInt val argb8888 = IntArray(width * height)
        var i = 0
        for (y in 0 until height) {
            val pY = yRowStride * y
            val uvRowStart = uvRowStride * (y shr 1)
            for (x in 0 until width) {
                val uvOffset = (x shr 1) * uvPixelStride
                argb8888[i++] =
                    yuvToRgb(
                        yuvBytes[0][pY + x].toIntUnsigned(),
                        yuvBytes[1][uvRowStart + uvOffset].toIntUnsigned(),
                        yuvBytes[2][uvRowStart + uvOffset].toIntUnsigned()
                    )
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(argb8888, 0, width, 0, 0, width, height)
        return bitmap
    }

    fun rotateAndCrop(
        bitmap: Bitmap,
        imageRotationDegrees: Int,
        cropRect: Rect
    ): Bitmap {
        val matrix = Matrix()
        matrix.preRotate(imageRotationDegrees.toFloat())
        return Bitmap.createBitmap(
            bitmap,
            cropRect.left,
            cropRect.top,
            cropRect.width(),
            cropRect.height(),
            matrix,
            true
        )
    }

    private val CHANNEL_RANGE = 0 until (1 shl 18)
    @ColorInt
    private fun yuvToRgb(nY: Int, nU: Int, nV: Int): Int {
        var nY = nY
        var nU = nU
        var nV = nV
        nY -= 16
        nU -= 128
        nV -= 128
        nY = nY.coerceAtLeast(0)

        // This is the floating point equivalent. We do the conversion in integer
        // because some Android devices do not have floating point in hardware.
        // nR = (int)(1.164 * nY + 2.018 * nU);
        // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
        // nB = (int)(1.164 * nY + 1.596 * nV);
        var nR = 1192 * nY + 1634 * nV
        var nG = 1192 * nY - 833 * nV - 400 * nU
        var nB = 1192 * nY + 2066 * nU

        // Clamp the values before normalizing them to 8 bits.
        nR = nR.coerceIn(CHANNEL_RANGE) shr 10 and 0xff
        nG = nG.coerceIn(CHANNEL_RANGE) shr 10 and 0xff
        nB = nB.coerceIn(CHANNEL_RANGE) shr 10 and 0xff
        return -0x1000000 or (nR shl 16) or (nG shl 8) or nB
    }

    private fun Byte.toIntUnsigned(): Int {
        return toInt() and 0xFF
    }
}