package com.dct.qr.data.model // Alebo iný vhodný balíček

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "generated_qr_codes")
data class GeneratedQrCodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String, // Text, z ktorého bol QR kód vygenerovaný
    val imagePath: String?, // Cesta k uloženému obrázku v internom úložisku alebo null, ak nie je uložený externe
    val timestamp: Long = System.currentTimeMillis(),
    var isFavorite: Boolean = false,
    val note: String? = null // Voliteľná poznámka k QR kódu
)