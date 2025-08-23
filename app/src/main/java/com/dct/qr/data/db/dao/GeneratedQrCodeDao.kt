package com.dct.qr.data.db.dao

import androidx.room.*
import com.dct.qr.data.model.GeneratedQrCodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeneratedQrCodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(qrCode: GeneratedQrCodeEntity): Long // Vráti ID vloženého záznamu

    @Update
    suspend fun update(qrCode: GeneratedQrCodeEntity)

    @Delete
    suspend fun delete(qrCode: GeneratedQrCodeEntity)

    @Query("SELECT * FROM generated_qr_codes WHERE id = :id")
    fun getById(id: Int): Flow<GeneratedQrCodeEntity?>

    @Query("SELECT * FROM generated_qr_codes ORDER BY timestamp DESC")
    fun getAll(): Flow<List<GeneratedQrCodeEntity>>

    @Query("SELECT * FROM generated_qr_codes WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<GeneratedQrCodeEntity>>

    // Prípadne funkcia na kontrolu, či už kód s daným obsahom existuje
    @Query("SELECT * FROM generated_qr_codes WHERE content = :content LIMIT 1")
    suspend fun getByContent(content: String): GeneratedQrCodeEntity?

}
