package com.dct.qr.ui.generator // Alebo váš správny package

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dct.qr.R // Uistite sa, že tento import je správny

// Definície stringov, ktoré by ste mohli chcieť presunúť sem alebo nechať v QrGeneratorScreen.kt
// a len ich tu použiť. Ak ich necháte v strings.xml, tak je to v poriadku.
// napr. R.string.generated_qr_code_label, R.string.qr_code_image_desc, atď.

@Composable
fun QrDisplayScreen(
    qrBitmap: Bitmap?,
    qrContent: String?,
    isLoading: Boolean,
    error: String?,
    onSaveToFavorites: () -> Unit,
    onDownloadToGallery: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (qrBitmap != null) {
            Text(
                text = stringResource(R.string.generated_qr_code_label),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.qr_code_image_desc),
                modifier = Modifier
                    .size(250.dp)
                    .aspectRatio(1f)
                    .padding(vertical = 8.dp)
            )
            qrContent?.let { content ->
                Text(
                    text = stringResource(R.string.qr_code_content_is, content),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = {
                        onSaveToFavorites()
                        Toast.makeText(context, R.string.toast_saved_to_favorites_todo, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.FavoriteBorder, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.button_save_to_favorites))
                }
                Button(
                    onClick = {
                        onDownloadToGallery()
                        Toast.makeText(context, R.string.toast_download_to_gallery_todo, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.button_download_to_gallery))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_back_to_form))
            }
        } else if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_back_to_form))
            }
        } else {
            Text(stringResource(R.string.error_unknown_qr_display))
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_back_to_form))
            }
        }
    }
}

