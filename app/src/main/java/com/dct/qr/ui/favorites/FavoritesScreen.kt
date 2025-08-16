package com.dct.qr.ui.favorites


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dct.qr.R // Import vášho R súboru

// Alebo lepšie, odovzdajte len potrebné dáta ako parametre,
// napr. názov obrazovky, ak nepoužívate Screen sealed class priamo tu.
// import com.dct.qr.ui.components.PlaceholderScreenContent // Ak by ste mali zdieľaný Placeholder

@Composable
fun FavoritesScreen() {
    // Ak PlaceholderScreenContent je v inom súbore/balíčku, naimportujte ho
    // PlaceholderScreenContent(screenTitle = stringResource(id = R.string.screen_title_history))

    // Alebo priamo tu:
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Toto je obrazovka: ${stringResource(id = R.string.screen_title_setting)}",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
