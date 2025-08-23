package com.dct.qr.data.db

import android.content.Context
// import androidx.privacysandbox.tools.core.generator.build
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dct.qr.data.db.dao.GeneratedQrCodeDao
import com.dct.qr.data.model.GeneratedQrCodeEntity

@Database(
    entities = [
        ScannedCode::class,
        GeneratedQrCodeEntity::class
    ], version = 3, exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scannedCodeDao(): ScannedCodeDao
    abstract fun generatedQrCodeDao(): GeneratedQrCodeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "qr_scanner_database"
                )
                    // Tu môžete pridať migrácie, ak budete meniť schému v budúcnosti
                    // .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
