package com.dct.qr.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.dct.qr.data.db.dao.GeneratedQrCodeDao
import com.dct.qr.data.model.GeneratedQrCodeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File

open class GeneratedQrRepository(
    private val generatedQrCodeDao: GeneratedQrCodeDao,
    private val context: Context
) {

    /**
     * Získa vygenerovaný QR kód podľa jeho obsahu.
     * Vráti entitu alebo null, ak neexistuje.
     */
    open suspend fun getByContent(content: String): GeneratedQrCodeEntity? {
        return withContext(Dispatchers.IO) {
            generatedQrCodeDao.getByContent(content)
        }
    }

    /**
     * Uloží NOVÝ QR kód do databázy.
     * Vracia ID novovloženej entity.
     */
    suspend fun insertNewGeneratedQr(qrCode: GeneratedQrCodeEntity): Long {
        return withContext(Dispatchers.IO) {
            generatedQrCodeDao.insert(qrCode)
        }
    }

    /**
     * Aktualizuje existujúci QR kód v databáze.
     */
    open suspend fun updateGeneratedQr(qrCode: GeneratedQrCodeEntity) {
        withContext(Dispatchers.IO) {
            generatedQrCodeDao.update(qrCode) // Predpokladá, že DAO má @Update metódu
        }
    }

    /**
     * Uloží informácie o vygenerovanom QR kóde.
     * Ak už záznam s daným obsahom existuje, aktualizuje ho.
     * Ak neexistuje, vytvorí nový záznam.
     * Táto metóda je komplexnejšia. Pre jednoduchý update existujúcej entity použite updateGeneratedQr.
     */
    suspend fun saveOrUpdateGeneratedQrComplex(
        content: String,
        imagePath: String?,
        isFavorite: Boolean
    ): Result<GeneratedQrCodeEntity> {
        return try {
            val existingCode = getByContent(content)
            if (existingCode != null) {
                val updatedCode = existingCode.copy(
                    isFavorite = isFavorite,
                    imagePath = imagePath ?: existingCode.imagePath,
                    timestamp = System.currentTimeMillis() // Vždy aktualizuj timestamp
                )
                updateGeneratedQr(updatedCode) // Použijeme našu novú update metódu
                Result.success(updatedCode)
            } else {
                val newQrCode = GeneratedQrCodeEntity(
                    content = content,
                    imagePath = imagePath,
                    isFavorite = isFavorite,
                    timestamp = System.currentTimeMillis()
                )
                val id = insertNewGeneratedQr(newQrCode) // Použijeme našu novú insert metódu
                Result.success(newQrCode.copy(id = id.toInt()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aktualizuje stav 'isFavorite' a timestamp pre existujúci QR kód podľa jeho ID.
     */
    suspend fun updateFavoriteStatus(qrCodeId: Int, newFavoriteState: Boolean): Result<GeneratedQrCodeEntity?> {
        return try {
            val qrCode : GeneratedQrCodeEntity? = generatedQrCodeDao.getById(qrCodeId).firstOrNull()

            qrCode?.let {
                if (it.isFavorite != newFavoriteState) {
                    val updatedCode = it.copy(
                        isFavorite = newFavoriteState,
                        timestamp = System.currentTimeMillis() // Vždy aktualizuj timestamp
                    )
                    updateGeneratedQr(updatedCode) // Použijeme našu novú update metódu
                    Result.success(updatedCode)
                } else {
                    // Ak sa stav 'isFavorite' nezmenil, stále aktualizujme timestamp, ak je to žiaduce
                    // alebo len vráťme existujúci kód. Tu aktualizujeme timestamp aj tak.
                    val timestampUpdatedCode = it.copy(timestamp = System.currentTimeMillis())
                    updateGeneratedQr(timestampUpdatedCode)
                    Result.success(timestampUpdatedCode)
                }
            } ?: Result.failure(Exception("QR kód s ID $qrCodeId nebol nájdený."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uloží bitmapu do galérie zariadenia.
     */
    suspend fun saveBitmapToGallery(bitmap: Bitmap, displayName: String): Result<Uri?> = withContext(Dispatchers.IO) {
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "MojeQRkody")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            // Pre staršie API sa súbor uloží priamo do Pictures.
            // Ak adresár "MojeQRkody" neexistuje pre staršie API, mohol by sa vytvoriť manuálne,
            // ale MediaStore API to priamo nerieši tak jednoducho ako RELATIVE_PATH.
        }

        val resolver = context.contentResolver
        var uri: Uri? = null

        try {
            uri = resolver.insert(imageCollection, contentValues)
            uri?.let { imageUri ->
                resolver.openOutputStream(imageUri)?.use { outputStream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                        throw Exception("Nepodarilo sa skomprimovať a uložiť bitmapu.")
                    }
                } ?: throw Exception("Nepodarilo sa otvoriť OutputStream pre URI.")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }
                Result.success(imageUri)
            } ?: throw Exception("Nepodarilo sa vytvoriť MediaStore URI pre obrázok.")
        } catch (e: Exception) {
            uri?.let { resolver.delete(it, null, null) }
            Result.failure(e)
        }
    }

    /**
     * Získa všetky vygenerované QR kódy ako Flow pre pozorovanie v UI.
     */
    fun getAllGeneratedQrsFlow(): Flow<List<GeneratedQrCodeEntity>> {
        return generatedQrCodeDao.getAll()
    }

    /**
     * Získa všetky obľúbené vygenerované QR kódy ako Flow pre pozorovanie v UI.
     */
    fun getFavoriteGeneratedQrsFlow(): Flow<List<GeneratedQrCodeEntity>> {
        return generatedQrCodeDao.getFavorites()
    }

    /**
     * Získa QR kód podľa jeho ID ako Flow pre pozorovanie v UI.
     */
    fun getByIdFlow(id: Int): Flow<GeneratedQrCodeEntity?> {
        return generatedQrCodeDao.getById(id)
    }
}

