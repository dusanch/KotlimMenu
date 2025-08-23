package com.dct.qr.data.db // Alebo váš preferovaný balíček pre dáta

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_codes")
data class ScannedCode(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val content: String, // Obsah QR kódu
    val type: Int,       // Typ čiarového kódu (ako ho poskytuje ML Kit Barcode)
    val timestamp: Long = System.currentTimeMillis() // Časová pečiatka skenovania
)
