package com.dct.qr.ui.generator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

@Composable
fun UrlQrForm(
    formData: UrlFormData,
    onUrlChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        OutlinedTextField(
            value = formData.url,
            onValueChange = onUrlChanged,
            label = { Text("URL adresa") },
            modifier = Modifier.fillMaxWidth(),
            isError = formData.urlError != null,
            supportingText = formData.urlError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true
        )
    }
}
