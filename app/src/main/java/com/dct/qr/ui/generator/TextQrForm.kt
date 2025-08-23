package com.dct.qr.ui.generator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TextQrForm(
    formData: TextFormData,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        OutlinedTextField(
            value = formData.text,
            onValueChange = onTextChanged,
            label = { Text("Textový obsah") },
            modifier = Modifier.fillMaxWidth(),
            isError = formData.textError != null,
            supportingText = formData.textError?.let { { Text(it) } },
            minLines = 3
        )
    }
}