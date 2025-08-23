package com.dct.qr.ui.generator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dct.qr.data.type.QrCodeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrTypeSelector(
    selectedType: QrCodeType,
    availableTypes: List<QrCodeType>,
    onTypeSelected: (QrCodeType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedType.getDisplayName(), // Použijeme našu @Composable funkciu z enumu
            onValueChange = {}, // Nemusí sa meniť priamo tu
            readOnly = true,
            label = { Text("Typ QR kódu") },
            leadingIcon = { Icon(selectedType.icon, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor() // Dôležité pre správne umiestnenie menu
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableTypes.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(type.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(text = type.getDisplayName())
                        }
                    },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

// Composable funkcia pre zobrazenie názvu typu (musí byť @Composable kvôli stringResource)
@Composable
fun QrCodeType.getDisplayName(): String {
    return stringResource(id = this.displayNameResourceId)
}
