package com.container.number.ocr.model.data

import android.graphics.Rect
import androidx.room.TypeConverter

class RectConverter {
    @TypeConverter
    fun toRect(value: String) = Rect.unflattenFromString(value)

    @TypeConverter
    fun fromRect(value: Rect) = value.flattenToString()
}