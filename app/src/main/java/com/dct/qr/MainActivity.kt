package com.dct.qr // Nahraďte vaším skutočným balíčkom

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dct.qr.data.db.AppDatabase
import com.dct.qr.data.repository.HistoryRepository
import com.dct.qr.ui.history.HistoryScreen
import com.dct.qr.ui.history.HistoryViewModel
import com.dct.qr.ui.history.HistoryViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
// import com.dct.qr.data.repository.GeneratedQrRepository // Zatiaľ nepotrebujeme pre QrGeneratorViewModel
// ----- ZAČIATOK ÚPRAVY IMPORTOV PRE GENERATE QR -----
// Použijeme QrGeneratorScreen a QrGeneratorViewModel
import com.dct.qr.ui.generator.QrGeneratorScreen
// import com.dct.qr.ui.generator.QrGeneratorViewModel // viewModel() ho nájde, ak je v rovnakom module
// ----- KONIEC ÚPRAVY IMPORTOV PRE GENERATE QR -----
import com.dct.qr.ui.scan.ScanScreenWithAnalysis
import com.dct.qr.ui.favorites.FavoritesScreen
// import com.dct.qr.ui.generate.GenerateQrViewModelFactory // Zatiaľ nepotrebujeme
import com.dct.qr.ui.myqr.MyQRScreen
import com.dct.qr.ui.scanimage.ScanImageScreen
import com.dct.qr.ui.scanresult.ScanResultScreen
import com.dct.qr.ui.settings.SettingsScreen
import com.dct.qr.ui.theme.QRTheme
import kotlinx.coroutines.launch

// --- Definície Screen a NavigationItem ---
sealed class Screen(val route: String, val titleResId: Int, val icon: ImageVector? = null, val isAction: Boolean = false) {
    object Scan : Screen("scan", R.string.screen_title_scan, Icons.Filled.QrCodeScanner)
    object ScanImage : Screen("scan_image", R.string.screen_title_scan_image, Icons.Filled.ImageSearch)
    // Ponechávam CreateQR, ako ho máte, ale jeho obsah bude naša nová obrazovka
    object CreateQR : Screen("create_qr", R.string.screen_title_create_qr, Icons.Filled.AddCircleOutline)
    object MyQR : Screen("my_qr", R.string.screen_title_my_qr, Icons.Filled.QrCode2)
    object History : Screen("history", R.string.screen_title_history, Icons.Filled.History)
    object Favorites : Screen("favorites", R.string.screen_title_favorites, Icons.Filled.Favorite)
    object Settings : Screen("settings", R.string.screen_title_settings, Icons.Filled.Settings)
    object ShareApp : Screen("share_app", R.string.screen_title_share_app, Icons.Filled.Share, isAction = true)
    object RemoveAds : Screen("remove_ads", R.string.screen_title_remove_ads, Icons.Filled.Block, isAction = true)
    object OurApps : Screen("our_apps", R.string.screen_title_our_apps, Icons.Filled.Apps, isAction = true)
    object ScanResult : Screen("scan_result", R.string.screen_title_scan_result) // Používa sa pre titulok TopAppBar
}

val mainNavigationItems = listOf(
    Screen.Scan,
    Screen.ScanImage,
    Screen.CreateQR, // Toto je naša položka pre generátor
    Screen.MyQR,
    Screen.History,
    Screen.Favorites
)
val utilityNavigationItems = listOf(
    Screen.Settings,
    Screen.ShareApp,
    Screen.RemoveAds,
    Screen.OurApps
)
// --- Koniec definícií Screen a NavigationItem ---

