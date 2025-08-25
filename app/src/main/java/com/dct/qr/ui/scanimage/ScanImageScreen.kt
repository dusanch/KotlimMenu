package com.dct.qr.ui.scanimage

import android.annotation.SuppressLint // Potrebné pre @SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dct.qr.R
import com.dct.qr.ui.theme.QRTheme
import androidx.compose.runtime.rememberCoroutineScope // Pridajte tento import
import kotlinx.coroutines.launch // Pridajte tento import

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanImageScreen(
    viewModel: ScanImageViewModel = viewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope() // Získanie CoroutineScope

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            // Už nepotrebujeme takePersistableUriPermission, ak hneď kopírujeme
            Log.d("ScanImageScreen", "Získané URI z výberu: $sourceUri. Pokúšam sa skopírovať.")
            scope.launch { // Spustenie coroutine pre kopírovanie
                val localFileUri = viewModel.copyUriToInternalStorage(context, sourceUri, "selected_qr_image")
                if (localFileUri != null) {
                    Log.d("ScanImageScreen", "Obrázok úspešne skopírovaný do: $localFileUri. Spracovávam...")
                    viewModel.processImageForQrCode(context, localFileUri)
                } else {
                    Log.e("ScanImageScreen", "Nepodarilo sa skopírovať obrázok $sourceUri do interného úložiska.")
                    // Informujte používateľa o chybe, napr. cez uiState vo ViewModele
                    viewModel.setImageCopyError("Nepodarilo sa načítať obrázok na spracovanie.")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.screen_title_scan_image)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.cd_navigate_back)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                pickImageLauncher.launch("image/*")
            }) {
                Text(stringResource(id = R.string.button_select_image_to_scan))
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator()
                Text(
                    stringResource(id = R.string.text_processing_image),
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            uiState.scannedQrContent?.let { content ->
                Text(
                    stringResource(id = R.string.text_qr_code_found),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    content,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    Log.d("ScanImageScreen", "TODO: Implementovať kopírovanie obsahu: $content")
                    // Príklad implementácie kopírovania:
                    // val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    // val clip = android.content.ClipData.newPlainText("QR Code Content", content)
                    // clipboardManager.setPrimaryClip(clip)
                    // android.widget.Toast.makeText(context, context.getString(R.string.toast_content_copied), android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(id = R.string.button_copy_content))
                }
            }

            uiState.noQrFoundMessage?.let { message ->
                Text(
                    message,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            uiState.errorMessage?.let { error ->
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

// --- Preview funkcie ---

// Predpokladáme, že ScanImageViewModel je 'open' a má 'internal fun setPreviewState'
private class PreviewScanImageViewModel : ScanImageViewModel() {
    fun setupState(initialState: ScanImageUiState) {
        super.setPreviewState(initialState)
    }
}

// Potlačenie lint varovania pre priame vytváranie ViewModelu v Preview
// Presný názov pravidla môže byť napr. "viewModel_composable_call", "ViewModelForwarding",
// alebo iné v závislosti od verzie Lint nástrojov.
// "ComposableNaming" je pre prípad, ak by IDE malo problém s názvom Preview funkcie (nemalo by).
// Ak "viewModel_composable_call" nefunguje, skúste nájsť presný kód chyby, ktorú Lint vypisuje.
@SuppressLint("ComposableNaming") // Všeobecné pre pomenovanie composables, ak by bolo relevantné
@Suppress("viewModel_composable_call", "ViewModelInjection") // Skúšame bežné názvy pravidiel
@Preview(showBackground = true, name = "Scan Image Screen - Initial")
@Composable
private fun ScanImageScreenPreviewInitial() {
    val previewViewModel = PreviewScanImageViewModel().apply {
        setupState(ScanImageUiState(isLoading = false, scannedQrContent = null, errorMessage = null, noQrFoundMessage = null))
    }
    QRTheme {
        ScanImageScreen(viewModel = previewViewModel, onNavigateBack = {})
    }
}

@SuppressLint("ComposableNaming")
@Suppress("viewModel_composable_call", "ViewModelInjection")
@Preview(showBackground = true, name = "Scan Image Screen - No Result")
@Composable
private fun ScanImageScreenPreviewNoResult() {
    val previewViewModel = PreviewScanImageViewModel().apply {
        setupState(ScanImageUiState(isLoading = false, noQrFoundMessage = "Žiadny QR kód na ukážku nebol nájdený."))
    }
    QRTheme {
        ScanImageScreen(viewModel = previewViewModel, onNavigateBack = {})
    }
}

@SuppressLint("ComposableNaming")
@Suppress("viewModel_composable_call", "ViewModelInjection")
@Preview(showBackground = true, name = "Scan Image Screen - With Result")
@Composable
private fun ScanImageScreenPreviewWithResult() {
    val previewViewModel = PreviewScanImageViewModel().apply {
        setupState(ScanImageUiState(isLoading = false, scannedQrContent = "https://example.com/preview-qr-data"))
    }
    QRTheme {
        ScanImageScreen(viewModel = previewViewModel, onNavigateBack = {})
    }
}

@SuppressLint("ComposableNaming")
@Suppress("viewModel_composable_call", "ViewModelInjection")
@Preview(showBackground = true, name = "Scan Image Screen - Loading")
@Composable
private fun ScanImageScreenPreviewLoading() {
    val previewViewModel = PreviewScanImageViewModel().apply {
        setupState(ScanImageUiState(isLoading = true))
    }
    QRTheme {
        ScanImageScreen(viewModel = previewViewModel, onNavigateBack = {})
    }
}

@SuppressLint("ComposableNaming")
@Suppress("viewModel_composable_call", "ViewModelInjection")
@Preview(showBackground = true, name = "Scan Image Screen - Error")
@Composable
private fun ScanImageScreenPreviewError() {
    val previewViewModel = PreviewScanImageViewModel().apply {
        setupState(ScanImageUiState(isLoading = false, errorMessage = "Ukážková chybová správa pri skenovaní obrázka."))
    }
    QRTheme {
        ScanImageScreen(viewModel = previewViewModel, onNavigateBack = {})
    }
}

