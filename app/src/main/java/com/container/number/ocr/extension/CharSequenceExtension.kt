package com.container.number.ocr.extension

import android.util.Patterns
import java.util.regex.Pattern

val GPS_PATTERN = Pattern.compile("^(-?\\d+(\\.\\d+)?),\\s*(-?\\d+(\\.\\d+)?)\$")
val NUMBER_PATTERN = Pattern.compile("^(\\d+\\.\\d+|\\d+)$")
fun CharSequence?.isValidEmail() =
    !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun CharSequence?.isValidGps() = !isNullOrEmpty() && GPS_PATTERN.matcher(this).matches()
fun CharSequence?.isNumber() = !isNullOrEmpty() && NUMBER_PATTERN.matcher(this).matches()
