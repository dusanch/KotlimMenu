package com.dct.qr

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CreditCardOff
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dct.qr.ui.ScanScreen
import com.dct.qr.ui.createqr.CreateQRScreen
import com.dct.qr.ui.favorites.FavoritesScreen
import com.dct.qr.ui.history.HistoryScreen
import com.dct.qr.ui.myqr.MyQRScreen
import com.dct.qr.ui.scanimage.ScanImageScreen
import com.dct.qr.ui.settings.SettingsScreen
import com.dct.qr.ui.theme.QRTheme
import kotlinx.coroutines.launch

// Váš kód pre Screen, drawerScreens, drawerActions a Composable funkcie
// môže byť tu, na najvyššej úrovni, mimo triedy MainActivity.

// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// TOTO JE VAŠA HLAVNÁ AKTIVITA
// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QRTheme { // <<--- APLIKUJTE VAŠU TÉMU
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp() // Zavoláte vašu hlavnú Composable funkciu
                }
            }
        }
    }
}
// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// KONIEC MainActivity triedy
// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++


// Všetky nasledujúce definície sú na najvyššej úrovni súboru,
// nie sú súčasťou žiadnej triedy, čo je v Kotline v poriadku.

sealed class Screen(val route: String, val titleResId: Int, val icon: ImageVector) {
    object Scan : Screen("scan", R.string.screen_title_scan, Icons.Filled.QrCodeScanner)
    object ScanImage : Screen("scan_image", R.string.screen_title_scan_image, Icons.Filled.ImageSearch)
    object Favorites : Screen("favorites", R.string.screen_title_favorites, Icons.Filled.Favorite)
    object History : Screen("history", R.string.screen_title_history, Icons.Filled.History)
    object MyQR : Screen("my_qr", R.string.screen_title_my_qr, Icons.Outlined.QrCode2)
    object CreateQR : Screen("create_qr", R.string.screen_title_create_qr, Icons.Filled.AddCircle)
    object Settings : Screen("settings", R.string.screen_title_settings, Icons.Filled.Settings)

    object ShareApp : Screen("action_share", R.string.drawer_share_app, Icons.AutoMirrored.Outlined.Send)
    object OurApps : Screen("action_our_apps", R.string.drawer_our_apps, Icons.AutoMirrored.Outlined.ArrowForward)
    object RemoveAds : Screen("action_remove_ads", R.string.drawer_remove_ads, Icons.Outlined.CreditCardOff)
}

val drawerScreens = listOf(
    Screen.Scan,
    Screen.ScanImage,
    Screen.Favorites,
    Screen.History,
    Screen.MyQR,
    Screen.CreateQR,
    Screen.Settings
)

val drawerActions = listOf(
    Screen.ShareApp,
    Screen.OurApps,
    Screen.RemoveAds
)


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainApp() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Scan) }

    val fabAction: () -> Unit = {
        currentScreen = Screen.Scan
        println("FAB clicked - Go to Scan Screen or Start Scanning")
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                currentRoute = currentScreen.route,
                onNavigate = { screen ->
                    scope.launch { drawerState.close() }
                    when (screen) {
                        is Screen.ShareApp -> shareApp(context)
                        is Screen.OurApps -> openOurApps(context)
                        is Screen.RemoveAds -> openRemoveAds(context)
                        else -> {
                            currentScreen = screen
                        }
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = stringResource(id = currentScreen.titleResId),
                    onNavigationIconClick = {
                        scope.launch { drawerState.open() }
                    },
                    showBackButton = currentScreen != Screen.Scan,
                    onBackClick = {
                        currentScreen = Screen.Scan
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = fabAction) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = stringResource(R.string.cd_scan_qr)
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.Center,
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                CurrentScreenContent(screen = currentScreen)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onNavigationIconClick: () -> Unit,
    showBackButton: Boolean,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back) // TODO: Vytvorte R.string.cd_back
                    )
                }
            } else {
                IconButton(onClick = onNavigationIconClick) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = stringResource(R.string.cd_open_menu)
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerContent(
    currentRoute: String,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(modifier) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(horizontal = 28.dp, vertical = 24.dp)
        )

        drawerScreens.forEach { screen ->
            NavigationDrawerItem(
                icon = { Icon(screen.icon, contentDescription = null) },
                label = { Text(stringResource(id = screen.titleResId)) },
                selected = currentRoute == screen.route,
                onClick = { onNavigate(screen) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        drawerActions.forEach { actionScreen ->
            NavigationDrawerItem(
                icon = { Icon(actionScreen.icon, contentDescription = null) },
                label = { Text(stringResource(id = actionScreen.titleResId)) },
                selected = false,
                onClick = { onNavigate(actionScreen) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}

@Composable
fun CurrentScreenContent(screen: Screen) {
    when (screen) {
        is Screen.Scan -> ScanScreen()
        is Screen.ScanImage -> ScanImageScreen()
        is Screen.Favorites -> FavoritesScreen()
        is Screen.History -> HistoryScreen()
        is Screen.MyQR -> MyQRScreen()
        is Screen.CreateQR -> CreateQRScreen()
        is Screen.Settings -> SettingsScreen()
        else -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Neznáma alebo akciová obrazovka: ${screen.route}")
            }
        }
    }
}

private fun shareApp(context: Context) {
    val appPackageName = context.packageName
    val appLink = "https://play.google.com/store/apps/details?id=$appPackageName"
    val shareText = context.getString(R.string.share_app_text, appLink)
     // Uistite sa, že R.string.share_app_text je definovaný ako:
    // <string name="share_app_text">Ahoj! Vyskúšaj túto skvelú aplikáciu na QR kódy: %1$s</string>


    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_app_via))) // TODO: Vytvorte R.string.share_app_via
}

private fun openOurApps(context: Context) {
    val developerId = "YOUR_DEVELOPER_ID" // TODO: Nahraďte vaším Google Play Developer ID
    if (developerId == "YOUR_DEVELOPER_ID" || developerId.isBlank()) {
        println("TODO: Nastavte správne Developer ID pre 'Naše aplikácie'")
        return
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://developer?id=$developerId")))
    } catch (anfe: android.content.ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=$developerId")))
    }
}

private fun openRemoveAds(context: Context) {
    val removeAdsUrl = "YOUR_REMOVE_ADS_URL" // TODO: Nahraďte URL vašej platenej verzie alebo in-app nákupu
    if (removeAdsUrl == "YOUR_REMOVE_ADS_URL" || removeAdsUrl.isBlank()) {
        println("TODO: Nastavte správnu URL pre 'Odstrániť reklamy'")
        return
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(removeAdsUrl)))
    } catch (anfe: android.content.ActivityNotFoundException) {
        println("Chyba pri otváraní odkazu na odstránenie reklám: $anfe")
    }
}

@Preview(showBackground = true, name = "Hlavná aplikácia - Skenovanie")
@Composable
fun DefaultAppPreview() {
    QRTheme { // <<--- APLIKUJTE VAŠU TÉMU AJ V PREVIEW
        MainApp()
    }
}
