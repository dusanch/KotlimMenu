package com.dct.qr.ui.scanimage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.ExifInterface // Pre EXIF informácie
import android.net.Uri
import android.os.Build
import android.os.Environment // Pre ukladanie debug bitmapy
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File // Pre ukladanie debug bitmapy
import java.io.FileOutputStream // Pre ukladanie debug bitmapy
import java.io.IOException



import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

data class ScanImageUiState(
    val isLoading: Boolean = false,
    val scannedQrContent: String? = null,
    val errorMessage: String? = null,
    val noQrFoundMessage: String? = null
)

open class ScanImageViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScanImageUiState())
    val uiState: StateFlow<ScanImageUiState> = _uiState.asStateFlow()

    // --- TÚTO FUNKCIU TREBA DOPLNIŤ ---
    fun setImageCopyError(message: String) {
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = message, // Zobrazí chybu kopírovania
                scannedQrContent = null, // Resetuje predchádzajúci skenovaný obsah
                noQrFoundMessage = null  // Resetuje správu o nenájdení QR
            )
        }
        Log.e("ScanImageVM", "Chyba kopírovania obrázka nastavená: $message")
    }

    // ... (začiatok ScanImageViewModel a ostatné funkcie) ...

    fun processImageForQrCode(context: Context, imageUri: Uri) {
        _uiState.update { it.copy(isLoading = true, scannedQrContent = null, errorMessage = null, noQrFoundMessage = null) }
        Log.d("ScanImageVM", "Spracovávam obrázok z URI (malo by byť lokálne): $imageUri")

        var loadedBitmapInternal: Bitmap? = null
        var argbBitmapInternal: Bitmap? = null
        var finalBitmapToProcess: Bitmap? = null // Inicializujeme na null

        try {
            // Krok 1: Načítanie Uri do Bitmap
            val imageFile = imageUri.path?.let { File(it) }
            if (imageFile != null && imageFile.exists()) {
                Log.d("ScanImageVM", "Načítavam bitmapu zo súboru: ${imageFile.absolutePath}")
                loadedBitmapInternal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(imageFile))
                } else {
                    @Suppress("DEPRECATION")
                    BitmapFactory.decodeFile(imageFile.absolutePath) // Použijeme BitmapFactory pre staršie API
                }
                Log.d("ScanImageVM", "Bitmapa načítaná z lokálneho súboru, Rozmery: ${loadedBitmapInternal?.width}x${loadedBitmapInternal?.height}, Konfigurácia: ${loadedBitmapInternal?.config}")
            } else {
                Log.e("ScanImageVM", "Súbor pre URI $imageUri neexistuje alebo cesta je null pri pokuse o načítanie bitmapy.")
                _uiState.update { it.copy(isLoading = false, errorMessage = "Chyba: Súbor obrázka sa nenašiel po skopírovaní.") }
                return
            }

            if (loadedBitmapInternal == null) {
                Log.e("ScanImageVM", "Nepodarilo sa načítať bitmapu z ${imageUri.path}")
                _uiState.update { it.copy(isLoading = false, errorMessage = "Chyba: Nepodarilo sa dekódovať obrázok.") }
                return
            }

            // Krok 2: Konverzia na ARGB_8888
            // Uistime sa, že loadedBitmapInternal nie je null
            argbBitmapInternal = loadedBitmapInternal.copy(Bitmap.Config.ARGB_8888, true)
            Log.d("ScanImageVM", "Bitmapa skonvertovaná na ARGB_8888, Rozmery: ${argbBitmapInternal.width}x${argbBitmapInternal.height}")


            // Krok 3: (VOLITEĽNÉ) Zmenšenie bitmapy
            finalBitmapToProcess = scaleBitmapDown(argbBitmapInternal, 4096, true)
            Log.d("ScanImageVM", "Bitmapa po zmenšení (alebo bez zmeny), Rozmery: ${finalBitmapToProcess.width}x${finalBitmapToProcess.height}")

            // --- ZAČIATOK VYLEPŠENÉHO UKLADANIA DEBUG BITMAPY ---
            val timestamp = System.currentTimeMillis()
            val debugFileName = "debug_mlkit_input_${timestamp}.png"
            val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            var debugFile: File? = null

            if (picturesDir == null) {
                Log.e("ScanImageVM_DebugSave", "Nepodarilo sa získať adresár getExternalFilesDir(Environment.DIRECTORY_PICTURES). Ukladanie debug bitmapy zlyhalo.")
            } else {
                if (!picturesDir.exists()) {
                    Log.d("ScanImageVM_DebugSave", "Adresár ${picturesDir.absolutePath} neexistuje, pokúšam sa vytvoriť.")
                    if (picturesDir.mkdirs()) {
                        Log.d("ScanImageVM_DebugSave", "Adresár ${picturesDir.absolutePath} úspešne vytvorený.")
                    } else {
                        Log.e("ScanImageVM_DebugSave", "Nepodarilo sa vytvoriť adresár ${picturesDir.absolutePath}.")
                    }
                }

                if (picturesDir.exists() && picturesDir.canWrite()) {
                    debugFile = File(picturesDir, debugFileName)
                    Log.d("ScanImageVM_DebugSave", "Pokus o uloženie debug bitmapy do: ${debugFile.absolutePath}")

                    var fos: FileOutputStream? = null
                    try {
                        fos = FileOutputStream(debugFile)
                        val success = finalBitmapToProcess.compress(Bitmap.CompressFormat.PNG, 99, fos)
                        fos.flush() // Explicitný flush pred zatvorením
                        Log.d("ScanImageVM_DebugSave", "Bitmap.compress(PNG, 90, fos) výsledok: $success")

                        if (success) {
                            Log.i("ScanImageVM_DebugSave", "Debug bitmapa by mala byť úspešne uložená do: ${debugFile.absolutePath}")
                        } else {
                            Log.e("ScanImageVM_DebugSave", "Bitmap.compress zlyhalo (vrátilo false) pre ${debugFile.absolutePath}")
                        }
                    } catch (e: FileNotFoundException) {
                        Log.e("ScanImageVM_DebugSave", "FileNotFoundException pri pokuse o zápis do ${debugFile.absolutePath}", e)
                    } catch (e: IOException) {
                        Log.e("ScanImageVM_DebugSave", "IOException pri zápise debug bitmapy do ${debugFile.absolutePath}", e)
                    } catch (e: Exception) {
                        Log.e("ScanImageVM_DebugSave", "Všeobecná chyba pri zápise debug bitmapy do ${debugFile.absolutePath}", e)
                    } finally {
                        try {
                            fos?.close()
                            Log.d("ScanImageVM_DebugSave", "FileOutputStream pre ${debugFile?.name} zatvorený.")
                        } catch (e: IOException) {
                            Log.e("ScanImageVM_DebugSave", "IOException pri zatváraní FileOutputStream pre ${debugFile?.name}", e)
                        }
                    }

                    // Overenie existencie a veľkosti súboru hneď po pokuse o zápis
                    if (debugFile.exists()) {
                        Log.i("ScanImageVM_DebugSave", "OVERENIE PO ZÁPISE: Súbor ${debugFile.name} EXISTUJE. Veľkosť: ${debugFile.length()} bytov.")
                        if (debugFile.length() == 0L) {
                            Log.w("ScanImageVM_DebugSave", "OVERENIE PO ZÁPISE: Súbor ${debugFile.name} je PRÁZDNY (0 bytov)!")
                        }
                    } else {
                        Log.e("ScanImageVM_DebugSave", "OVERENIE PO ZÁPISE: Súbor ${debugFile.name} NEEXISTUJE!")
                    }

                } else {
                    Log.e("ScanImageVM_DebugSave", "Adresár ${picturesDir.absolutePath} neexistuje alebo doň nie je možné zapisovať.")
                }
            }
            // --- KONIEC VYLEPŠENÉHO UKLADANIA DEBUG BITMAPY ---

            // Krok 5: Zistenie rotácie obrázka
            // Predpokladáme, že rotácia z lokálneho súboru už nie je potrebná, ak sme ju riešili pri kopírovaní,
            // alebo ak je obrázok už správne orientovaný. Ak nie, musíte ju zistiť znova z 'imageUri' (lokálneho súboru).
            // Pre jednoduchosť ju tu teraz nastavíme na 0, ale mali by ste to overiť.
            val rotationDegrees = getRotationDegreesFromUri(context, imageUri) // Alebo ak je to už zbytočné, tak 0
            Log.d("ScanImageVM", "Zistená rotácia (z lokálneho súboru, ak relevantné): $rotationDegrees stupňov")


            // Krok 6: Vytvorenie InputImage a spracovanie
            val inputImage = InputImage.fromBitmap(finalBitmapToProcess, rotationDegrees)

            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            val scanner = BarcodeScanning.getClient(options)

            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val firstBarcode = barcodes.first()
                        val qrContent = firstBarcode.rawValue
                        _uiState.update { it.copy(isLoading = false, scannedQrContent = qrContent) }
                        Log.d("ScanImageVM", "Nájdený QR kód (z Bitmap): $qrContent")
                    } else {
                        _uiState.update { it.copy(isLoading = false, noQrFoundMessage = "Na obrázku nebol nájdený žiadny QR kód (skúšané z Bitmap).") }
                        Log.d("ScanImageVM", "Na obrázku nebol nájdený žiadny QR kód (skúšané z Bitmap).")
                    }
                }
                .addOnFailureListener { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Chyba pri skenovaní obrázku (z Bitmap): ${e.localizedMessage}") }
                    Log.e("ScanImageVM", "Chyba pri spracovaní obrázku (z Bitmap) pomocou ML Kit", e)
                }
                .addOnCompleteListener {
                    recycleBitmaps(finalBitmapToProcess, argbBitmapInternal, loadedBitmapInternal)
                }

        } catch (e: OutOfMemoryError) {
            Log.e("ScanImageVM", "OutOfMemoryError pri spracovaní obrázka", e)
            _uiState.update { it.copy(isLoading = false, errorMessage = "Nedostatok pamäte pri spracovaní obrázka. Skúste menší obrázok.") }
            recycleBitmaps(finalBitmapToProcess, argbBitmapInternal, loadedBitmapInternal) // Aj tu recykluj
        } catch (e: Exception) {
            val errorMessageText = "Vyskytla sa neočakávaná chyba (Bitmap): ${e.localizedMessage}"
            _uiState.update { it.copy(isLoading = false, errorMessage = errorMessageText) }
            Log.e("ScanImageVM", "Chyba v processImageForQrCode: $errorMessageText", e)
            recycleBitmaps(finalBitmapToProcess, argbBitmapInternal, loadedBitmapInternal)
        }
    }

