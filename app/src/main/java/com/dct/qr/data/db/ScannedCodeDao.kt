package com.dct.qr.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@androidx.room.Dao
interface ScannedCodeDao {
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertCode(scannedCode: ScannedCode)

    @androidx.room.Query("SELECT * FROM scanned_codes ORDER BY timestamp DESC")
    fun getAllCodes(): Flow<List<ScannedCode>> // Použijeme Flow pre reaktívne UI

    @androidx.room.Query("SELECT * FROM scanned_codes WHERE id = :id")
    suspend fun getCodeById(id: Int): ScannedCode?

    @androidx.room.Delete
    suspend fun deleteCode(scannedCode: ScannedCode)

    @androidx.room.Query("DELETE FROM scanned_codes")
    suspend fun clearAllCodes()
}