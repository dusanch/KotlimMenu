package com.dct.qr.ui.generator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dct.qr.data.type.QrCodeType

@Composable
fun QrTypeListScreen(
    availableTypes: List<QrCodeType>,
    onTypeSelected: (QrCodeType) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(availableTypes) { type ->
            // Implementujeme len pre tie, ktoré majú formulár (TEXT, URL)
            // Ostatné zobrazíme ako neaktívne, alebo ich vôbec nezobrazíme, kým nebudú funkčné
            val isEnabled = type == QrCodeType.TEXT || type == QrCodeType.URL ||
                    type == QrCodeType.WIFI || type == QrCodeType.EMAIL_MESSAGE // Pridajte ďalšie implementované typy

            Card( // Použijeme Card pre krajší vzhľad položky
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = isEnabled) { // Klikateľné len ak je enabled
                        if (isEnabled) {
                            onTypeSelected(type)
                        }
                    },
                elevation = CardDefaults.cardElevation(if (isEnabled) 2.dp else 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEnabled) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // Menej výrazné pre neaktívne
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = type.icon,
                        contentDescription = type.getDisplayName(), // Pre prístupnosť
                        modifier = Modifier.size(32.dp),
                        tint = if (isEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = type.getDisplayName(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
