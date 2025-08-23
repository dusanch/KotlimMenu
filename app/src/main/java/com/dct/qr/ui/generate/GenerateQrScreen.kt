@file:OptIn(ExperimentalMaterial3Api::class)

package com.dct.qr.ui.generate

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color // Používané v Preview pre Canvas
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dct.qr.R
import com.dct.qr.data.db.dao.GeneratedQrCodeDao
import com.dct.qr.data.model.GeneratedQrCodeEntity
import com.dct.qr.data.repository.GeneratedQrRepository
import com.dct.qr.ui.theme.QRTheme
import kotlinx.coroutines.flow.Flow
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun GenerateQrScreen(
    generateQrViewModel: GenerateQrViewModel
) {
    val uiState by generateQrViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage by generateQrViewModel.userMessage.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        Log.d("GenerateQrScreen", "LaunchedEffect(Unit) called, clearing ViewModel state.")
        generateQrViewModel.clearInputAndState()
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
            generateQrViewModel.onUserMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_title_generate_qr)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = { generateQrViewModel.onInputTextChanged(it) },
                label = { Text(stringResource(R.string.label_qr_text_input)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false, // Umožňuje viacriadkový vstup
                isError = uiState.errorMessage != null
            )

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = { generateQrViewModel.generateQrCode() },
                enabled = !uiState.isLoading && uiState.inputText.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.button_generate_qr))
                }
            }

            // --- ČASŤ S OBRÁZKOM A TLAČIDLAMI ---
            if (uiState.generatedQrBitmap != null) { // Použijeme if namiesto .let pre lepšiu čitateľnosť tejto podmienky
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.label_generated_qr_code),
                    style = MaterialTheme.typography.titleMedium
                )
                Image(
                    bitmap = uiState.generatedQrBitmap!!.asImageBitmap(), // Použijeme !!, lebo sme v if bloku
                    contentDescription = stringResource(R.string.desc_generated_qr_code),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .aspectRatio(1f)
                        .padding(vertical = 8.dp),
                    contentScale = ContentScale.Fit
                )

                // Stav pre tlačidlo obľúbených z ViewModelu
                val dbEntity by generateQrViewModel.dbQrEntityState.collectAsState()

                // Riadok s tlačidlami "Uložiť" a "Obľúbené"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tlačidlo Uložiť do Galérie
                    Button(
                        onClick = { generateQrViewModel.saveQrToGallery() },
                        enabled = !uiState.isLoading // Bitmapa je už overená vyššie
                    ) {
                        Icon(
                            Icons.Filled.SaveAlt,
                            contentDescription = stringResource(R.string.button_save_to_gallery)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.button_save))
                    }

                    // Tlačidlo Obľúbené
                    IconButton(
                        onClick = { generateQrViewModel.toggleFavorite() },
                        enabled = !uiState.isLoading && dbEntity != null
                    ) {
                        Icon(
                            imageVector = if (dbEntity?.isFavorite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (dbEntity?.isFavorite == true) stringResource(R.string.desc_remove_from_favorites) else stringResource(R.string.desc_add_to_favorites),
                            tint = if (dbEntity?.isFavorite == true) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                }
            }
            // --- KONIEC ČASTI S OBRÁZKOM A TLAČIDLAMI ---
        }
    }
}

// ----- FAKE IMPLEMENTATIONS FOR PREVIEW -----
// Tieto zostávajú rovnaké ako v predchádzajúcej verzii, vrátane úprav pre testovanie obľúbených

private class FakeGeneratedQrDao : GeneratedQrCodeDao {
    private val codes = mutableMapOf<Long, GeneratedQrCodeEntity>()
    private var nextId = 1L

    override suspend fun insert(qrCode: GeneratedQrCodeEntity): Long {
        val idToInsert = qrCode.id.takeIf { it != 0 }?.toLong() ?: nextId++
        val toInsert = qrCode.copy(id = idToInsert.toInt())
        codes[idToInsert] = toInsert
        return idToInsert
    }

    override suspend fun update(qrCode: GeneratedQrCodeEntity) {
        if (codes.containsKey(qrCode.id.toLong())) {
            codes[qrCode.id.toLong()] = qrCode
        }
    }

    override suspend fun delete(qrCode: GeneratedQrCodeEntity) {
        codes.remove(qrCode.id.toLong())
    }

    override fun getById(id: Int): Flow<GeneratedQrCodeEntity?> {
        return flowOf(codes[id.toLong()])
    }

    override fun getAll(): Flow<List<GeneratedQrCodeEntity>> {
        return flowOf(codes.values.toList().sortedByDescending { it.timestamp })
    }

