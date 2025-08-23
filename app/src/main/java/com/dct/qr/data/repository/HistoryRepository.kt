package com.dct.qr.data.repository

import com.dct.qr.data.db.ScannedCode
import com.dct.qr.data.db.ScannedCodeDao
import kotlinx.coroutines.flow.Flow

open class HistoryRepository(private val scannedCodeDao: ScannedCodeDao) {

    val allScannedCodes: Flow<List<ScannedCode>> = scannedCodeDao.getAllCodes()

    suspend fun insertScannedCode(content: String, type: Int) {
        val newCode = ScannedCode(content = content, type = type)
        scannedCodeDao.insertCode(newCode)
    }

    suspend fun deleteScannedCode(scannedCode: ScannedCode) {
        scannedCodeDao.deleteCode(scannedCode)
    }

    suspend fun clearHistory() {
        scannedCodeDao.clearAllCodes()
    }
}