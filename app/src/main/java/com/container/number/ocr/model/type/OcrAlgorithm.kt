package com.container.number.ocr.model.type

import androidx.room.TypeConverter

enum class OcrAlgorithm {
    ONE_LINE,
    TWO_LINE,
    VERTICAL;

    class Converters {
        @TypeConverter
        fun toEnum(value: String) = enumValueOf<OcrAlgorithm>(value)

        @TypeConverter
        fun fromEnum(value: OcrAlgorithm) = value.name
    }
}