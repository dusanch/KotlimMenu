package com.dct.qr.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dct.qr.R // Import vášho R súboru
import com.dct.qr.ui.theme.QRTheme // Import vašej témy

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    // Stavy pre prepínače (v reálnej aplikácii by ste ich ukladali do SharedPreferences alebo DataStore)
    var enableNotifications by remember { mutableStateOf(true) }
    var enableVibration by remember { mutableStateOf(false) }
    var useDarkTheme by remember { mutableStateOf(false) } // Toto by reálne menilo tému aplikácie

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // Umožní skrolovanie, ak je obsahu viac
            .padding(16.dp)
    ) {
        SettingsSectionTitle(title = stringResource(R.string.settings_section_general))

        SettingsToggleItem(
            icon = Icons.Filled.Notifications,
            title = stringResource(R.string.settings_enable_notifications),
            subtitle = stringResource(R.string.settings_enable_notifications_desc),
            checked = enableNotifications,
            onCheckedChange = { enableNotifications = it }
        )

        SettingsToggleItem(
            icon = Icons.Filled.Vibration,
            title = stringResource(R.string.settings_enable_vibration),
            subtitle = stringResource(R.string.settings_enable_vibration_desc),
            checked = enableVibration,
            onCheckedChange = { enableVibration = it }
        )

        SettingsItemDivider()

        SettingsSectionTitle(title = stringResource(R.string.settings_section_appearance))

        SettingsToggleItem(
            icon = Icons.Filled.ColorLens,
            title = stringResource(R.string.settings_dark_theme),
            subtitle = stringResource(R.string.settings_dark_theme_desc),
            checked = useDarkTheme,
            onCheckedChange = {
                useDarkTheme = it
                // TODO: V reálnej aplikácii by toto spustilo logiku na zmenu témy aplikácie
                // napr. aktualizácia stavu v AppViewModel, ktorý ovplyvňuje tému
            }
        )

        SettingsItemDivider()

        SettingsSectionTitle(title = stringResource(R.string.settings_section_other))

        SettingsNavigationItem(
            icon = Icons.Filled.Shield,
            title = stringResource(R.string.settings_privacy_policy),
            onClick = {
                // TODO: Otvoriť obrazovku/web stránku s pravidlami ochrany súkromia
                println("Privacy Policy clicked")
            }
        )

        SettingsNavigationItem(
            icon = Icons.Filled.Info,
            title = stringResource(R.string.settings_about_app),
            subtitle = "Verzia aplikácie: 1.0.0 (demo)", // TODO: Načítať dynamicky
            onClick = {
                // TODO: Otvoriť obrazovku "O aplikácii"
                println("About App clicked")
            }
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp), // Znížený vertikálny padding môže pomôcť, ak je problémom výška
            verticalAlignment = Alignment.CenterVertically,
            // horizontalArrangement = Arrangement.SpaceBetween // Túto časť dočasne odstránime alebo upravíme
        ) {
            // Kontajner pre ikonu a texty
            Row(
                modifier = Modifier.weight(1f), // Tento Row zaberie všetok dostupný priestor PO Switchi
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(16.dp))
                Column { // Bez weight tu, nech sa prispôsobí obsahu
                    Text(text = title, style = MaterialTheme.typography.bodyLarge)
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp)) // Malý odstup medzi titulkom a podtitulkom
                        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Switch umiestnený na konci s explicitným paddingom, ak je to potrebné
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.padding(start = 8.dp) // Zaistí odstup od textovej časti
            )
        }
    }
}

@Composable
fun SettingsNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp), // Väčší padding pre lepšie klikanie
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null, // Dekorácia
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}


// --- String Resources (pridajte do strings.xml) ---
/*
<resources>
    <!-- ... ostatné stringy ... -->
    <string name="settings_section_general">Všeobecné</string>
    <string name="settings_enable_notifications">Povoliť upozornenia</string>
    <string name="settings_enable_notifications_desc">Zobrazovať upozornenia aplikácie</string>
    <string name="settings_enable_vibration">Povoliť vibrácie</string>
    <string name="settings_enable_vibration_desc">Vibrovať pri udalostiach</string>

    <string name="settings_section_appearance">Vzhľad</string>
    <string name="settings_dark_theme">Tmavý režim</string>
    <string name="settings_dark_theme_desc">Použiť tmavú tému aplikácie</string>

    <string name="settings_section_other">Ostatné</string>
    <string name="settings_privacy_policy">Pravidlá ochrany osobných údajov</string>
    <string name="settings_about_app">O aplikácii</string>
</resources>
*/

// --- Preview ---
@Preview(showBackground = true, name = "Settings Screen")
@Composable
fun SettingsScreenPreview() {
    QRTheme { // Použite vašu tému
        SettingsScreen()
    }
}

@Preview(showBackground = true, name = "Settings Screen Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsScreenDarkPreview() {
    QRTheme { // Použite vašu tému
        SettingsScreen()
    }
}
