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
    val generatedQrBitmap: Bitmap? = null,
    val isFormValid: Boolean = false,
    val isGeneratingQr: Boolean = false,
    val qrCodeError: String? = null
)

enum class GeneratorScreenState {
    TYPE_SELECTION,
    FORM_INPUT,
    QR_DISPLAY
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
                generatedQrBitmap = null,
                textFormData = TextFormData(),
                urlFormData = UrlFormData(),
                isFormValid = checkFormValidity(type, TextFormData(), UrlFormData()),
                isGeneratingQr = false,
                qrCodeError = null
            )
        }
    }

    fun navigateToTypeSelection() {
        _uiState.update {
            it.copy(
                currentScreen = GeneratorScreenState.TYPE_SELECTION,
                selectedType = null,
                generatedQrContent = null,
                generatedQrBitmap = null,
                textFormData = TextFormData(),
                urlFormData = UrlFormData(),
                isFormValid = false,
                isGeneratingQr = false,
                qrCodeError = null
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
                    isFormValid = checkFormValidity(type, newFormData, currentState.urlFormData),
                    isGeneratingQr = false,
                    qrCodeError = null // Chyba sa resetuje pri zmene vstupu
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
                    isFormValid = checkFormValidity(type, currentState.textFormData, newFormData),
                    isGeneratingQr = false,
                    qrCodeError = null // Chyba sa resetuje pri zmene vstupu
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
            // TODO: Implementovať validáciu pre ostatné typy, ak vyžadujú vstupné polia
            else -> {
                // Pre typy, ktoré nevyžadujú komplexný vstup (napr. WIFI, VEVENT, atď.),
                // kde by formulár mohol byť rozsiahlejší a validácia zložitejšia.
                // Ak typ vyžaduje komplexný vstup (type.requiresComplexInput == true),
                // ale ešte tu nie je case, vrátime false, kým sa neimplementuje.
                // Ak nevyžaduje komplexný vstup (je to len placeholder), môžeme vrátiť true
                // alebo false podľa toho, či chceme povoliť generovanie "prázdneho" QR (čo zvyčajne nie).
                if (type.requiresComplexInput) {
                    Log.d("checkFormValidity", "Typ ${type.name} vyžaduje komplexný vstup, ale validácia nie je implementovaná.")
                    false
                } else {
                    // Pre jednoduché typy, ktoré nemajú špecifický formulár, zatiaľ false.
                    // Alebo ak by sme chceli pre niektoré typy povoliť "prázdne" generovanie, tu by bola logika.
                    Log.d("checkFormValidity", "Validácia pre typ ${type.name} nie je špecifikovaná, predpokladá sa nevalidný.")
                    false
                }
            }
        }
    }

    fun generateQrCodeAndNavigate() {
        val currentState = _uiState.value
        if (!currentState.isFormValid || currentState.selectedType == null) {
            Log.w("QrGeneratorVM", "Pokus o generovanie QR kódu s nevalidným formulárom alebo bez vybraného typu.")
            _uiState.update { it.copy(qrCodeError = "Formulár nie je správne vyplnený alebo nie je vybraný typ QR kódu.", isGeneratingQr = false) }
            return
        }

        _uiState.update { it.copy(isGeneratingQr = true, generatedQrBitmap = null, generatedQrContent = null, qrCodeError = null) }

        val selectedType = currentState.selectedType
        val contentToEncode = when (selectedType) {
            QrCodeType.TEXT -> currentState.textFormData.text
            QrCodeType.URL -> {
                val rawUrl = currentState.urlFormData.url
                // Zaistíme, že URL má schému
                if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                    rawUrl
                } else {
                    (selectedType.dataPrefix ?: "https://") + rawUrl
                }
            }
            // TODO: Implementovať získavanie obsahu pre ostatné QrCodeType
            else -> {
                Log.w("QrGeneratorVM", "Generovanie pre typ ${selectedType.name} ešte nie je plne implementované pre textový obsah.")
                _uiState.update { it.copy(isGeneratingQr = false, qrCodeError = "Generovanie pre typ ${selectedType.name} ešte nie je podporované.") }
                return
            }
        }

        if (contentToEncode.isBlank()) {
             Log.w("QrGeneratorVM", "Obsah na zakódovanie je prázdny pre typ ${selectedType.name}.")
            _uiState.update { it.copy(isGeneratingQr = false, qrCodeError = "Obsah na zakódovanie je prázdny.") }
            return
        }

        viewModelScope.launch {
            val bitmap = try {
                createQrBitmapFromContent(contentToEncode)
            } catch (e: Exception) {
                Log.e("QrGeneratorVM", "Chyba pri generovaní QR bitmapy pre: $contentToEncode", e)
                null
            }

            if (bitmap != null) {
                _uiState.update {
                    it.copy(
                        generatedQrContent = contentToEncode,
                        generatedQrBitmap = bitmap,
                        isGeneratingQr = false,
                        currentScreen = GeneratorScreenState.QR_DISPLAY,
                        qrCodeError = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isGeneratingQr = false,
                        qrCodeError = "Chyba pri vytváraní obrázku QR kódu.",
                        generatedQrBitmap = null,
                        generatedQrContent = contentToEncode // Ponecháme obsah pre prípadnú diagnostiku
                    )
                }
            }
        }
    }

    fun navigateBackFromQrDisplay() {
        _uiState.update {
            it.copy(
                currentScreen = GeneratorScreenState.FORM_INPUT, // Vrátime sa na formulár
                generatedQrBitmap = null, // Reset QR dát po opustení obrazovky zobrazenia
                generatedQrContent = null,
                qrCodeError = null // Reset chybovej správy
            )
        }
    }

    private suspend fun createQrBitmapFromContent(
        content: String,
        width: Int = 512,
        height: Int = 512,
        marginBlocks: Int = 1
    ): Bitmap? {
        return withContext(Dispatchers.Default) {
            try {
                val hints = mutableMapOf<EncodeHintType, Any>()
                hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
                hints[EncodeHintType.MARGIN] = marginBlocks

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
            // Môžete použiť MediaStore API.
        } else {
            Log.w("QrGeneratorVM", "Nie je čo stiahnuť do galérie (chýba vygenerovaná bitmapa).")
        }
    }
}

