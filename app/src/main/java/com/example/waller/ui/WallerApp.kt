/**
 * Root-level Compose setup for Waller.
 * Handles:
 * - App-wide theme (light / dark / system), persisted with SharedPreferences
 * - Gradient background toggle, persisted
 * - Wallpaper defaults (orientation, gradient count, effects), persisted
 * - Bottom navigation between Home (wallpapers), Settings and About screens
 * - Back button behavior: About -> Settings -> Home -> Exit
 */

package com.example.waller.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.waller.R
import com.example.waller.ui.settings.AboutScreen
import com.example.waller.ui.settings.AppThemeMode
import com.example.waller.ui.settings.DefaultOrientation
import com.example.waller.ui.settings.SettingsScreen
import com.example.waller.ui.theme.WallerTheme
import com.example.waller.ui.wallpaper.WallpaperGeneratorScreen

// Which top-level screen is shown.
private enum class RootScreen { HOME, SETTINGS, ABOUT }

@Composable
fun WallerApp() {
    val systemIsDark = isSystemInDarkTheme()
    val context = LocalContext.current

    // --- SharedPreferences handle ---
    val prefs = remember {
        context.getSharedPreferences("waller_prefs", Context.MODE_PRIVATE)
    }

    // --- PERSISTED THEME ---
    val initialThemeMode = remember {
        when (prefs.getString("theme_mode", AppThemeMode.SYSTEM.name)) {
            AppThemeMode.LIGHT.name -> AppThemeMode.LIGHT
            AppThemeMode.DARK.name -> AppThemeMode.DARK
            else -> AppThemeMode.SYSTEM
        }
    }
    var appThemeMode by remember { mutableStateOf(initialThemeMode) }
    fun updateThemeMode(mode: AppThemeMode) {
        appThemeMode = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    // --- PERSISTED GRADIENT BACKGROUND ---
    val initialGradientBg = remember {
        prefs.getBoolean("use_gradient_bg", true)
    }
    var useGradientBackground by remember { mutableStateOf(initialGradientBg) }
    fun updateUseGradientBackground(value: Boolean) {
        useGradientBackground = value
        prefs.edit().putBoolean("use_gradient_bg", value).apply()
    }

    // --- PERSISTED WALLPAPER DEFAULTS ---

    // Orientation: AUTO / PORTRAIT / LANDSCAPE
    val initialOrientation = remember {
        when (prefs.getString("default_orientation", DefaultOrientation.AUTO.name)) {
            DefaultOrientation.PORTRAIT.name -> DefaultOrientation.PORTRAIT
            DefaultOrientation.LANDSCAPE.name -> DefaultOrientation.LANDSCAPE
            else -> DefaultOrientation.AUTO
        }
    }
    var defaultOrientation by remember { mutableStateOf(initialOrientation) }
    fun updateDefaultOrientation(value: DefaultOrientation) {
        defaultOrientation = value
        prefs.edit().putString("default_orientation", value.name).apply()
    }

    // Gradient count: 12, 16, 20
    val initialGradientCount = remember {
        val stored = prefs.getInt("default_gradient_count", 20)
        if (stored in listOf(12, 16, 20)) stored else 20
    }
    var defaultGradientCount by remember { mutableStateOf(initialGradientCount) }
    fun updateDefaultGradientCount(value: Int) {
        defaultGradientCount = value
        prefs.edit().putInt("default_gradient_count", value).apply()
    }

    // Default effects
    val initialNothing = remember {
        prefs.getBoolean("default_enable_nothing", false)
    }
    var enableNothingByDefault by remember { mutableStateOf(initialNothing) }
    fun updateEnableNothing(value: Boolean) {
        enableNothingByDefault = value
        prefs.edit().putBoolean("default_enable_nothing", value).apply()
    }

    val initialSnow = remember {
        prefs.getBoolean("default_enable_snow", false)
    }
    var enableSnowByDefault by remember { mutableStateOf(initialSnow) }
    fun updateEnableSnow(value: Boolean) {
        enableSnowByDefault = value
        prefs.edit().putBoolean("default_enable_snow", value).apply()
    }

    val initialStripes = remember {
        prefs.getBoolean("default_enable_stripes", false)
    }
    var enableStripesByDefault by remember { mutableStateOf(initialStripes) }
    fun updateEnableStripes(value: Boolean) {
        enableStripesByDefault = value
        prefs.edit().putBoolean("default_enable_stripes", value).apply()
    }

    // --- NAVIGATION STATE ---
    var currentScreen by remember { mutableStateOf(RootScreen.HOME) }

    val isDarkTheme = when (appThemeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> systemIsDark
    }

    // For bottom bar highlighting: About is grouped under Settings.
    val selectedForNav = when (currentScreen) {
        RootScreen.HOME -> RootScreen.HOME
        RootScreen.SETTINGS, RootScreen.ABOUT -> RootScreen.SETTINGS
    }

    // --- BACK BUTTON HANDLING ---
    // About -> Settings -> Home -> exit
    BackHandler(enabled = currentScreen != RootScreen.HOME) {
        currentScreen = when (currentScreen) {
            RootScreen.ABOUT -> RootScreen.SETTINGS
            RootScreen.SETTINGS -> RootScreen.HOME
            RootScreen.HOME -> RootScreen.HOME
        }
    }

    WallerTheme(darkTheme = isDarkTheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (useGradientBackground) {
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    }
                )
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                bottomBar = {
                    WallerBottomBar(
                        selectedScreen = selectedForNav,
                        onScreenSelected = { screen ->
                            currentScreen = screen
                        }
                    )
                }
            ) { innerPadding ->
                when (currentScreen) {
                    RootScreen.HOME -> {
                        WallpaperGeneratorScreen(
                            modifier = Modifier.padding(innerPadding),
                            isAppDarkMode = isDarkTheme,
                            onThemeChange = {
                                // Header icon toggles between LIGHT and DARK explicitly.
                                val next = when (appThemeMode) {
                                    AppThemeMode.LIGHT -> AppThemeMode.DARK
                                    AppThemeMode.DARK -> AppThemeMode.LIGHT
                                    AppThemeMode.SYSTEM ->
                                        if (systemIsDark) AppThemeMode.LIGHT
                                        else AppThemeMode.DARK
                                }
                                updateThemeMode(next)
                            },
                            defaultOrientation = defaultOrientation,
                            defaultGradientCount = defaultGradientCount,
                            defaultEnableNothing = enableNothingByDefault,
                            defaultEnableSnow = enableSnowByDefault,
                            defaultEnableStripes = enableStripesByDefault
                        )
                    }

                    RootScreen.SETTINGS -> {
                        SettingsScreen(
                            modifier = Modifier.padding(innerPadding),
                            appThemeMode = appThemeMode,
                            onAppThemeModeChange = { mode -> updateThemeMode(mode) },
                            useGradientBackground = useGradientBackground,
                            onUseGradientBackgroundChange = { updateUseGradientBackground(it) },

                            defaultOrientation = defaultOrientation,
                            onDefaultOrientationChange = { updateDefaultOrientation(it) },
                            defaultGradientCount = defaultGradientCount,
                            onDefaultGradientCountChange = { updateDefaultGradientCount(it) },
                            enableNothingByDefault = enableNothingByDefault,
                            onEnableNothingByDefaultChange = { updateEnableNothing(it) },
                            enableSnowByDefault = enableSnowByDefault,
                            onEnableSnowByDefaultChange = { updateEnableSnow(it) },
                            enableStripesByDefault = enableStripesByDefault,
                            onEnableStripesByDefaultChange = { updateEnableStripes(it) },
                            onAboutClick = { currentScreen = RootScreen.ABOUT }
                        )
                    }

                    RootScreen.ABOUT -> {
                        AboutScreen(
                            modifier = Modifier.padding(innerPadding),
                            onBackToSettings = { currentScreen = RootScreen.SETTINGS }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WallerBottomBar(
    selectedScreen: RootScreen,
    onScreenSelected: (RootScreen) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedScreen == RootScreen.HOME,
            onClick = { onScreenSelected(RootScreen.HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(text = stringResource(id = R.string.nav_home)) }
        )
        NavigationBarItem(
            selected = selectedScreen == RootScreen.SETTINGS,
            onClick = { onScreenSelected(RootScreen.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text(text = stringResource(id = R.string.nav_settings)) }
        )
    }
}
