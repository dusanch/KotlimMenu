package com.dct.qr.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// POUŽITIE NOVÝCH MODRÝCH FARIEB PRE SVETLÝ REŽIM
private val LightBlueColorScheme = lightColorScheme(
    primary = BluePrimaryLight,
    onPrimary = BlueOnPrimaryLight,
    primaryContainer = BluePrimaryContainerLight,
    onPrimaryContainer = BlueOnPrimaryContainerLight,
    secondary = BlueSecondaryLight,
    onSecondary = BlueOnSecondaryLight,
    secondaryContainer = BlueSecondaryContainerLight,
    onSecondaryContainer = BlueOnSecondaryContainerLight,
    tertiary = BlueTertiaryLight,
    onTertiary = BlueOnTertiaryLight,
    tertiaryContainer = BlueTertiaryContainerLight,
    onTertiaryContainer = BlueOnTertiaryContainerLight,
    error = BlueErrorLight,
    onError = BlueOnErrorLight,
    errorContainer = BlueErrorContainerLight,
    onErrorContainer = BlueOnErrorContainerLight,
    background = BlueBackgroundLight,
    onBackground = BlueOnBackgroundLight,
    surface = BlueSurfaceLight,
    onSurface = BlueOnSurfaceLight,
    surfaceVariant = BlueSurfaceVariantLight,
    onSurfaceVariant = BlueOnSurfaceVariantLight,
    outline = BlueOutlineLight
)

// POUŽITIE NOVÝCH MODRÝCH FARIEB PRE TMAVÝ REŽIM
private val DarkBlueColorScheme = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = BlueOnPrimaryDark,
    primaryContainer = BluePrimaryContainerDark,
    onPrimaryContainer = BlueOnPrimaryContainerDark,
    secondary = BlueSecondaryDark,
    onSecondary = BlueOnSecondaryDark,
    secondaryContainer = BlueSecondaryContainerDark,
    onSecondaryContainer = BlueOnSecondaryContainerDark,
    tertiary = BlueTertiaryDark,
    onTertiary = BlueOnTertiaryDark,
    tertiaryContainer = BlueTertiaryContainerDark,
    onTertiaryContainer = BlueOnTertiaryContainerDark,
    error = BlueErrorDark,
    onError = BlueOnErrorDark,
    errorContainer = BlueErrorContainerDark,
    onErrorContainer = BlueOnErrorContainerDark,
    background = BlueBackgroundDark,
    onBackground = BlueOnBackgroundDark,
    surface = BlueSurfaceDark,
    onSurface = BlueOnSurfaceDark,
    surfaceVariant = BlueSurfaceVariantDark,
    onSurfaceVariant = BlueOnSurfaceVariantDark,
    outline = BlueOutlineDark
)

@Composable
fun QRTheme( // Alebo akokoľvek sa volá vaša hlavná téma
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color je dostupná na Androide 12+
    // Ak chcete striktne použiť vašu modrú tému a nie dynamické farby z tapety, nastavte dynamicColor na false
    dynamicColor: Boolean = false, // <--- ZMEŇTE NA false, AK CHCETE VŽDY SVOJU MODRÚ
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Použije vašu novú modrú schému
        darkTheme -> DarkBlueColorScheme
        else -> LightBlueColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb() // Môžete upraviť na colorScheme.surface napr.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme // Alebo podľa potreby
            // Pre navigačný panel (ak ho chcete zafarbiť)
            // window.navigationBarColor = colorScheme.surface.toArgb()
            // WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Vaša definícia typografie
        shapes = AppShapes, // Vaše definície tvarov (ak máte)
        content = content
    )
}