// ... (zvyšok ScanImageViewModel, vrátane recycleBitmaps, scaleBitmapDown, getRotationDegreesFromUri, setPreviewState, copyUriToInternalStorage, setImageCopyError) ...



    private fun recycleBitmaps(vararg bitmaps: Bitmap?) {
        bitmaps
            .filterNotNull() // Odstráni null hodnoty
            .distinct()     // Odstráni duplicitné referencie
            .forEach { bmp ->
                if (!bmp.isRecycled) {
                    bmp.recycle()
                    Log.d("ScanImageVM", "Recyklovaná bitmapa ID: ${System.identityHashCode(bmp)}")
                }
            }
    }

    private fun getRotationDegreesFromUri(context: Context, imageUri: Uri): Int {
        var rotationDegrees = 0
        try {
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val exifInterface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    ExifInterface(inputStream)
                } else {
                    // Pre staršie API by sa musela cesta získať inak, ak Uri nie je file Uri.
                    // Toto je zjednodušenie. Pre Content Uris na starších API je to komplikovanejšie.
                    val path = imageUri.path
                    if (path != null) ExifInterface(path) else {
                        Log.w("ScanImageVM", "Nepodarilo sa získať cestu pre ExifInterface na staršom API pre $imageUri")
                        return 0
                    }
                }
                val orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                rotationDegrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } catch (e: Exception) {
            Log.e("ScanImageVM", "Chyba pri čítaní EXIF orientácie pre $imageUri: ${e.message}")
        }
        Log.d("ScanImageVM", "Zistená rotácia pre obrázok: $rotationDegrees stupňov")
        return rotationDegrees
    }

    private fun scaleBitmapDown(
        bitmap: Bitmap,
        maxDimension: Int,
        filter: Boolean = true
    ): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
            Log.d("ScanImageVM_Scale", "Bitmapa nie je príliš veľká ($originalWidth x $originalHeight), vracia sa referencia alebo kópia.")
            // Ak je bitmapa mutable, môžeme vrátiť priamo ju. Ak bola immutable, .copy() v kroku 2 už vytvorilo mutable kópiu.
            // Ale argbBitmapInternal by už mala byť mutable.
            // Ak je bitmap.config null, použijeme ARGB_8888 ako default.
            return if (bitmap.isMutable) bitmap else bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        }

        val ratio = originalWidth.toFloat() / originalHeight.toFloat()
        val resizedWidth: Int
        val resizedHeight: Int

        if (originalWidth > originalHeight) {
            resizedWidth = maxDimension
            resizedHeight = (maxDimension / ratio).toInt()
        } else {
            resizedHeight = maxDimension
            resizedWidth = (maxDimension * ratio).toInt()
        }

        if (resizedWidth <= 0 || resizedHeight <= 0) {
            Log.w("ScanImageVM_Scale", "Pokus o zmenšenie na neplatné rozmery ($resizedWidth x $resizedHeight) z ($originalWidth x $originalHeight). Vracia sa kópia originálu.")
            // Ak je bitmap.config null, použijeme ARGB_8888 ako default.
            return bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true) // Vráti kópiu, aby sa nemodifikoval originál
        }
        Log.d("ScanImageVM_Scale", "Zmenšuje sa z ($originalWidth x $originalHeight) na ($resizedWidth x $resizedHeight)")
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, filter)
    }


    internal fun setPreviewState(state: ScanImageUiState) {
        _uiState.value = state
    }

    internal suspend fun copyUriToInternalStorage(context: Context, sourceUri: Uri, fileNamePrefix: String): Uri? {
        return withContext(Dispatchers.IO) { // Vykonávame I/O operácie na IO threade
            try {
                val inputStream = context.contentResolver.openInputStream(sourceUri)
                // Uloženie do app-specific files directory s unikátnym názvom
                val outputFile = File(context.filesDir, "${fileNamePrefix}_${System.currentTimeMillis()}.jpg")
                // Alebo do cache adresára, ak je to dočasné: val outputFile = File(context.cacheDir, "${fileNamePrefix}_${System.currentTimeMillis()}.jpg")

                FileOutputStream(outputFile).use { outputStream ->
                    inputStream?.use { input ->
                        input.copyTo(outputStream)
                    } ?: run {
                        Log.e("CopyUtil", "InputStream pre sourceUri $sourceUri je null")
                        return@withContext null // Návrat null ak je inputStream null
                    }
                }
                Log.d("CopyUtil", "Súbor skopírovaný do: ${outputFile.absolutePath}")
                Uri.fromFile(outputFile) // Vráti file:/// Uri pre lokálny súbor
            } catch (e: IOException) {
                Log.e("CopyUtil", "IOException pri kopírovaní Uri do interného úložiska: ${e.message}", e)
                null
            } catch (e: SecurityException) {
                Log.e("CopyUtil", "SecurityException pri kopírovaní Uri (možno neskorá strata povolenia?): ${e.message}", e)
                null
            } catch (e: Exception) {
                Log.e("CopyUtil", "Všeobecná chyba pri kopírovaní Uri: ${e.message}", e)
                null
            }
        }
    }
}
