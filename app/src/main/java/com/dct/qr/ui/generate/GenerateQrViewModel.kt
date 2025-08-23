package com.dct.qr.ui.generate

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dct.qr.data.model.GeneratedQrCodeEntity
import com.dct.qr.data.repository.GeneratedQrRepository
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.EnumMap

data class GenerateQrUiState(
    val inputText: String = "",
    val generatedQrBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

open class GenerateQrViewModel( // 'open' pre prípadné dedenie vo Fake verziách pre Preview
    private val generatedQrRepository: GeneratedQrRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenerateQrUiState())
    val uiState: StateFlow<GenerateQrUiState> = _uiState.asStateFlow()

    private val _dbQrEntityState = MutableStateFlow<GeneratedQrCodeEntity?>(null)
    val dbQrEntityState: StateFlow<GeneratedQrCodeEntity?> = _dbQrEntityState.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // ===== NOVÁ METÓDA PRE RESET =====
    fun clearInputAndState() {
        _uiState.update {
            GenerateQrUiState() // Vráti na úplne počiatočný stav
            // Alebo ak chcete zachovať niektoré iné časti stavu:
            // it.copy(
            //     inputText = "",
            //     generatedQrBitmap = null,
            //     errorMessage = null
            //     // isLoading by sa mal tiež resetovať, alebo manažovať inak
            // )
        }
        _dbQrEntityState.value = null // Resetovať aj info o entite z DB
        Log.d("ViewModel", "clearInputAndState: Stav bol resetovaný.")
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { currentState ->
            currentState.copy(
                inputText = text,
                errorMessage = null,
                // generatedQrBitmap = null // Resetovať až priamo pri generovaní
            )
        }
        if (text.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                Log.d("ViewModel", "onInputTextChanged: Načítavam dbEntity pre '$text'")
                val entity = generatedQrRepository.getByContent(text)
                _dbQrEntityState.value = entity
                Log.d("ViewModel", "onInputTextChanged: dbEntity pre '$text' je: $entity")
            }
        } else {
            _dbQrEntityState.value = null
            _uiState.update { it.copy(generatedQrBitmap = null) }
        }
    }

    fun generateQrCode() {
        val currentInputText = _uiState.value.inputText
        if (currentInputText.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Text pre QR kód nemôže byť prázdny.") } // Použite string resource
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, generatedQrBitmap = null) } // Reset bitmapy tu

        viewModelScope.launch(Dispatchers.Default) {
            try {
                Log.d("ViewModel", "Začínam generovať bitmapu pre: $currentInputText")
                val writer = QRCodeWriter()
                val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                    put(EncodeHintType.CHARACTER_SET, "UTF-8")
                    put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L)
                    put(EncodeHintType.MARGIN, 1)
                }
                // ***** ZMENA VEĽKOSTI QR KÓDU *****
                val qrSize = 256 // Nová veľkosť (napr. 256x256)
                // val qrSize = 128 // Alebo ešte menší pre test

                Log.i("ViewModel_Debug", "Cieľová veľkosť QR kódu: ${qrSize}x${qrSize}")

                val bitMatrix = writer.encode(currentInputText, BarcodeFormat.QR_CODE, qrSize, qrSize, hints)
                Log.i("ViewModel_Debug", "BitMatrix vygenerovaná: width=${bitMatrix.width}, height=${bitMatrix.height}")

                val width = bitMatrix.width
                val height = bitMatrix.height
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                Log.i("ViewModel_Debug", "Prázdna bitmapa vytvorená: ${bmp.width}x${bmp.height}")

                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                    }
                }
                Log.i("ViewModel_Debug", "Bitmapa naplnená pixelmi.")

                val finalBitmapForUi = bmp
                Log.d("ViewModel_Debug", "FINÁLNA BITMAPA pred update UI: ${if (finalBitmapForUi != null) "EXISTUJE" else "NULL"}, ${finalBitmapForUi?.width}x${finalBitmapForUi?.height}")

                _uiState.update { currentState ->
                    Log.d("ViewModel_Debug", "Aktualizujem _uiState: generatedQrBitmap bude ${if (finalBitmapForUi != null) "EXISTOVAŤ" else "NULL"}")
                    currentState.copy(generatedQrBitmap = finalBitmapForUi, isLoading = false)
                }
                Log.d("ViewModel_Debug", "_uiState PO UPDATE: generatedQrBitmap je ${if (_uiState.value.generatedQrBitmap != null) "EXISTUJE" else "NULL"}")

                launch(Dispatchers.IO) {
                    Log.d("ViewModel_Debug", "Načítavam dbEntity po generovaní pre: $currentInputText")
                    val entity = generatedQrRepository.getByContent(currentInputText)
                    _dbQrEntityState.value = entity
                    Log.d("ViewModel_Debug", "Po generovaní, dbEntity pre '$currentInputText' je: $entity")
                }


            } catch (e: Exception) {
                Log.e("ViewModel", "Chyba pri generovaní QR kódu pre '$currentInputText': ${e.message}", e)
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        errorMessage = "Chyba pri generovaní QR kódu: ${e.message}" // Použite string resource
                    )
                }
                _dbQrEntityState.value = null
            }
        }
    }

    fun saveQrToGallery() {
        val bitmapToSave = _uiState.value.generatedQrBitmap
        val contentToSave = _uiState.value.inputText

        if (bitmapToSave == null) {
            _userMessage.value = "Najprv vygenerujte QR kód." // String resource
            return
        }
        if (contentToSave.isBlank()) {
            _userMessage.value = "Obsah QR kódu je prázdny." // String resource
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Skutočné ukladanie do galérie
                val result = generatedQrRepository.saveBitmapToGallery(bitmapToSave, "QR_${System.currentTimeMillis()}")
                val imageGalleryPath: String?

                if (result.isSuccess) {
                    imageGalleryPath = result.getOrNull()?.toString()
                    Log.d("ViewModel", "Úspešné uloženie do galérie pre: $contentToSave, cesta: $imageGalleryPath")
                    _userMessage.value = "QR kód uložený do galérie." // String resource

                    // Aktualizácia alebo vloženie do internej databázy po uložení do galérie
                    // Tu používame existujúci dbQrEntityState.value na určenie, či už bol kód obľúbený
                    val currentDbEntity = _dbQrEntityState.value
                    updateOrInsertInAppDatabase(contentToSave, imageGalleryPath, currentDbEntity?.isFavorite ?: false)

                } else {
                    val error = result.exceptionOrNull()
                    Log.e("ViewModel", "Chyba pri ukladaní QR do galérie: ${error?.message}", error)
                    _userMessage.value = "Chyba pri ukladaní do galérie: ${error?.message}" // String resource
                }
            } catch (e: Exception) { // Neočakávaná chyba
                Log.e("ViewModel", "Neočakávaná chyba pri ukladaní QR do galérie: ${e.message}", e)
                _userMessage.value = "Neočakávaná chyba pri ukladaní do galérie." // String resource
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleFavorite() {
        val currentContent = _uiState.value.inputText
        if (currentContent.isBlank()) {
            _userMessage.value = "Najprv zadajte text a vygenerujte QR kód." // String resource
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var currentEntity = _dbQrEntityState.value
                // Ak by sa stalo, že _dbQrEntityState nebolo ešte načítané pre currentContent, skúsime načítať
                if (currentEntity?.content != currentContent) {
                    Log.d("ViewModel", "toggleFavorite: Entita pre '$currentContent' nie je aktuálna alebo chýba, načítavam...")
                    currentEntity = generatedQrRepository.getByContent(currentContent)
                }

                if (currentEntity != null) {
                    val newFavoriteState = !currentEntity.isFavorite
                    val updatedEntity = currentEntity.copy(
                        isFavorite = newFavoriteState,
                        timestamp = System.currentTimeMillis() // Vždy aktualizuj timestamp
                    )
                    generatedQrRepository.updateGeneratedQr(updatedEntity) // <--- VOLANIE NOVEJ METÓDY
                    _dbQrEntityState.value = updatedEntity
                    _userMessage.value = if (newFavoriteState) "Pridané do obľúbených" else "Odstránené z obľúbených"
                    Log.d("ViewModel", "toggleFavorite: Entita ${currentEntity.id} aktualizovaná, isFavorite=$newFavoriteState")
                } else {
                    // Kód ešte nie je v DB (alebo nebol nájdený po opätovnom načítaní), vytvoríme ho a označíme ako obľúbený
                    if (_uiState.value.generatedQrBitmap == null) {
                        _userMessage.value = "Najprv vygenerujte QR kód, aby mohol byť pridaný ako obľúbený."
                        _uiState.update { it.copy(isLoading = false) }
                        return@launch
                    }
                    val newEntity = GeneratedQrCodeEntity(
                        content = currentContent,
                        imagePath = null, // Cesta k obrázku sa pridá, ak sa uloží do galérie
                        isFavorite = true, // Pri prvom pridaní cez toto tlačidlo je vždy obľúbený
                        timestamp = System.currentTimeMillis()
                    )
                    val newId = generatedQrRepository.insertNewGeneratedQr(newEntity) // <--- VOLANIE NOVEJ METÓDY
                    if (newId > 0) {
                        _dbQrEntityState.value = newEntity.copy(id = newId.toInt())
                        _userMessage.value = "Pridané do obľúbených a uložené v aplikácii."
                        Log.d("ViewModel", "toggleFavorite: Nová entita pre '$currentContent' uložená, isFavorite=true, id=$newId")
                    } else {
                        _userMessage.value = "Chyba pri ukladaní a pridaní do obľúbených."
                        Log.e("ViewModel", "toggleFavorite: Chyba pri ukladaní novej entity pre '$currentContent'")
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Chyba pri toggleFavorite pre '$currentContent': ${e.message}", e)
                _userMessage.value = "Chyba pri aktualizácii obľúbenosti: ${e.message}"
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun updateOrInsertInAppDatabase(content: String, imageGalleryPath: String?, currentFavoriteStatus: Boolean) {
        // Táto metóda je volaná z saveQrToGallery, takže Dispatchers.IO už je aktívny
        try {
            var entityToSave = generatedQrRepository.getByContent(content)
            if (entityToSave != null) {
                entityToSave = entityToSave.copy(
                    imagePath = imageGalleryPath ?: entityToSave.imagePath,
                    isFavorite = currentFavoriteStatus, // Zachová alebo nastaví aktuálny stav obľúbenosti
                    timestamp = System.currentTimeMillis()
                )
                generatedQrRepository.updateGeneratedQr(entityToSave) // <--- VOLANIE NOVEJ METÓDY
                Log.d("ViewModel", "updateOrInsertInAppDatabase: Entita pre '$content' aktualizovaná.")
            } else {
                entityToSave = GeneratedQrCodeEntity(
                    content = content,
                    imagePath = imageGalleryPath,
                    isFavorite = currentFavoriteStatus, // Ak ukladáme prvýkrát po galérii, môže byť true/false podľa toho, čo bolo predtým
                    timestamp = System.currentTimeMillis()
                )
                val newId = generatedQrRepository.insertNewGeneratedQr(entityToSave) // <--- VOLANIE NOVEJ METÓDY
                entityToSave = entityToSave.copy(id = newId.toInt())
                Log.d("ViewModel", "updateOrInsertInAppDatabase: Nová entita pre '$content' uložená s id $newId.")
            }
            _dbQrEntityState.value = entityToSave
        } catch (e: Exception) {
            Log.e("ViewModel", "Chyba pri updateOrInsertInAppDatabase pre '$content': ${e.message}", e)
            // Tu by sme mohli zvážiť aj _userMessage, ale saveQrToGallery už rieši hlavné správy
        }
    }

    fun onUserMessageShown() {
        _userMessage.value = null
    }

    // Metódy pre Preview (ak ich potrebujete)
    fun setPreviewState(previewState: GenerateQrUiState) {
        _uiState.value = previewState
    }

    fun setPreviewDbEntityState(entity: GeneratedQrCodeEntity?) {
        _dbQrEntityState.value = entity
    }
}
