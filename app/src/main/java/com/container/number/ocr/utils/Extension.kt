package com.container.number.ocr.utils
fun String.isLetterOnly(): Boolean {
    return this.matches("^[a-zA-Z]*$".toRegex())
}