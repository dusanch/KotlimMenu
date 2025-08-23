package com.dct.qr.ui.generator

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dct.qr.data.type.QrCodeType
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TextFormData(
    val text: String = "",
    val textError: String? = null
)

data class UrlFormData(
    val url: String = "",
    val urlError: String? = null
)

data class QrGeneratorUiState(
    val currentScreen: GeneratorScreenState = GeneratorScreenState.TYPE_SELECTION,
    val selectedType: QrCodeType? = null,
    val availableTypes: List<QrCodeType> = QrCodeType.values().toList(),
    val textFormData: TextFormData = TextFormData(),
    val urlFormData: UrlFormData = UrlFormData(),
    val generatedQrContent: String? = null,
    val generatedQrBitmap: Bitmap? = null, // Pre vygenerovaný obrázok QR kódu
    val isFormValid: Boolean = false,
    val isGeneratingQr: Boolean = false // Na zobrazenie indikátora načítania
)

enum class GeneratorScreenState {
    TYPE_SELECTION,
    FORM_INPUT
}

class QrGeneratorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QrGeneratorUiState())
    val uiState: StateFlow<QrGeneratorUiState> = _uiState.asStateFlow()

    fun onTypeSelectedFromList(type: QrCodeType) {
        _uiState.update {
            it.copy(
                currentScreen = GeneratorScreenState.FORM_INPUT,
                selectedType = type,
                generatedQrContent = null,
                generatedQrBitmap = null, // Reset bitmapy
                textFormData = TextFormData(),
                urlFormData = UrlFormData(),
                isFormValid = checkFormValidity(type, TextFormData(), UrlFormData()),
                isGeneratingQr = false
            )
        }
    }

    fun navigateToTypeSelection() {
        _uiState.update {
            it.copy(
                currentScreen = GeneratorScreenState.TYPE_SELECTION,
                selectedType = null,
                generatedQrContent = null,
                generatedQrBitmap = null, // Reset bitmapy
                textFormData = TextFormData(),
                urlFormData = UrlFormData(),
                isFormValid = false,
                isGeneratingQr = false
            )
        }
    }

    fun onTextChanged(newText: String) {
        val error = if (newText.isBlank()) "Text nesmie byť prázdny." else null
        _uiState.update { currentState ->
            currentState.selectedType?.let { type ->
                val newFormData = currentState.textFormData.copy(text = newText, textError = error)
                currentState.copy(
                    textFormData = newFormData,
                    generatedQrContent = null, // Reset pri zmene vstupu
                    generatedQrBitmap = null,  // Reset bitmapy
                    isFormValid = checkFormValidity(type, newFormData, currentState.urlFormData),
                    isGeneratingQr = false
                )
            } ?: currentState
        }
    }

    fun onUrlChanged(newUrl: String) {
        val error = if (newUrl.isBlank()) {
            "URL nesmie byť prázdna."
        } else if (!android.util.Patterns.WEB_URL.matcher(
                if (newUrl.startsWith("http://") || newUrl.startsWith("https://")) newUrl
                else "https://$newUrl"
            ).matches()
        ) {
            "Neplatný formát URL."
        } else null

        _uiState.update { currentState ->
            currentState.selectedType?.let { type ->
                val newFormData = currentState.urlFormData.copy(url = newUrl, urlError = error)
                currentState.copy(
                    urlFormData = newFormData,
                    generatedQrContent = null, // Reset pri zmene vstupu
                    generatedQrBitmap = null,  // Reset bitmapy
                    isFormValid = checkFormValidity(type, currentState.textFormData, newFormData),
                    isGeneratingQr = false
                )
            } ?: currentState
        }
    }

    private fun checkFormValidity(
        type: QrCodeType,
        textData: TextFormData,
        urlData: UrlFormData
    ): Boolean {
        return when (type) {
            QrCodeType.TEXT -> textData.text.isNotBlank() && textData.textError == null
            QrCodeType.URL -> urlData.url.isNotBlank() && urlData.urlError == null
            else -> {
                // Pre ostatné typy, ktoré ešte nemajú formulár, zatiaľ false
                // Ak typ má requiresComplexInput = true, ale ešte nie je tu case, vrátime false
                if (type.requiresComplexInput) {
                    Log.d("checkFormValidity", "Typ ${type.name} vyžaduje komplexný vstup, ale validácia nie je implementovaná.")
                    false
                } else {
                    Log.d("checkFormValidity", "Validácia pre typ ${type.name} nie je špecifikovaná.")
                    false
                }
            }
        }
    }

    fun generateQrCode() {
        val currentState = _uiState.value
        if (!currentState.isFormValid || currentState.selectedType == null) {
            Log.w("QrGeneratorVM", "Pokus o generovanie QR kódu s nevalidným formulárom alebo bez vybraného typu.")
            return
        }

        _uiState.update { it.copy(isGeneratingQr = true, generatedQrBitmap = null, generatedQrContent = null) } // Začíname generovať

        val selectedType = currentState.selectedType // Už vieme, že nie je null
        val contentToEncode = when (selectedType) {
            QrCodeType.TEXT -> currentState.textFormData.text
            QrCodeType.URL -> {
                val rawUrl = currentState.urlFormData.url
                if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                    rawUrl
                } else {
                    (selectedType.dataPrefix ?: "https://") + rawUrl
                }
            }
            else -> {
                Log.w("QrGeneratorVM", "Generovanie pre typ ${selectedType.name} ešte nie je plne implementované pre textový obsah.")
                null
            }
        }

        if (contentToEncode != null) {
            viewModelScope.launch {
                val bitmap = try {
                    createQrBitmapFromContent(contentToEncode)
                } catch (e: Exception) {
                    Log.e("QrGeneratorVM", "Chyba pri generovaní QR bitmapy pre: $contentToEncode", e)
                    null
                }
                _uiState.update {
                    it.copy(
                        generatedQrContent = contentToEncode, // Uložíme aj textový obsah
                        generatedQrBitmap = bitmap,
                        isGeneratingQr = false // Generovanie ukončené
                    )
                }
            }
        } else {
            // Ak nebol získaný obsah na kódovanie
            _uiState.update {
                it.copy(
                    generatedQrContent = null,
                    generatedQrBitmap = null,
                    isGeneratingQr = false // Generovanie ukončené (neúspešne)
                )
            }
        }
    }

    /**
     * Generuje QR kód ako Bitmap z daného textového obsahu pomocou knižnice ZXing.
     * Táto funkcia by mala byť volaná z korutiny bežiacej na Dispatchers.Default alebo Dispatchers.IO.
     */
    private suspend fun createQrBitmapFromContent(
        content: String,
        width: Int = 512, // Šírka výslednej bitmapy v pixeloch
        height: Int = 512, // Výška výslednej bitmapy v pixeloch
        marginBlocks: Int = 1 // Veľkosť okraja v blokoch QR kódu (0 pre žiadny okraj)
    ): Bitmap? {
        return withContext(Dispatchers.Default) {
            try {
                val hints = mutableMapOf<EncodeHintType, Any>()
                hints[EncodeHintType.CHARACTER_SET] = "UTF-8" // Explicitné nastavenie kódovania
                hints[EncodeHintType.MARGIN] = marginBlocks    // Nastavenie okraja

                val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    width,
                    height,
                    hints
                )

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                    }
                }
                bitmap
            } catch (e: Exception) {
                Log.e("createQrBitmap", "Chyba pri generovaní QR bitmapy z obsahu: $content", e)
                null // Vráti null v prípade chyby
            }
        }
    }

    fun saveToFavorites() {
        val content = _uiState.value.generatedQrContent
        val type = _uiState.value.selectedType
        // Bitmapa sa priamo neukladá do favorites, len jej obsah a typ
        if (content != null && type != null) {
            Log.d("QrGeneratorVM", "Požiadavka na uloženie do obľúbených: Typ=${type.name}, Obsah=$content")
            // TODO: Implementovať logiku pre ukladanie do databázy/SharedPreferences
        } else {
            Log.w("QrGeneratorVM", "Nie je čo uložiť do obľúbených (chýba obsah alebo typ).")
        }
    }

    fun downloadToGallery() {
        val bitmap = _uiState.value.generatedQrBitmap // Na stiahnutie potrebujeme bitmapu
        if (bitmap != null) {
            Log.d("QrGeneratorVM", "Požiadavka na stiahnutie QR bitmapy do galérie.")
            // TODO: Implementovať logiku pre generovanie Bitmapy a uloženie do galérie
            // Bude to vyžadovať Context a pravdepodobne CoroutineScope pre I/O operácie.
        } else {
            Log.w("QrGeneratorVM", "Nie je čo stiahnuť do galérie (chýba vygenerovaná bitmapa).")
        }
    }
}
