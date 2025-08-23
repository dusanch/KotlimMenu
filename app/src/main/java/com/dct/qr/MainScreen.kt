package com.dct.qr

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu // Ikona pre menu
import androidx.compose.material.icons.filled.QrCodeScanner // Ikona pre FAB
import androidx.compose.material.icons.outlined.History // Ikona pre históriu
import androidx.compose.material.icons.outlined.Info // Ikona pre o aplikácii
import androidx.compose.material.icons.outlined.Settings // Ikona pre nastavenia
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch



// Dátová trieda pre položku v navigačnom menu
data class DrawerItem(
    val icon: ImageVector,
    val label: String,
    val route: String // Pre navigáciu (ak používate Navigation Component)
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Zoznam položiek pre navigačné menu
    val drawerItems = listOf(
        DrawerItem(Icons.Outlined.History, stringResource(R.string.drawer_history), "history_screen"),
        DrawerItem(Icons.Outlined.Settings, stringResource(R.string.drawer_settings), "settings_screen"),
        DrawerItem(Icons.Outlined.Info, stringResource(R.string.drawer_about), "about_screen")
    )

    var selectedItemIndex by remember { mutableStateOf(0) } // Ktorá položka menu je aktívna

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                drawerItems = drawerItems,
                selectedItemIndex = selectedItemIndex,
                onItemClick = { index ->
                    selectedItemIndex = index
                    scope.launch {
                        drawerState.close()
                    }
                    // Tu by ste pridali logiku pre navigáciu na základe drawerItems[index].route
                    // napr. navController.navigate(drawerItems[index].route)
                    println("Navigating to: ${drawerItems[index].label}")
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name_qr_scanner)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = stringResource(R.string.cd_open_menu) // Content Description pre prístupnosť
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    // Akcia pre spustenie skenovania
                    println("FAB clicked - Start scanning")
                }) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = stringResource(R.string.cd_scan_qr)
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.Center, // Alebo FabPosition.End
        ) { paddingValues ->
            // Hlavný obsah aplikácie (napr. náhľad kamery)
            MainContent(modifier = Modifier.padding(paddingValues))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerContent(
    drawerItems: List<DrawerItem>,
    selectedItemIndex: Int,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(modifier) {
        // Logo alebo názov aplikácie v hornej časti menu (voliteľné)
        Text(
            text = stringResource(R.string.app_name_qr_scanner),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(16.dp)
        )
        HorizontalDivider() // Oddeľovač
        Spacer(Modifier.height(12.dp))

        drawerItems.forEachIndexed { index, item ->
            NavigationDrawerItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = index == selectedItemIndex,
                onClick = { onItemClick(index) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}

@Composable
fun MainContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Tu bude zobrazený náhľad kamery pre skenovanie QR kódu
        // a prípadne výsledky po naskenovaní.
        // Pre jednoduchosť zatiaľ len text.
        Text(
            text = stringResource(R.string.camera_preview_placeholder),
            style = MaterialTheme.typography.headlineMedium
        )
        // Príklad zobrazenia naskenovaného textu
         val scannedText by remember { mutableStateOf<String?>(null) }
         if (scannedText != null) {
             Text("Naskenované: $scannedText", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 70.dp))
         }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview2() {
    MaterialTheme { // Uistite sa, že máte nastavenú Material3 tému
        MainScreen()
    }
}
