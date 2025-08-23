package com.dct.qr.ui.scan // Nahraďte vaším skutočným balíčkom, ak sa líši

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class QrCodeAnalyzer(
    private val onQrCodeScanned: (value: String, type: Int) -> Unit,
    private val onAnalysisError: (Exception) -> Unit,
    private val onProcessingComplete: () -> Unit // Callback na signalizáciu ukončenia spracovania
) : ImageAnalysis.Analyzer {

    companion object {
        const val TAG = "QrCodeAnalyzer"
    }

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_DATA_MATRIX,
            Barcode.FORMAT_PDF417,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODABAR,
            Barcode.FORMAT_ITF
            // Pridajte ďalšie formáty podľa potreby
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)
    private var isScanningPaused = false

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (isScanningPaused) {
            imageProxy.close() // Dôležité aj keď je pozastavené
            onProcessingComplete() // Signalizovať, že sme skončili (aj keď sme nič nerobili)
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val barcode = barcodes.first() // Vezmeme prvý nájdený kód
                        val rawValue = barcode.rawValue
                        val valueType = barcode.valueType

                        if (rawValue != null) {
                            Log.i(TAG, "Nájdený QR/čiarový kód: '$rawValue', Typ: $valueType")
                            isScanningPaused = true // Pozastaviť ďalšie skenovanie
                            onQrCodeScanned(rawValue, valueType)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Chyba pri analýze čiarového kódu", e)
                    onAnalysisError(e)
                }
                .addOnCompleteListener {
                    // Vždy zatvorte ImageProxy, keď skončíte,
                    // aby CameraX mohla poslať ďalší frame.
                    imageProxy.close()
                    onProcessingComplete() // Signalizovať ukončenie spracovania
                }
        } else {
            // Ak mediaImage je null, stále musíme zavrieť proxy a signalizovať.
            imageProxy.close()
            onProcessingComplete()
        }
    }

    fun resumeScanning() {
        if (isScanningPaused) {
            isScanningPaused = false
            Log.d(TAG, "Skenovanie obnovené.")
        } else {
            Log.d(TAG, "Skenovanie už bolo aktívne, resumeScanning preskočené.")
        }
    }

    fun pauseScanning() {
        if(!isScanningPaused) {
            isScanningPaused = true
            Log.d(TAG, "Skenovanie pozastavené.")
        }
    }
}