typealias OnQrCodeScanned = (value: String, type: Int) -> Unit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QRTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainApp() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val database = remember { AppDatabase.getDatabase(context.applicationContext) }
    val historyRepository = remember { HistoryRepository(database.scannedCodeDao()) }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Scan) }
    var scannedQrValue by remember { mutableStateOf<String?>(null) }
    var scannedQrType by remember { mutableStateOf<Int?>(null) }

    val onQrScannedCallback: OnQrCodeScanned = { value, type ->
        Log.d("MainApp", "QR kód naskenovaný: Hodnota: '$value', Typ: $type")
        scannedQrValue = value
        scannedQrType = type
        scope.launch {
            try {
                historyRepository.insertScannedCode(value, type)
                Log.i("MainApp", "Naskenovaný kód '$value' uložený do databázy.")
            } catch (e: Exception) {
                Log.e("MainApp", "Chyba pri ukladaní kódu do databázy: ${e.message}", e)
            }
        }
        // Po naskenovaní a uložení sa chceme vrátiť na obrazovku skenovania (alebo kamkoľvek, odkiaľ sme prišli)
        // a zobraziť ScanResultScreen. Titulok sa upraví automaticky.
        // currentScreen zostáva, logika v Boxe nižšie to vyrieši.
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                mainNavigationItems.forEach { screen ->
                    NavigationDrawerItem(
                        icon = { screen.icon?.let { Icon(it, contentDescription = stringResource(screen.titleResId)) } },
                        label = { Text(stringResource(screen.titleResId)) },
                        selected = screen == currentScreen && scannedQrValue == null, // Iba ak nie sme v ScanResult
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (currentScreen != screen || scannedQrValue != null) {
                                scannedQrValue = null // Vždy resetujeme výsledok skenovania pri zmene hlavnej obrazovky
                                scannedQrType = null
                                currentScreen = screen
                                Log.d("MainApp", "Navigácia na: ${screen.route}")
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                utilityNavigationItems.forEach { screen ->
                    NavigationDrawerItem(
                        icon = { screen.icon?.let { Icon(it, contentDescription = stringResource(screen.titleResId)) } },
                        label = { Text(stringResource(screen.titleResId)) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            Log.d("MainApp", "Kliknuté na utility item: ${context.getString(screen.titleResId)}")
                            if (screen == Screen.Settings) {
                                scannedQrValue = null
                                scannedQrType = null
                                currentScreen = screen
                            } else if (screen.isAction) {
                                // TODO: Spracovať akcie pre utility itemy
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                AppTopBar(
                    // Ak je scannedQrValue, titulok je ScanResult, inak titulok aktuálnej obrazovky
                    currentScreenTitle = if (scannedQrValue != null) stringResource(Screen.ScanResult.titleResId)
                    else stringResource(currentScreen.titleResId),
                    onNavigationIconClick = {
                        scope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    },
                    showBackButton = scannedQrValue != null, // Zobraziť šípku späť len pre ScanResultScreen
                    onBackButtonClick = {
                        // Šípka späť vracia z ScanResultScreen na predchádzajúcu obrazovku (currentScreen)
                        Log.d("MainApp", "Šípka späť kliknutá na ScanResultScreen, vraciam sa na ${currentScreen.route}")
                        scannedQrValue = null
                        scannedQrType = null
                        // Nie je potrebné meniť currentScreen, zostávame na nej
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                if (scannedQrValue != null && scannedQrType != null) {
                    // Ak máme naskenovanú hodnotu, zobrazíme ScanResultScreen
                    Log.d("MainApp", "Zobrazujem ScanResultScreen")
                    ScanResultScreen(
                        scannedValue = scannedQrValue!!,
                        barcodeType = scannedQrType!!, // Pozor, ScanResultScreen môže očakávať iný typ (napr. Barcode.FORMAT_*)
                        onNavigateBack = {
                            Log.d("MainApp", "Navigácia späť z ScanResultScreen (cez tlačidlo v obsahu)")
                            scannedQrValue = null
                            scannedQrType = null
                        }
                    )
                } else {
                    // Inak zobrazíme obsah podľa currentScreen
                    Log.d("MainApp", "Zobrazujem CurrentScreenContent pre: ${currentScreen.route}")
                    CurrentScreenContent(
                        screen = currentScreen,
                        onQrCodeScanned = onQrScannedCallback,
                        historyRepository = historyRepository
                        // QrGeneratorViewModel bude vytvorený priamo v CurrentScreenContent
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    currentScreenTitle: String,
    onNavigationIconClick: () -> Unit,
    showBackButton: Boolean = false,
    onBackButtonClick: (() -> Unit)? = null // Nulovateľné, ak sa šípka späť nezobrazuje
) {
    TopAppBar(
        title = { Text(currentScreenTitle) },
        navigationIcon = {
            if (showBackButton && onBackButtonClick != null) {
                IconButton(onClick = onBackButtonClick) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            } else {
                IconButton(onClick = onNavigationIconClick) {
                    Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.action_open_drawer))
                }
            }
        }
    )
}

@Composable
fun CurrentScreenContent(
    screen: Screen,
    onQrCodeScanned: OnQrCodeScanned, // onQrCodeScanned by mal byť vždy dostupný pre ScanScreen
    historyRepository: HistoryRepository
) {
    Log.d("CurrentScreenContent", "Zobrazujem obrazovku: ${screen.route}")
    when (screen) {
        is Screen.Scan -> {
            ScanScreenWithAnalysis(onQrCodeScanned = onQrCodeScanned)
        }
        is Screen.ScanImage -> ScanImageScreen()
        // ----- ZAČIATOK ÚPRAVY PRE GENERATE QR -----
        is Screen.CreateQR -> {
            // QrGeneratorViewModel bude vytvorený vo vnútri QrGeneratorScreen pomocou viewModel()
            // Nevyžaduje zatiaľ Factory, ak nemá závislosti v konštruktore
            QrGeneratorScreen() // Priamo voláme našu novú obrazovku
        }
        // ----- KONIEC ÚPRAVY PRE GENERATE QR -----
        is Screen.MyQR -> MyQRScreen()
        is Screen.History -> {
            val historyViewModel: HistoryViewModel = viewModel(
                factory = HistoryViewModelFactory(historyRepository)
            )
            HistoryScreen(historyViewModel = historyViewModel)
        }
        is Screen.Favorites -> FavoritesScreen()
        is Screen.Settings -> SettingsScreen()
        // Screen.ScanResult sa rieši v MainApp, tu by nemal nastať
        // Akcie (ShareApp, RemoveAds, OurApps) sa riešia v onClick v drawer, tu nezobrazujú obsah
        else -> {
            // Pre ostatné prípady, ktoré by tu nemali byť alebo sú akcie
            if (!screen.isAction && screen !is Screen.ScanResult) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Obsah pre '${stringResource(id = screen.titleResId)}' nie je definovaný v CurrentScreenContent.",
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    QRTheme {
        MainApp()
    }
}
