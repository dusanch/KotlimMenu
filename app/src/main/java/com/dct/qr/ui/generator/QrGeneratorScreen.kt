package com.dct.qr.ui.generator

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
// import androidx.compose.foundation.layout.height // Odstránené, ak sa používa len v MyCustomTopBar
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dct.qr.R // Uistite sa, že máte tento import a súbor strings.xml
import com.dct.qr.data.type.QrCodeType
import com.dct.qr.ui.theme.QRTheme

// Výška pre vlastný horný panel
val MyCustomTopBarHeight =54.dp // Alebo 44.dp, 48.dp - experimentujte

@Composable
fun MyCustomTopBar(
    title: String,
    showBackButton: Boolean,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    titleVerticalPadding: Dp = 0.dp // Predvolená hodnota pre menšie medzery
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(MyCustomTopBarHeight), // Použije globálnu konštantu
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
                style = MaterialTheme.typography.titleSmall, // Zvážte menší štýl pre kompaktnejší bar
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
    val context = LocalContext.current

    val titleTextValue = if (uiState.currentScreen == GeneratorScreenState.FORM_INPUT && uiState.selectedType != null) {
        stringResource(R.string.title_create_specific_qr, uiState.selectedType!!.getDisplayName())
    } else {
        stringResource(R.string.screen_title_generate_qr)
    }

    Scaffold(
        topBar = {
            MyCustomTopBar(
                title = titleTextValue,
                showBackButton = uiState.currentScreen == GeneratorScreenState.FORM_INPUT,
                onBackClicked = { viewModel.navigateToTypeSelection() },
                backgroundColor = Color.Yellow, // Pre ladenie, zmeňte podľa potreby
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer, // Alebo Color.Black pre žlté pozadie
                titleVerticalPadding = 2.dp // Nastavte podľa potreby, napr. 0.dp alebo 2.dp
            )
        }
    ) { paddingValues -> // Tieto paddingValues by mali teraz správne zohľadniť výšku MyCustomTopBar
        when (uiState.currentScreen) {
            GeneratorScreenState.TYPE_SELECTION -> {
                QrTypeListScreen(
                    availableTypes = uiState.availableTypes,
                    onTypeSelected = viewModel::onTypeSelectedFromList,
                    modifier = Modifier.padding(paddingValues) // Aplikujeme padding zo Scaffold
                )
            }
            GeneratorScreenState.FORM_INPUT -> {
                uiState.selectedType?.let { currentType ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize() // Column by mal vyplniť priestor daný Scaffoldom
                            .padding(paddingValues) // Aplikujeme padding zo Scaffold
                            .padding(horizontal = 16.dp) // Dodatočný horizontálny padding pre obsah
                            // Vertikálny padding pre obsah Columnu, ak je potrebný (napr. 8.dp alebo 16.dp)
                            // Tento padding je OKREM toho, čo poskytuje paddingValues.calculateTopPadding()
                            .padding(bottom = 16.dp) // Príklad spodného paddingu pre obsah
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Formuláre
                        when (currentType) {
                            QrCodeType.TEXT -> {
                                TextQrForm(
                                    formData = uiState.textFormData,
                                    onTextChanged = viewModel::onTextChanged,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            QrCodeType.URL -> {
                                UrlQrForm(
                                    formData = uiState.urlFormData,
                                    onUrlChanged = viewModel::onUrlChanged,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            else -> {
                                Text(
                                    stringResource(R.string.form_not_implemented_for_type, currentType.getDisplayName()),
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.generateQrCode() },
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
                            Text(stringResource(R.string.button_generate))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Zobrazenie vygenerovaného QR kódu alebo chybovej správy
                        if (uiState.isGeneratingQr && uiState.generatedQrBitmap == null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        if (!uiState.isGeneratingQr || uiState.generatedQrBitmap != null) {
                            uiState.generatedQrBitmap?.let { bitmap ->
                                /*Text(
                                    text = stringResource(R.string.generated_qr_code_label),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )*/
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = stringResource(R.string.qr_code_image_desc),
                                    modifier = Modifier
                                        // .size(200.dp)
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .padding(vertical = 8.dp)
                                        .align(Alignment.CenterHorizontally)
                                )

                                /*uiState.generatedQrContent?.let { content ->
                                    OutlinedTextField(
                                        value = content,
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        label = { Text(stringResource(R.string.qr_code_content_textfield_label)) }
                                    )
                                }*/

                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.saveToFavorites()
                                            Toast.makeText(context, R.string.toast_saved_to_favorites_todo, Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !uiState.isGeneratingQr
                                    ) {
                                        Icon(Icons.Filled.FavoriteBorder, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text(stringResource(R.string.button_save_to_favorites))
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.downloadToGallery()
                                            Toast.makeText(context, R.string.toast_download_to_gallery_todo, Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !uiState.isGeneratingQr
                                    ) {
                                        Icon(Icons.Filled.Download, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text(stringResource(R.string.button_download_to_gallery))
                                    }
                                }
                            } ?: run {
                                if (uiState.generatedQrContent != null) {
                                    Text(
                                        text = stringResource(R.string.error_generating_qr_image),
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(vertical = 16.dp)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(0.dp))
                                }
                            }
                        }
                    }
                } ?: run {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.error_no_type_selected_for_form))
                    }
                }
            }
        }
    }
}

// ----- String Resources (ako ste mali) -----
/*
<resources>
    ...
</resources>
*/

// ----- Previews (ako ste mali, ale teraz by mali používať MyCustomTopBar nepriamo cez QrGeneratorScreen) -----
// Previews budú teraz odrážať použitie MyCustomTopBar, ak QrGeneratorViewModel
// nespôsobuje žiadne problémy pri inicializácii pre preview.

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
