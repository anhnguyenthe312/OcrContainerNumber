package com.container.number.ocr.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.container.number.ocr.db.dao.PhotoOcrDao
import com.container.number.ocr.model.data.RectConverter
import com.container.number.ocr.model.entity.PhotoOcr
import com.container.number.ocr.model.type.OcrAlgorithm
import com.container.number.ocr.model.type.Evaluate

@Database(
    version = 2,
    exportSchema = false,
    entities = [
        PhotoOcr::class,
    ]
)
@TypeConverters(
    Evaluate.Converters::class,
    OcrAlgorithm.Converters::class,
    RectConverter::class,
)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        private var appDatabase: AppDatabase? = null
        fun buildDatabase(context: Context): AppDatabase {
            synchronized(AppDatabase::class){
                if (appDatabase == null) {
                    appDatabase = Room.databaseBuilder(
                        context,
                        AppDatabase::class.java,
                        "local.db"
                    )
                        .addMigrations(from1To2)
                        .build()
                }
            }
            return appDatabase!!
        }
        private val from1To2 = object : Migration(1, 2){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `PhotoOcr` ADD COLUMN `logs` TEXT DEFAULT ''")
            }

        }
    }



    //Dao
    abstract fun photoOcrDao(): PhotoOcrDao

}