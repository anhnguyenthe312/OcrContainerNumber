package com.container.number.ocr.utils

import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import com.container.number.ocr.extension.isNumber
import kotlin.math.max
import kotlin.math.pow

object ContainerNumberUtils {

    const val CONTAINER_NUMBER_LENGTH = 11

    // XXXUDDDDDDDC, where XXX - owner code; U - category identifier; DDDDDD - six digits serial number, C - check digit
    //
    // Owner code only allow character
    //
    // Category identifier only allow:
    // U for all freight containers
    // J for detachable freight container-related equipment
    // Z for trailers and chassis
    //
    // Serial number: only allow number

    fun getCheckDigit(containerNumber: String): Int {
        val containerNumberToCheck =
            if (containerNumber.length == CONTAINER_NUMBER_LENGTH) containerNumber.substring(
                containerNumber.length - 1
            ) else containerNumber
        when {
            containerNumberToCheck.length != CONTAINER_NUMBER_LENGTH - 1 -> return -1
            containerNumberToCheck[3] !in arrayOf('U', 'I', 'Z') -> return -1
            containerNumberToCheck.substring(0, 2).any { it.isLowerCase() } -> return -1
            containerNumberToCheck.substring(4).any { !it.isDigit() } -> return -1
        }

        val equivalentNumericalValues: MutableMap<Char, Int> = HashMap()
        var value = 0
        for (i in '0'..'9') {
            equivalentNumericalValues[i] = value++
        }
        for (i in 'A'..'Z') {
            if (value % 11 == 0) value++
            equivalentNumericalValues[i] = value++
        }
        // Step 1,2 and 3
        var sum = 0.0
        for (i in containerNumberToCheck.indices) {
            sum += equivalentNumericalValues[containerNumberToCheck[i]]!! * 2.0.pow(i)
        }
        var sumInteger = sum.toInt()
        // Step 4
        sumInteger /= 11
        // Step 5
        sumInteger *= 11
        // Step 6
        return (sum.toInt() - sumInteger) % 10
    }

    fun verifyDigit(fullContainerNumber: String): Boolean {
        // Get the last number of the container number which is the check digit.
        val lastChar = fullContainerNumber.substring(fullContainerNumber.length - 1)
        return if (lastChar.isNumber()) {
            val checkDigitToVerify = lastChar.toInt()
            // Get rid of the last number, the last number is the check digit where we want to generated in order to validate.
            val containerNumberToCheck =
                fullContainerNumber.substring(0, fullContainerNumber.length - 1)
            checkDigitToVerify == getCheckDigit(containerNumberToCheck)
        } else false
    }

    fun verifyDigit(
        ownerPrefix: String,
        equipmentIdentifier: String,
        serialNumber: String,
        checkDigit: String
    ): Boolean {
        // checkDigit is valid
        return if (checkDigit.length == 1 && checkDigit.isNumber()) {
            val checkDigitToVerify = checkDigit.toInt()

            val containerNumberToCheck = "$ownerPrefix$equipmentIdentifier$serialNumber"
            return checkDigitToVerify == getCheckDigit(containerNumberToCheck)
        } else false
    }

    fun verifyDigit(firstWord: String, secondWord: String, thirdWord: String): Boolean {
        return if (firstWord.length == 4 && secondWord.length == 6 && thirdWord.length == 1) {
            verifyDigit(firstWord.substring(0, 3), firstWord.substring(3), secondWord, thirdWord)
        } else false
    }

    fun verifyDigit(firstWord: String, secondWord: String): Pair<Boolean, Int> {
        return if (firstWord.length == 4 && secondWord.length == 6 && secondWord.isNumber()) {
            val containerNumberToCheck = "$firstWord$secondWord"
            val checkDigit = getCheckDigit(containerNumberToCheck)
            Pair(checkDigit in 0..9, checkDigit)
        } else Pair(false, -1)
    }

    fun getContainerNumber(visionText: Text): Pair<String, Rect> {
        var containerNumber = ""
        var rect = Rect()
        run breaker@{
            visionText.textBlocks.forEach block@{ block ->
                block.lines.forEach { line ->
                    // check if line is a container number
                    if (verifyDigit(line.text.trim())) {
                        containerNumber = line.text
                        rect = getBoundRect(
                            listOf(
                                line.boundingBox,
                                null,
                                null
                            )
                        )
                        return@breaker
                    }

                    //check if three word in a row is a container number
                    if (line.elements.size >= 3) {
                        for (i in 0 until line.elements.size - 2) {
                            if (verifyDigit(
                                    line.elements[i].text.trim(),
                                    line.elements[i + 1].text.trim(),
                                    line.elements[i + 2].text.trim()
                                )
                            ) {
                                containerNumber = line.elements[i].text.trim() +
                                        line.elements[i + 1].text.trim() +
                                        line.elements[i + 2].text.trim()
                                rect = getBoundRect(
                                    listOf(
                                        line.elements[i].boundingBox,
                                        line.elements[i + 1].boundingBox,
                                        line.elements[i + 2].boundingBox
                                    )
                                )
                                return@breaker
                            }
                        }
                    }

                    //there is a case that last digit number cannot recognize because of rectangle over it, check only 2 word
                    if (line.elements.size >= 2) {
                        for (i in 0 until line.elements.size - 1) {
                            val pair = verifyDigit(
                                line.elements[i].text.trim(),
                                line.elements[i + 1].text.trim()
                            )
                            if (pair.first) {
                                containerNumber = line.elements[i].text.trim() +
                                        line.elements[i + 1].text.trim() +
                                        pair.second
                                rect = getBoundRect(
                                    listOf(
                                        line.elements[i].boundingBox,
                                        line.elements[i + 1].boundingBox,
                                        null
                                    )
                                )
                                return@breaker
                            }
                        }
                    }
                }
            }
        }
        return Pair(containerNumber, rect)
    }

    private fun getBoundRect(rectList: List<Rect?>): Rect {
        val listOfRect: List<Rect> = rectList.filterNotNull()
        var minLeft = if (listOfRect.isEmpty()) 0 else listOfRect.minOf { it.left }
        var minTop = if (listOfRect.isEmpty()) 0 else listOfRect.minOf { it.top }
        var maxRight = if (listOfRect.isEmpty()) 0 else listOfRect.maxOf { it.right }
        var maxBottom = if (listOfRect.isEmpty()) 0 else listOfRect.maxOf { it.bottom }
        val width = maxRight - minLeft
        val height = maxBottom - minTop

        minLeft -= width / 20
        minTop -= height / 20
        maxRight += width / 20
        maxBottom += height / 20
        return Rect(minLeft, minTop, maxRight, maxBottom)
    }
}