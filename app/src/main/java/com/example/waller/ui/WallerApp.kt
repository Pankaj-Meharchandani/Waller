/**
 * Root-level Compose setup for Waller.
 * Handles:
 * - App-wide theme (light / dark / system), persisted
 * - Gradient background toggle, persisted
 * - Wallpaper defaults (orientation, gradient count, effects, tone, multicolor), persisted
 * - Shared favourite wallpapers (snapshot of wallpaper + effects), persisted
 * - Shared effect toggles (snow / stripes / glass) used by Home + Favourites
 * - Shared orientation state (portrait / landscape) for Home + Favourites
 * - Bottom navigation between Home, Favourites, Settings, About
 * - Back button behavior: About -> Settings -> Home/Favourites -> Exit
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
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.waller.ui.wallpaper.FavoriteWallpaper
import com.example.waller.ui.wallpaper.FavoritesScreen
import com.example.waller.ui.wallpaper.GradientType
import com.example.waller.ui.wallpaper.ToneMode
import com.example.waller.ui.wallpaper.Wallpaper
import com.example.waller.ui.wallpaper.WallpaperGeneratorScreen
import com.example.waller.ui.wallpaper.colorFromHexOrNull
import com.example.waller.ui.wallpaper.toHexString
import androidx.core.content.edit

// Which top-level screen is shown.
private enum class RootScreen { HOME, FAVOURITES, SETTINGS, ABOUT }

private const val FAVOURITES_KEY = "favourites_v1"

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
        prefs.edit { putString("theme_mode", mode.name) }
    }

    // --- PERSISTED GRADIENT BACKGROUND ---
    val initialGradientBg = remember {
        prefs.getBoolean("use_gradient_bg", true)
    }
    var useGradientBackground by remember { mutableStateOf(initialGradientBg) }
    fun updateUseGradientBackground(value: Boolean) {
        useGradientBackground = value
        prefs.edit { putBoolean("use_gradient_bg", value)}
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
        prefs.edit { putString("default_orientation", value.name) }
    }

    // Session orientation shared between Home + Favourites
    val initialSessionPortrait = remember {
        when (defaultOrientation) {
            DefaultOrientation.LANDSCAPE -> false
            DefaultOrientation.AUTO, DefaultOrientation.PORTRAIT -> true
        }
    }
    var sessionIsPortrait by remember { mutableStateOf(initialSessionPortrait) }

    // Gradient count: 12, 16, 20
    val initialGradientCount = remember {
        val stored = prefs.getInt("default_gradient_count", 20)
        if (stored in listOf(12, 16, 20)) stored else 20
    }
    var defaultGradientCount by remember { mutableIntStateOf(initialGradientCount) }
    fun updateDefaultGradientCount(value: Int) {
        defaultGradientCount = value
        prefs.edit { putInt("default_gradient_count", value) }
    }

    // Default effects (for Settings)
    val initialNothing = remember {
        prefs.getBoolean("default_enable_nothing", false)
    }
    var enableNothingByDefault by remember { mutableStateOf(initialNothing) }
    fun updateEnableNothing(value: Boolean) {
        enableNothingByDefault = value
        prefs.edit { putBoolean("default_enable_nothing", value) }
    }

    val initialSnow = remember {
        prefs.getBoolean("default_enable_snow", false)
    }
    var enableSnowByDefault by remember { mutableStateOf(initialSnow) }
    fun updateEnableSnow(value: Boolean) {
        enableSnowByDefault = value
        prefs.edit { putBoolean("default_enable_snow", value) }
    }

    val initialStripes = remember {
        prefs.getBoolean("default_enable_stripes", false)
    }
    var enableStripesByDefault by remember { mutableStateOf(initialStripes) }
    fun updateEnableStripes(value: Boolean) {
        enableStripesByDefault = value
        prefs.edit { putBoolean("default_enable_stripes", value) }
    }

    // Default tone: DARK / NEUTRAL / LIGHT
    val initialToneMode = remember {
        when (prefs.getString("default_tone_mode", ToneMode.LIGHT.name)) {
            ToneMode.DARK.name -> ToneMode.DARK
            ToneMode.NEUTRAL.name -> ToneMode.NEUTRAL
            else -> ToneMode.LIGHT
        }
    }
    var defaultToneMode by remember { mutableStateOf(initialToneMode) }
    fun updateDefaultToneMode(value: ToneMode) {
        defaultToneMode = value
        prefs.edit { putString("default_tone_mode", value.name) }
    }

    // Default multicolor: ON / OFF (off by default)
    val initialMulticolor = remember {
        prefs.getBoolean("default_enable_multicolor", false)
    }
    var enableMulticolorByDefault by remember { mutableStateOf(initialMulticolor) }
    fun updateEnableMulticolor(value: Boolean) {
        enableMulticolorByDefault = value
        prefs.edit { putBoolean("default_enable_multicolor", value) }
    }

    // --- SHARED EFFECT STATE (used by Home + as defaults when app starts) ---
    var snowEffectEnabled by remember { mutableStateOf(enableSnowByDefault) }
    var stripesEffectEnabled by remember { mutableStateOf(enableStripesByDefault) }
    var overlayEffectEnabled by remember { mutableStateOf(enableNothingByDefault) }

    // --- SHARED FAVOURITES (snapshot of wallpaper + effects), PERSISTED ---
    var favouriteWallpapers by remember {
        mutableStateOf(
            prefs.getString(FAVOURITES_KEY, null)
                ?.let { decodeFavourites(it) }
                ?: emptyList()
        )
    }

    fun persistFavourites() {
        prefs.edit {
            putString(FAVOURITES_KEY, encodeFavourites(favouriteWallpapers))
        }
    }

    // From Home screen: toggle by wallpaper; snapshot current effects when adding.
    fun toggleFavouriteFromHome(
        wallpaper: Wallpaper,
        addNoise: Boolean,
        addStripes: Boolean,
        addOverlay: Boolean
    ) {
        val existing = favouriteWallpapers.find { it.wallpaper == wallpaper }
        favouriteWallpapers =
            if (existing != null) {
                favouriteWallpapers - existing
            } else {
                favouriteWallpapers + FavoriteWallpaper(
                    wallpaper = wallpaper,
                    addNoise = addNoise,
                    addStripes = addStripes,
                    addOverlay = addOverlay
                )
            }
        persistFavourites()
    }

    // From Favourites screen: remove that exact favourite entry.
    fun removeFavourite(fav: FavoriteWallpaper) {
        favouriteWallpapers = favouriteWallpapers - fav
        persistFavourites()
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
        RootScreen.FAVOURITES -> RootScreen.FAVOURITES
        RootScreen.SETTINGS, RootScreen.ABOUT -> RootScreen.SETTINGS
    }

    // --- BACK BUTTON HANDLING ---
    BackHandler(enabled = currentScreen != RootScreen.HOME) {
        currentScreen = when (currentScreen) {
            RootScreen.ABOUT -> RootScreen.SETTINGS
            RootScreen.SETTINGS -> RootScreen.HOME
            RootScreen.FAVOURITES -> RootScreen.HOME
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
                                val next = when (appThemeMode) {
                                    AppThemeMode.LIGHT -> AppThemeMode.DARK
                                    AppThemeMode.DARK -> AppThemeMode.LIGHT
                                    AppThemeMode.SYSTEM ->
                                        if (systemIsDark) AppThemeMode.LIGHT
                                        else AppThemeMode.DARK
                                }
                                updateThemeMode(next)
                            },
                            defaultGradientCount = defaultGradientCount,
                            defaultEnableNothing = enableNothingByDefault,
                            defaultEnableSnow = enableSnowByDefault,
                            defaultEnableStripes = enableStripesByDefault,
                            defaultToneMode = defaultToneMode,
                            defaultEnableMulticolor = enableMulticolorByDefault,
                            addNoise = snowEffectEnabled,
                            onAddNoiseChange = { snowEffectEnabled = it },
                            addStripes = stripesEffectEnabled,
                            onAddStripesChange = { stripesEffectEnabled = it },
                            addOverlay = overlayEffectEnabled,
                            onAddOverlayChange = { overlayEffectEnabled = it },
                            favouriteWallpapers = favouriteWallpapers,
                            onToggleFavourite = { w, n, s, o ->
                                toggleFavouriteFromHome(w, n, s, o)
                            },
                            isPortrait = sessionIsPortrait,
                            onOrientationChange = { sessionIsPortrait = it }
                        )
                    }

                    RootScreen.FAVOURITES -> {
                        FavoritesScreen(
                            modifier = Modifier.padding(innerPadding),
                            isAppDarkMode = isDarkTheme,
                            onThemeChange = {
                                val next = when (appThemeMode) {
                                    AppThemeMode.LIGHT -> AppThemeMode.DARK
                                    AppThemeMode.DARK -> AppThemeMode.LIGHT
                                    AppThemeMode.SYSTEM ->
                                        if (systemIsDark) AppThemeMode.LIGHT
                                        else AppThemeMode.DARK
                                }
                                updateThemeMode(next)
                            },
                            favourites = favouriteWallpapers,
                            isPortrait = sessionIsPortrait,
                            onOrientationChange = { sessionIsPortrait = it },
                            onRemoveFavourite = { removeFavourite(it) }
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
                            defaultToneMode = defaultToneMode,
                            onDefaultToneModeChange = { updateDefaultToneMode(it) },
                            defaultEnableMulticolor = enableMulticolorByDefault,
                            onDefaultEnableMulticolorChange = { updateEnableMulticolor(it) },
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
            selected = selectedScreen == RootScreen.FAVOURITES,
            onClick = { onScreenSelected(RootScreen.FAVOURITES) },
            icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
            label = { Text(text = stringResource(id = R.string.nav_favourites)) }
        )

        NavigationBarItem(
            selected = selectedScreen == RootScreen.SETTINGS,
            onClick = { onScreenSelected(RootScreen.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text(text = stringResource(id = R.string.nav_settings)) }
        )
    }
}

/* --------------------------- Favourites encode/decode --------------------------- */

