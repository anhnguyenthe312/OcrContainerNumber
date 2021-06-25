package com.container.number.ocr.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.container.number.ocr.model.entity.PhotoOcr
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoOcrDao: BaseDao<PhotoOcr> {

    @Query("SELECT * FROM PhotoOcr WHERE uriStr = :uriStr")
    fun getByUriStr(uriStr: String): PhotoOcr?

    @Query("SELECT * FROM PhotoOcr WHERE uriStr LIKE :folderUriStr || '%'")
    fun getAllPhotoOcrInFolder(folderUriStr: String): Flow<List<PhotoOcr>>

    @Query("SELECT * FROM PhotoOcr")
    fun getAllPhotoOcr(): Flow<List<PhotoOcr>>
}