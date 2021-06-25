package com.container.number.ocr.model.type

import androidx.room.TypeConverter
import com.container.number.ocr.R

enum class Evaluate(val resId: Int) {
    NOT_READ(R.string.not_read),
    READ_OK(R.string.read_ok),
    READ_NOT_STABLE(R.string.read_not_stable),
    READ_NOT_GOOD(R.string.read_not_good);

    class Converters {
        @TypeConverter
        fun toEnum(value: String) = enumValueOf<Evaluate>(value)

        @TypeConverter
        fun fromEnum(value: Evaluate) = value.name
    }
}