    override fun getFavorites(): Flow<List<GeneratedQrCodeEntity>> {
        return flowOf(codes.values.filter { it.isFavorite }.sortedByDescending { it.timestamp })
    }

    override suspend fun getByContent(content: String): GeneratedQrCodeEntity? {
        val found = codes.values.find { it.content == content }
        if (content == "preview_favorite_initial_not_fav" && found == null) {
            return GeneratedQrCodeEntity(id = 100, content = content, imagePath = null, isFavorite = false, timestamp = System.currentTimeMillis())
        }
        if (content == "preview_favorite_initial_is_fav" && found == null) {
            return GeneratedQrCodeEntity(id = 101, content = content, imagePath = null, isFavorite = true, timestamp = System.currentTimeMillis())
        }
        return found
    }
}

private open class FakeGeneratedQrRepository(
    private val context: Context,
    private val fakeDao: GeneratedQrCodeDao
) : GeneratedQrRepository(fakeDao, context) {
    override suspend fun getByContent(content: String): GeneratedQrCodeEntity? {
        return fakeDao.getByContent(content)
    }
}

// ----- PREVIEW FUNCTIONS -----
// Tieto zostávajú rovnaké, vrátane nastavenia setPreviewState a onInputTextChanged

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, name = "Generate QR Screen - Empty")
@Composable
fun GenerateQrScreenPreview() {
    QRTheme {
        val context = LocalContext.current
        val fakeRepo = remember { FakeGeneratedQrRepository(context.applicationContext, FakeGeneratedQrDao()) }
        val fakeViewModel = remember { GenerateQrViewModel(fakeRepo) }
        GenerateQrScreen(generateQrViewModel = fakeViewModel)
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, name = "Generate QR Screen - With Bitmap, Not Favorite")
@Composable
fun GenerateQrScreenWithBitmapNotFavoritePreview() {
    QRTheme {
        val context = LocalContext.current
        val fakeDao = remember { FakeGeneratedQrDao() }
        val fakeRepo = remember { FakeGeneratedQrRepository(context.applicationContext, fakeDao) }
        val fakeViewModel = remember { GenerateQrViewModel(fakeRepo) }

        val previewBitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).apply {
            val canvas = android.graphics.Canvas(this)
            val paint = android.graphics.Paint().apply { color = Color.LTGRAY; style = android.graphics.Paint.Style.FILL }
            canvas.drawRect(0f, 0f, 256f, 256f, paint)
            paint.color = Color.BLACK; paint.textSize = 30f; paint.textAlign = android.graphics.Paint.Align.CENTER
            canvas.drawText("NOT FAV", 128f, 128f + paint.textSize / 3, paint)
        }
        val inputText = "preview_favorite_initial_not_fav"

        fakeViewModel.setPreviewState(
            GenerateQrUiState(
                inputText = inputText,
                generatedQrBitmap = previewBitmap,
                isLoading = false
            )
        )
        fakeViewModel.onInputTextChanged(inputText)
        GenerateQrScreen(generateQrViewModel = fakeViewModel)
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, name = "Generate QR Screen - With Bitmap, Is Favorite")
@Composable
fun GenerateQrScreenWithBitmapIsFavoritePreview() {
    QRTheme {
        val context = LocalContext.current
        val fakeDao = remember { FakeGeneratedQrDao() }
        val fakeRepo = remember { FakeGeneratedQrRepository(context.applicationContext, fakeDao) }
        val fakeViewModel = remember { GenerateQrViewModel(fakeRepo) }

        val previewBitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).apply {
            val canvas = android.graphics.Canvas(this)
            val paint = android.graphics.Paint().apply { color = android.graphics.Color.DKGRAY; style = android.graphics.Paint.Style.FILL }
            canvas.drawRect(0f, 0f, 256f, 256f, paint)
            paint.color = android.graphics.Color.WHITE; paint.textSize = 30f; paint.textAlign = android.graphics.Paint.Align.CENTER
            canvas.drawText("IS FAV", 128f, 128f + paint.textSize / 3, paint)
        }
        val inputText = "preview_favorite_initial_is_fav" // Iný text pre iný stav

        fakeViewModel.setPreviewState(
            GenerateQrUiState(
                inputText = inputText,
                generatedQrBitmap = previewBitmap,
                isLoading = false
            )
        )
        // Simulácia, že pre tento text je entita obľúbená
        // FakeGeneratedQrDao by mala byť pripravená vrátiť entitu s isFavorite = true pre "preview_favorite_initial_is_fav"
        fakeViewModel.onInputTextChanged(inputText)

        GenerateQrScreen(generateQrViewModel = fakeViewModel)
    }
}
