package com.container.number.ocr.model.type

import androidx.room.TypeConverter
import com.container.number.ocr.R


enum class OcrAlgorithm(val stringResId: Int, val imageResId: Int) {
    OneLine(R.string.Container_number_in_one_line, R.drawable.ic_container_horizontal),
    TwoLine(R.string.Container_number_in_two_line, R.drawable.ic_container_two_lines),
    Vertical(R.string.Container_number_in_vertical, R.drawable.ic_container_vertical);

    class Converters {
        @TypeConverter
        fun toEnum(value: String) = enumValueOf<OcrAlgorithm>(value)

        @TypeConverter
        fun fromEnum(value: OcrAlgorithm) = value.name
    }
}