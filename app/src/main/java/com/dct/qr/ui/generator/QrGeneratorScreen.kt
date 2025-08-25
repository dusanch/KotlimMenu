package com.dct.qr.ui.generator

import android.annotation.SuppressLint
// import android.widget.Toast // Pravdepodobne už nebude potrebný tu, ak je len v QrDisplayScreen
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
// import androidx.compose.material.icons.filled.Download // Presunuté do QrDisplayScreen
// import androidx.compose.material.icons.filled.FavoriteBorder // Presunuté do QrDisplayScreen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
// import androidx.compose.ui.graphics.asImageBitmap // Presunuté do QrDisplayScreen
// import androidx.compose.ui.platform.LocalContext // Pravdepodobne už nebude potrebný tu
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dct.qr.R
import com.dct.qr.data.type.QrCodeType
import com.dct.qr.ui.theme.QRTheme
// Uistite sa, že máte správny import pre QrDisplayScreen, ak je v inom súbore
// napr. import com.dct.qr.ui.qrdisplay.QrDisplayScreen alebo podľa vašej štruktúry

// Výška pre vlastný horný panel
val MyCustomTopBarHeight = 40.dp

@Composable
fun MyCustomTopBar(
    title: String,
    showBackButton: Boolean,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    titleVerticalPadding: Dp = 0.dp
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(MyCustomTopBarHeight),
        color = backgroundColor,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.width(48.dp).fillMaxHeight()) {
                if (showBackButton) {
                    IconButton(
                        onClick = onBackClicked,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = contentColor
                        )
                    }
                }
            }
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = titleVerticalPadding)
                    .padding(horizontal = 8.dp),
                color = contentColor,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(modifier = Modifier.width(48.dp)) { /* Pre akčné ikony */ }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrGeneratorScreen(
    viewModel: QrGeneratorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val titleTextValue = when (uiState.currentScreen) {
        GeneratorScreenState.FORM_INPUT -> uiState.selectedType?.let {
            stringResource(R.string.title_create_specific_qr, it.getDisplayName())
        } ?: stringResource(R.string.screen_title_generate_qr) // Fallback
        GeneratorScreenState.QR_DISPLAY -> stringResource(R.string.screen_title_qr_display)
        else -> stringResource(R.string.screen_title_generate_qr)
    }

    Scaffold(
        topBar = {
            MyCustomTopBar(
                title = titleTextValue,
                showBackButton = uiState.currentScreen == GeneratorScreenState.FORM_INPUT || uiState.currentScreen == GeneratorScreenState.QR_DISPLAY,
                onBackClicked = {
                    when (uiState.currentScreen) {
                        GeneratorScreenState.FORM_INPUT -> viewModel.navigateToTypeSelection()
                        GeneratorScreenState.QR_DISPLAY -> viewModel.navigateBackFromQrDisplay()
                        else -> { /* Žiadna akcia alebo navigácia na predvolenú obrazovku */ }
                    }
                },
                backgroundColor = Color.Yellow, // Pre ladenie, zmeňte podľa potreby
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                titleVerticalPadding = 0.dp
            )
        }
    ) { paddingValues ->
        when (uiState.currentScreen) {
            GeneratorScreenState.TYPE_SELECTION -> {
                QrTypeListScreen(
                    availableTypes = uiState.availableTypes,
                    onTypeSelected = viewModel::onTypeSelectedFromList,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            GeneratorScreenState.FORM_INPUT -> {
                uiState.selectedType?.let { currentType ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp) // Horný padding je už z paddingValues
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Formuláre
                        when (currentType) {
                            QrCodeType.TEXT -> TextQrForm(
                                formData = uiState.textFormData,
                                onTextChanged = viewModel::onTextChanged,
                                modifier = Modifier.fillMaxWidth()
                            )
                            QrCodeType.URL -> UrlQrForm(
                                formData = uiState.urlFormData,
                                onUrlChanged = viewModel::onUrlChanged,
                                modifier = Modifier.fillMaxWidth()
                            )
                            else -> {
                                Text(
                                    stringResource(R.string.form_not_implemented_for_type, currentType.getDisplayName()),
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Zobrazenie chybovej správy z ViewModelu
                        uiState.qrCodeError?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Button(
                            onClick = { viewModel.generateQrCodeAndNavigate() },
                            enabled = uiState.isFormValid && !uiState.isGeneratingQr,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isGeneratingQr) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(stringResource(R.string.button_generate_and_show)) // Alebo iný text
                        }
                    }
                } ?: run {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.error_no_type_selected_for_form))
                    }
                }
            }
            GeneratorScreenState.QR_DISPLAY -> {
                // Tu voláte QrDisplayScreen, ktorá je teraz definovaná v samostatnom súbore
                // Uistite sa, že máte správny import pre túto funkciu.
                QrDisplayScreen(
                    qrBitmap = uiState.generatedQrBitmap,
                    qrContent = uiState.generatedQrContent,
                    isLoading = uiState.isGeneratingQr,
                    error = uiState.qrCodeError,
                    onSaveToFavorites = { viewModel.saveToFavorites() },
                    onDownloadToGallery = { viewModel.downloadToGallery() },
                    onNavigateBack = { viewModel.navigateBackFromQrDisplay() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}


// ----- Previews -----
// Tieto Previews budú stále fungovať pre TYPE_SELECTION a FORM_INPUT.
// Pre QR_DISPLAY by ste si museli vytvoriť samostatný Preview,
// ktorý by priamo volal QrDisplayScreen s ukážkovými dátami.

@SuppressLint("ComposableNaming", "ComposableViewModelCreation", "ViewModelConstructorInComposable")
@Preview(showBackground = true, name = "Type Selection Screen")
@Composable
fun QrGeneratorScreenTypeListPreview() {
    QRTheme {
        val previewViewModel = QrGeneratorViewModel()
        QrGeneratorScreen(viewModel = previewViewModel)
    }
}

@SuppressLint("ComposableNaming", "ComposableViewModelCreation", "ViewModelConstructorInComposable")
@Preview(showBackground = true, name = "Form Input Screen - TEXT")
@Composable
fun QrGeneratorScreenFormInputTextPreview() {
    QRTheme {
        val previewViewModel = QrGeneratorViewModel()
        previewViewModel.onTypeSelectedFromList(QrCodeType.TEXT)
        previewViewModel.onTextChanged("Text pre preview")
        QrGeneratorScreen(viewModel = previewViewModel)
    }
}

@SuppressLint("ComposableNaming", "ComposableViewModelCreation", "ViewModelConstructorInComposable")
@Preview(showBackground = true, name = "Form Input Screen - URL")
@Composable
fun QrGeneratorScreenFormInputUrlPreview() {
    QRTheme {
        val previewViewModel = QrGeneratorViewModel()
        previewViewModel.onTypeSelectedFromList(QrCodeType.URL)
        previewViewModel.onUrlChanged("https://example.com")
        QrGeneratorScreen(viewModel = previewViewModel)
    }
}

@SuppressLint("ComposableNaming", "ComposableViewModelCreation", "ViewModelConstructorInComposable")
@Preview(showBackground = true, name = "Form Input Screen - WIFI (Form not implemented)")
@Composable
fun QrGeneratorScreenFormInputWifiPreview() {
    QRTheme {
        val previewViewModel = QrGeneratorViewModel()
        previewViewModel.onTypeSelectedFromList(QrCodeType.WIFI)
        QrGeneratorScreen(viewModel = previewViewModel)
    }
}
