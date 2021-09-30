package com.container.number.ocr.utils

import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import com.container.number.ocr.extension.isNumber
import com.container.number.ocr.extension.logcat
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.max
import kotlin.math.pow

object ContainerNumberUtils {

    const val CONTAINER_NUMBER_LENGTH = 11
    private const val OWNER_LENGTH = 3
    private const val OWNER_CATEGORY_LENGTH = 4
    private const val SERIAL_NUMBER_LENGTH = 6
    private const val CHECK_DIGIT_LENGTH = 1
    private const val CATEGORY_POSITION = 3
    private val ISO_CATEGORIES = arrayOf('U', 'J', 'Z')

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
            (if (containerNumber.length == CONTAINER_NUMBER_LENGTH) containerNumber.substring(
                containerNumber.length - 1
            ) else containerNumber)
        when {
            containerNumberToCheck.length != CONTAINER_NUMBER_LENGTH - CHECK_DIGIT_LENGTH -> return -1
            containerNumberToCheck[CATEGORY_POSITION] !in ISO_CATEGORIES -> return -1
            containerNumberToCheck.substring(0, CATEGORY_POSITION - 1)
                .any { it.isLowerCase() } -> return -1
            containerNumberToCheck.substring(CATEGORY_POSITION + 1)
                .any { !it.isDigit() } -> return -1
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
        val lastChar =
            fullContainerNumber.substring(fullContainerNumber.length - CHECK_DIGIT_LENGTH)
        return if (lastChar.isNumber()) {
            val checkDigitToVerify = lastChar.toInt()
            // Get rid of the last number, the last number is the check digit where we want to generated in order to validate.
            val containerNumberToCheck =
                fullContainerNumber.substring(0, fullContainerNumber.length - CHECK_DIGIT_LENGTH)
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
        return if (checkDigit.length == CHECK_DIGIT_LENGTH && checkDigit.isNumber()) {
            val checkDigitToVerify = checkDigit.toInt()

            val containerNumberToCheck = "$ownerPrefix$equipmentIdentifier$serialNumber"
            return checkDigitToVerify == getCheckDigit(containerNumberToCheck)
        } else false
    }

    fun verifyDigit(firstWord: String, secondWord: String, thirdWord: String): Boolean {
        return if (firstWord.length == OWNER_CATEGORY_LENGTH && secondWord.length == SERIAL_NUMBER_LENGTH && thirdWord.length == CHECK_DIGIT_LENGTH) {
            verifyDigit(
                firstWord.substring(0, CATEGORY_POSITION),
                firstWord.substring(CATEGORY_POSITION),
                secondWord,
                thirdWord
            )
        } else false
    }

    fun verifyDigit(firstWord: String, secondWord: String): Pair<Boolean, Int> {
        return if (firstWord.length == OWNER_CATEGORY_LENGTH && secondWord.length == SERIAL_NUMBER_LENGTH && secondWord.isNumber()) {
            val containerNumberToCheck = "$firstWord$secondWord"
            val checkDigit = getCheckDigit(containerNumberToCheck)
            Pair(checkDigit in 0..9, checkDigit)
        } else Pair(false, -1)
    }

    fun verifyDigitWithoutDigit(ContainerNumberWithoutDigit: String): Pair<Boolean, Int> {
        return if (ContainerNumberWithoutDigit.length == CONTAINER_NUMBER_LENGTH - CHECK_DIGIT_LENGTH) {
            val firstWord = ContainerNumberWithoutDigit.substring(0, CATEGORY_POSITION + 1)
            val secondWord = ContainerNumberWithoutDigit.substring(
                CATEGORY_POSITION + 1,
                CONTAINER_NUMBER_LENGTH - 1
            )
            verifyDigit(firstWord, secondWord)
        } else Pair(false, -1)
    }

    private fun isValidOwnerCategory(text: String): Boolean {
        return text.length == OWNER_CATEGORY_LENGTH && text[CATEGORY_POSITION] in ISO_CATEGORIES
    }

    fun getContainerNumberVertical(visionText: Text, type: BoundRectType): OcrResult {
        var containerNumber = ""
        var rect = Rect()
        run breaker@{

        }
        return OcrResult(containerNumber, rect, visionText.text)
    }

    fun getContainerNumber2Line(visionText: Text, type: BoundRectType): OcrResult {
        var containerNumber = ""
        var rect = Rect()
        run breaker@{
            logcat("====================================================")

            // Build List of Candidate list
            // With each item is a list that have first item isValidBICCode
            val blockCandidateList = arrayListOf<List<Text.Element>>()
            visionText.textBlocks.forEachIndexed textBlock@{ index, textBlock ->
                logcat("---------------------------------------------")
                logcat(textBlock.text)
                if (textBlock.lines.size > 0
                    && isValidOwnerCategory(textBlock.lines[0].text.trim().toUpperCase())
                ) {
                    val listWord = arrayListOf<Text.Element>()
                    var checkLength = 0
                    textBlock.lines.forEach { line ->
                        listWord.addAll(line.elements)
                        checkLength += line.elements.sumBy { ele -> ele.text.trim().length }
                    }

                    // not enough length then check nearby TextBlock to add more word
                    if (checkLength < CONTAINER_NUMBER_LENGTH - 1) {
                        for (i in index + 1 until visionText.textBlocks.size) {
                            val block = visionText.textBlocks[i]
                            block.lines.forEach { line ->
                                listWord.addAll(line.elements)
                                checkLength += line.elements.sumBy { ele -> ele.text.trim().length }
                            }
                            if (checkLength >= CONTAINER_NUMBER_LENGTH - 1) {
                                blockCandidateList.add(listWord)
                                break
                            }
                        }
                    } else blockCandidateList.add(listWord)
                }
            }

            //Verify that item have container number chain in this
            blockCandidateList.forEach { listWord ->
                var listCandidate = getCandidateWordListByLength(listWord, CONTAINER_NUMBER_LENGTH)
                if (listCandidate.isNotEmpty()) {
                    val lineStr =
                        listCandidate.joinToString(separator = "") { word -> word.text.trim() }
                            .toUpperCase()
                    logcat("Candidate: $lineStr")
                    if (verifyDigit(lineStr)) {
                        containerNumber = lineStr
                        rect = getBound(
                            type,
                            listCandidate.map { it.boundingBox }
                        )
                        return@breaker
                    }
                }

                // there is a case that last digit number cannot recognize because of rectangle over it
                listCandidate = getCandidateWordListByLength(
                    listWord,
                    CONTAINER_NUMBER_LENGTH - CHECK_DIGIT_LENGTH
                )
                if (listCandidate.isNotEmpty()) {
                    val lineStr =
                        listCandidate.joinToString(separator = "") { word -> word.text.trim() }
                            .toUpperCase()
                    logcat("Candidate: $lineStr + ?")
                    val pair = verifyDigitWithoutDigit(lineStr)
                    if (pair.first) {
                        containerNumber = lineStr + pair.second
                        rect = getBound(
                            type,
                            listCandidate.map { it.boundingBox }
                        )
                        return@breaker
                    }
                }
            }
        }
        return OcrResult(containerNumber, rect, visionText.text)
    }

    fun getContainerNumber(visionText: Text, type: BoundRectType): OcrResult {
        var containerNumber = ""
        var rect = Rect()
        logcat("==================================================")

        run breaker@{
            val blockCandidateList = arrayListOf<List<Text.Element>>()
            visionText.textBlocks.forEachIndexed block@{ index, textBlock ->
                logcat("----------------------------------------------")
                logcat(textBlock.text)
                textBlock.lines.forEach { line ->
                    // check if line is a container number
                    var lineStr = line.text.trim().toUpperCase()
                    if (verifyDigit(lineStr)) {
                        containerNumber = lineStr
                        rect = getBound(
                            type,
                            listOf(
                                line.boundingBox,
                                null,
                                null
                            )
                        )
                        return@breaker
                    }

                    var listCandidate = getCandidateByLength(line.elements, CONTAINER_NUMBER_LENGTH)
                    listCandidate.forEach { itemList ->
                        lineStr =
                            itemList.joinToString(separator = "") { word -> word.text.trim() }
                                .toUpperCase()

                        logcat("Candidate: $lineStr")
                        if (verifyDigit(lineStr)) {
                            containerNumber = lineStr
                            rect = getBound(
                                type,
                                itemList.map { it.boundingBox }
                            )
                            return@breaker
                        }
                    }

                    listCandidate = getCandidateByLength(
                        line.elements,
                        CONTAINER_NUMBER_LENGTH - CHECK_DIGIT_LENGTH
                    )
                    listCandidate.forEach { itemList ->
                        lineStr =
                            itemList.joinToString(separator = "") { word -> word.text.trim() }
                                .toUpperCase()

                        logcat("Candidate: $lineStr ?")
                        val pair = verifyDigitWithoutDigit(lineStr)
                        if (pair.first) {
                            containerNumber = lineStr + pair.second
                            rect = getBound(
                                type,
                                itemList.map { it.boundingBox }
                            )
                            return@breaker
                        }
                    }
                }

                // Build List of Candidate list
                // With each item is a list that have first item isValidBICCode
                if (textBlock.lines.size > 0
                    && isValidOwnerCategory(
                        textBlock.lines[0].elements[0].text.trim().toUpperCase()
                    )
                ) {
                    val validBICCodeElement = textBlock.lines[0].elements[0]

                    // check nearby TextBlock to add more word
                    // nearby textBlock is +- 2 item in list
                    val nearbyTextBlock = arrayListOf<Text.TextBlock>()
                    for (i in (-2..2)) {
                        if (i == 0) continue
                        if (index + i > -1 && index + i < visionText.textBlocks.size) {
                            nearbyTextBlock.add(visionText.textBlocks[index + i])
                        }
                    }
                    nearbyTextBlock.forEach { block ->
                        block.lines.forEach { line ->
                            if (line.text.length >= SERIAL_NUMBER_LENGTH) {
                                val listWord = arrayListOf<Text.Element>()
                                listWord.add(validBICCodeElement)
                                listWord.addAll(line.elements)
                                blockCandidateList.add(listWord)
                            }
                        }
                    }
                }
            }

            //Verify that item have container number chain in this
            blockCandidateList.forEach { listWord ->
                var listCandidate = getCandidateWordListByLength(listWord, CONTAINER_NUMBER_LENGTH)
                if (listCandidate.isNotEmpty()) {
                    val lineStr =
                        listCandidate.joinToString(separator = "") { word -> word.text.trim() }
                            .toUpperCase()
                    logcat("Candidate: $lineStr")
                    if (verifyDigit(lineStr)) {
                        containerNumber = lineStr
                        rect = getBound(
                            type,
                            listCandidate.map { it.boundingBox }
                        )
                        return@breaker
                    }
                }

                // there is a case that last digit number cannot recognize because of rectangle over it
                listCandidate = getCandidateWordListByLength(
                    listWord,
                    CONTAINER_NUMBER_LENGTH - CHECK_DIGIT_LENGTH
                )
                if (listCandidate.isNotEmpty()) {
                    val lineStr =
                        listCandidate.joinToString(separator = "") { word -> word.text.trim() }
                            .toUpperCase()
                    logcat("Candidate: $lineStr + ?")
                    val pair = verifyDigitWithoutDigit(lineStr)
                    if (pair.first) {
                        containerNumber = lineStr + pair.second
                        rect = getBound(
                            type,
                            listCandidate.map { it.boundingBox }
                        )
                        return@breaker
                    }
                }
            }
        }
        return OcrResult(containerNumber, rect, visionText.text)
    }

    private fun getCandidateWordListByLength(
        list: List<Text.Element>,
        candidateLength: Int
    ): List<Text.Element> {
        if (candidateLength <= 0) return emptyList()
        if (list.isEmpty()) return emptyList()
        val element = list[0]
        val elementText = element.text.trim()
        return when {
            elementText.length > candidateLength -> emptyList()
            elementText.length < candidateLength -> {
                if (list.size > 1) {
                    val arr = getCandidateWordListByLength(
                        list.subList(1, list.size),
                        candidateLength - elementText.length
                    )
                    if (arr.isNotEmpty()) {
                        arrayListOf<Text.Element>().apply {
                            add(element)
                            addAll(arr)
                        }
                    } else emptyList()
                } else emptyList()
            }

            else -> arrayListOf(element)
        }
    }

    private fun getCandidateByLength(
        listWord: List<Text.Element>,
        candidateLength: Int
    ): List<List<Text.Element>> {
        val arr = arrayListOf<List<Text.Element>>()
        for (i in listWord.indices) {
            val candidateList = getCandidateWordListByLength(
                listWord.subList(i, listWord.size),
                candidateLength
            )
            if (candidateList.isNotEmpty()) arr.add(candidateList)
        }
        return arr
    }

    private fun getBound(boundRectType: BoundRectType, rectList: List<Rect?>): Rect {
        val listOfRect: List<Rect> = rectList.filterNotNull()
        var minLeft = if (listOfRect.isEmpty()) 0 else listOfRect.minOf { it.left }
        var minTop = if (listOfRect.isEmpty()) 0 else listOfRect.minOf { it.top }
        var maxRight = if (listOfRect.isEmpty()) 0 else listOfRect.maxOf { it.right }
        var maxBottom = if (listOfRect.isEmpty()) 0 else listOfRect.maxOf { it.bottom }
        val width = maxRight - minLeft
        val height = maxBottom - minTop

        when (boundRectType) {
            BoundRectType.SQUARE -> {
                var size = max(width, height)
                size = (1.3f * size).toInt()
                minLeft = minLeft + width / 2 - size / 2
                minTop = minTop + height / 2 - size / 2
                maxRight = minLeft + if (listOfRect.size == 2) (1.2f * size).toInt() else size
                maxBottom = minTop + if (listOfRect.size == 2) (1.2f * size).toInt() else size
            }
            BoundRectType.CORRECT_BOUND -> {
                minLeft -= width / 20
                minTop -= height / 20
                maxRight += width / 20
                maxBottom += height / 20
            }
        }

        return Rect(minLeft, minTop, maxRight, maxBottom)
    }


    enum class BoundRectType {
        SQUARE,
        CORRECT_BOUND
    }

    data class OcrResult(
        var containerNumber: String,
        var rect: Rect,
        var logs: String
    )
}