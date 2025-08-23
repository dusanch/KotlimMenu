package com.dct.qr.ui.history // Alebo váš preferovaný balíček pre UI

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dct.qr.data.db.ScannedCode
import com.dct.qr.data.repository.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch // Import pre error handling, ak ho budete chcieť pridať
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: HistoryRepository) : ViewModel() {

    val historyUiState: StateFlow<HistoryUiState> =
        repository.allScannedCodes // Zdrojový Flow<List<ScannedCode>>
            .map { list -> // Transformácia Flow<List<ScannedCode>> na Flow<HistoryUiState>
                if (list.isEmpty()) {
                    HistoryUiState.Empty
                } else {
                    HistoryUiState.Success(list)
                }
            }
            .catch { e -> // Voliteľné: Zachytenie chýb z repository.allScannedCodes alebo z .map
                // Emituje Error stav v prípade chyby a dokončí flow (alebo re-throw, ak je to potrebné)
                // Log.e("HistoryViewModel", "Error in historyUiState flow", e) // Príklad logovania
                emit(HistoryUiState.Error(e.message ?: "An unknown error occurred"))
            }
            .stateIn( // Konverzia Flow<HistoryUiState> na StateFlow<HistoryUiState>
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L), // Zostane aktívny 5s po odhlásení posledného odberateľa
                initialValue = HistoryUiState.Loading // Počiatočný stav, kým flow nezačne emitovať
            )

    fun deleteCode(scannedCode: ScannedCode) {
        viewModelScope.launch {
            repository.deleteScannedCode(scannedCode)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}

// Stavy pre UI histórie
sealed interface HistoryUiState {
    object Loading : HistoryUiState
    data class Success(val codes: List<ScannedCode>) : HistoryUiState
    object Empty : HistoryUiState // Pre prípad, že história je prázdna
    data class Error(val message: String) : HistoryUiState // Voliteľné pre error handling
}

// Factory pre vytvorenie HistoryViewModel s parametrami (Repository)
class HistoryViewModelFactory(private val repository: HistoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