private fun encodeFavourites(list: List<FavoriteWallpaper>): String =
    list.joinToString(";") { fav ->
        val typeName = fav.wallpaper.type.name
        val colorsStr = fav.wallpaper.colors.joinToString(",") { it.toHexString() }
        val flagsStr = listOf(fav.addNoise, fav.addStripes, fav.addOverlay)
            .joinToString(",") { if (it) "1" else "0" }

        listOf(typeName, colorsStr, flagsStr).joinToString("|")
    }

private fun decodeFavourites(raw: String): List<FavoriteWallpaper> =
    raw.split(";")
        .mapNotNull { item ->
            if (item.isBlank()) return@mapNotNull null
            val parts = item.split("|")
            if (parts.size != 3) return@mapNotNull null

            val type = runCatching { GradientType.valueOf(parts[0]) }.getOrNull()
                ?: return@mapNotNull null

            val colors = parts[1]
                .split(",")
                .mapNotNull { token -> colorFromHexOrNull(token) }
                .takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null

            val flagTokens = parts[2].split(",")
            val addNoise = flagTokens.getOrNull(0) == "1"
            val addStripes = flagTokens.getOrNull(1) == "1"
            val addOverlay = flagTokens.getOrNull(2) == "1"

            FavoriteWallpaper(
                wallpaper = Wallpaper(colors = colors, type = type),
                addNoise = addNoise,
                addStripes = addStripes,
                addOverlay = addOverlay
            )
        }
