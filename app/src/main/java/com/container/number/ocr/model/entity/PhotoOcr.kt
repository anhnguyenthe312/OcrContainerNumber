package com.container.number.ocr.model.entity

import android.graphics.Rect
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.container.number.ocr.model.type.Evaluate
import com.container.number.ocr.model.type.OcrAlgorithm

@Entity(tableName = "PhotoOcr")
class PhotoOcr {
    @PrimaryKey
    var uriStr: String = ""

    @ColumnInfo
    var evaluate: Evaluate = Evaluate.NOT_READ

    @ColumnInfo
    var containerNumber: String = ""

    @ColumnInfo
    var boundingRect: Rect = Rect()

    @ColumnInfo
    var algorithm: OcrAlgorithm = OcrAlgorithm.ONE_LINE
}