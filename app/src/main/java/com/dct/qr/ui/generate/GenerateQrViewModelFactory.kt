package com.dct.qr.ui.generate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dct.qr.data.repository.GeneratedQrRepository

class GenerateQrViewModelFactory(
    private val generatedQrRepository: GeneratedQrRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GenerateQrViewModel::class.java)) {
            return GenerateQrViewModel(generatedQrRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}