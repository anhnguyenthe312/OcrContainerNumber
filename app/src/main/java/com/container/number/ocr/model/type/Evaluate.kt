package com.container.number.ocr.model.type

import androidx.room.TypeConverter
import com.container.number.ocr.R

enum class Evaluate(val resId: Int) {
    NOT_READ(R.string.not_read),
    READ_OK(R.string.read_ok),
    INCORRECT(R.string.read_incorrect),
    READ_NOT_OK(R.string.read_not_ok);

    class Converters {
        @TypeConverter
        fun toEnum(value: String) = enumValueOf<Evaluate>(value)

        @TypeConverter
        fun fromEnum(value: Evaluate) = value.name
    }
}