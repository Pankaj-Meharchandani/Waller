/**
 * WallerApp.kt
 *
 * Root application composable and global state holder for Waller.
 *
 * Responsibilities:
 * - Owns app-wide state (theme, interaction mode, orientation, defaults)
 * - Resolves wallpaper orientation (AUTO / PORTRAIT / LANDSCAPE) at session start
 * - Manages navigation between Home, Favourites, Settings, and About
 * - Persists and restores user preferences via SharedPreferences
 * - Handles one-time onboarding dialogs and update checks
 *
 * Adding a new effect requires NO change here — effect state is generic EffectMap.
 *
 * Encode/decode format (v2): type|hex1,hex2,...|angleInt|id:enabled:alpha,id:enabled:alpha,...
 * Legacy v1 format (5-flag, 5-alpha positions) is decoded for backward compatibility.
 */

package com.example.waller.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.view.Surface
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.example.waller.R
import com.example.waller.ui.marketplace.MarketplaceScreen
import com.example.waller.ui.onboarding.ModePickerDialog
import com.example.waller.ui.onboarding.UpdateAvailableDialog
import com.example.waller.ui.onboarding.UpdateChecker
import com.example.waller.ui.settings.AboutScreen
import com.example.waller.ui.settings.AppThemeMode
import com.example.waller.ui.settings.DefaultOrientation
import com.example.waller.ui.settings.FavoritesTab
import com.example.waller.ui.settings.SettingsScreen
import com.example.waller.ui.theme.WallerTheme
import com.example.waller.ui.wallfile.WallFileManager
import com.example.waller.ui.wallpaper.*
import com.example.waller.ui.wallpaper.components.FloatingNavBar
import com.example.waller.ui.wallpaper.components.FloatingNavItem
import java.util.Locale
import kotlin.math.roundToInt

private enum class RootScreen { HOME, FAVOURITES, MARKET, SETTINGS, ABOUT }

private const val FAVOURITES_KEY              = "favourites_v2"
private const val FAVOURITES_KEY_LEGACY       = "favourites_v1"
private const val HISTORY_KEY                 = "history_v1"
private const val PREF_KEY_DEFAULT_TAB        = "default_tab_v1"
private const val PREF_KEY_INTERACTION_MODE   = "interaction_mode_v1"
private const val PREF_KEY_LOCKED_ORIENTATION = "locked_orientation_v1"
private const val PREF_KEY_HAPTICS_ENABLED    = "haptics_enabled_v1"
private const val PREF_KEY_BETA_UPDATES       = "beta_updates_v1"
private const val PREF_KEY_MODE_PICKER_SHOWN_VERSION = "mode_picker_shown_version_v1"

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun WallerApp(openedWallUri: Uri? = null) {
    val systemIsDark = isSystemInDarkTheme()
    val context      = LocalContext.current
    val activity     = context as Activity

    val appVersion = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "-" }
        catch (_: Exception) { "-" }
    }

    val prefs = remember { context.getSharedPreferences("waller_prefs", Context.MODE_PRIVATE) }

    // ── Haptics ───────────────────────────────────────────────────────────────
    val hapticsState = remember { mutableStateOf(prefs.getBoolean(PREF_KEY_HAPTICS_ENABLED, true)) }
    var hapticsEnabled by hapticsState
    LaunchedEffect(Unit) { Haptics.enabled = hapticsEnabled }
    fun updateHapticsEnabled(value: Boolean) {
        hapticsState.value = value; Haptics.enabled = value
        prefs.edit { putBoolean(PREF_KEY_HAPTICS_ENABLED, value) }
    }

    // ── Beta Updates ─────────────────────────────────────────────────────────
    val betaUpdatesState = remember { mutableStateOf(prefs.getBoolean(PREF_KEY_BETA_UPDATES, false)) }
    var showBetaUpdates by betaUpdatesState
    fun updateBetaUpdates(value: Boolean) {
        betaUpdatesState.value = value
        prefs.edit { putBoolean(PREF_KEY_BETA_UPDATES, value) }
    }

    // ── Theme ─────────────────────────────────────────────────────────────────
    val appThemeModeState = remember {
        mutableStateOf(when (prefs.getString("theme_mode", AppThemeMode.SYSTEM.name)) {
            AppThemeMode.LIGHT.name -> AppThemeMode.LIGHT
            AppThemeMode.DARK.name  -> AppThemeMode.DARK
            else -> AppThemeMode.SYSTEM
        })
    }
    var appThemeMode by appThemeModeState
    fun updateThemeMode(mode: AppThemeMode) {
        appThemeModeState.value = mode; prefs.edit { putString("theme_mode", mode.name) }
    }

    // ── Gradient background ───────────────────────────────────────────────────
    val useGradientBgState = remember { mutableStateOf(prefs.getBoolean("use_gradient_bg", true)) }
    var useGradientBackground by useGradientBgState
    fun updateUseGradientBackground(value: Boolean) {
        useGradientBgState.value = value; prefs.edit { putBoolean("use_gradient_bg", value) }
    }

    // ── Interaction mode (Simple / Advanced) ──────────────────────────────────
    val interactionModeState = remember {
        mutableStateOf(
            if (prefs.getString(PREF_KEY_INTERACTION_MODE, InteractionMode.SIMPLE.name) == InteractionMode.ADVANCED.name)
                InteractionMode.ADVANCED else InteractionMode.SIMPLE
        )
    }
    var interactionMode by interactionModeState
    fun updateInteractionMode(mode: InteractionMode) {
        interactionModeState.value = mode
        prefs.edit { putString(PREF_KEY_INTERACTION_MODE, mode.name) }
        if (mode == InteractionMode.ADVANCED) {
            val savedLock = prefs.getInt(PREF_KEY_LOCKED_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
            val lockMode = if (savedLock != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) savedLock else {
                @Suppress("DEPRECATION")
                val rotation = try { activity.windowManager.defaultDisplay.rotation }
                catch (_: Exception) {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                        Surface.ROTATION_0 else Surface.ROTATION_90
                }
                val computed = when (rotation) {
                    Surface.ROTATION_0   -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    Surface.ROTATION_90  -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    else                 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                prefs.edit { putInt(PREF_KEY_LOCKED_ORIENTATION, computed) }; computed
            }
            activity.requestedOrientation = lockMode
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            prefs.edit { remove(PREF_KEY_LOCKED_ORIENTATION) }
        }
    }

    // ── Onboarding / update dialogs ───────────────────────────────────────────
    var showModePickerDialog by remember { mutableStateOf(false) }
    data class UpdateInfo(val version: String, val notes: String, val url: String)
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        UpdateChecker.check(
            currentVersion = appVersion,
            repoOwner = "Pankaj-Meharchandani",
            repoName = "Waller",
            includePreReleases = showBetaUpdates
        ) { latestVersion, releaseNotes, releaseUrl ->
            updateInfo = UpdateInfo(
                version = latestVersion,
                notes = releaseNotes,
                url = releaseUrl
            )
        }
    }

    val currentVersionCode: Int = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionCode
    } catch (_: Exception) { 1 }

    LaunchedEffect(Unit) {
        if (prefs.getInt(PREF_KEY_MODE_PICKER_SHOWN_VERSION, -1) != currentVersionCode)
            showModePickerDialog = true
    }

    // Reapply orientation lock on startup
    LaunchedEffect(Unit) {
        val savedModeName = prefs.getString(PREF_KEY_INTERACTION_MODE, InteractionMode.SIMPLE.name)
        val savedMode = if (savedModeName == InteractionMode.ADVANCED.name) InteractionMode.ADVANCED else InteractionMode.SIMPLE
        interactionModeState.value = savedMode
        if (savedMode == InteractionMode.ADVANCED) {
            val savedLock = prefs.getInt(PREF_KEY_LOCKED_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
            if (savedLock != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                activity.requestedOrientation = savedLock
            } else {
                @Suppress("DEPRECATION")
                val rotation = try { activity.windowManager.defaultDisplay.rotation }
                catch (_: Exception) {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                        Surface.ROTATION_0 else Surface.ROTATION_90
                }
                val computed = when (rotation) {
                    Surface.ROTATION_0   -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    Surface.ROTATION_90  -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    else                 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                activity.requestedOrientation = computed
                prefs.edit { putInt(PREF_KEY_LOCKED_ORIENTATION, computed) }
            }
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // ── Orientation ───────────────────────────────────────────────────────────
    var defaultOrientation by remember {
        mutableStateOf(when (prefs.getString("default_orientation", DefaultOrientation.AUTO.name)) {
            DefaultOrientation.PORTRAIT.name  -> DefaultOrientation.PORTRAIT
            DefaultOrientation.LANDSCAPE.name -> DefaultOrientation.LANDSCAPE
            else -> DefaultOrientation.AUTO
        })
    }
    fun updateDefaultOrientation(value: DefaultOrientation) {
        defaultOrientation = value; prefs.edit { putString("default_orientation", value.name) }
    }

    // ── Default tab ───────────────────────────────────────────────────────────
    var defaultTab by remember {
        mutableStateOf(when (prefs.getString(PREF_KEY_DEFAULT_TAB, FavoritesTab.FAVOURITES.name)) {
            FavoritesTab.HISTORY.name -> FavoritesTab.HISTORY
            else -> FavoritesTab.FAVOURITES
        })
    }
    fun updateDefaultTab(value: FavoritesTab) {
        defaultTab = value; prefs.edit { putString(PREF_KEY_DEFAULT_TAB, value.name) }
    }

    val configuration = LocalConfiguration.current
    val resolvedIsPortrait = remember(defaultOrientation, configuration) {
        when (defaultOrientation) {
            DefaultOrientation.PORTRAIT  -> true
            DefaultOrientation.LANDSCAPE -> false
            DefaultOrientation.AUTO      -> configuration.smallestScreenWidthDp < 600
        }
    }
    val sessionIsPortraitState = remember { mutableStateOf(resolvedIsPortrait) }
    var sessionIsPortrait by sessionIsPortraitState

    // ── Gradient count ────────────────────────────────────────────────────────
    val initialGradientCount = remember {
        val s = prefs.getInt("default_gradient_count", 20)
        if (s in listOf(12, 16, 20)) s else 20
    }
    val defaultGradientCountState = remember { mutableIntStateOf(initialGradientCount) }
    var defaultGradientCount by defaultGradientCountState
    fun updateDefaultGradientCount(value: Int) {
        defaultGradientCountState.intValue = value; prefs.edit { putInt("default_gradient_count", value) }
    }

    // ── Default effects (for Settings screen toggles) ─────────────────────────
    // Use explicit MutableState so local functions can write .value without
    // the "captured var delegate" compiler restriction.
    val enableNothingState   = remember { mutableStateOf(prefs.getBoolean("default_enable_nothing", false)) }
    val enableSnowState      = remember { mutableStateOf(prefs.getBoolean("default_enable_snow", false)) }
    val enableStripesState   = remember { mutableStateOf(prefs.getBoolean("default_enable_stripes", false)) }
    val enableGeometricState = remember { mutableStateOf(prefs.getBoolean("default_enable_geometric", false)) }
    val enableBlurState      = remember { mutableStateOf(prefs.getBoolean("default_enable_blur", false)) }
    var enableNothingByDefault   by enableNothingState
    var enableSnowByDefault      by enableSnowState
    var enableStripesByDefault   by enableStripesState
    var enableGeometricByDefault by enableGeometricState
    var enableBlurByDefault      by enableBlurState

    // ── Tone / multicolor defaults ────────────────────────────────────────────
    val defaultToneModeState = remember {
        mutableStateOf(when (prefs.getString("default_tone_mode", ToneMode.LIGHT.name)) {
            ToneMode.DARK.name    -> ToneMode.DARK
            ToneMode.NEUTRAL.name -> ToneMode.NEUTRAL
            else -> ToneMode.LIGHT
        })
    }
    var defaultToneMode by defaultToneModeState
    fun updateDefaultToneMode(value: ToneMode) {
        defaultToneModeState.value = value; prefs.edit { putString("default_tone_mode", value.name) }
    }
    val enableMulticolorState = remember { mutableStateOf(prefs.getBoolean("default_enable_multicolor", false)) }
    var enableMulticolorByDefault by enableMulticolorState
    fun updateEnableMulticolor(value: Boolean) {
        enableMulticolorState.value = value; prefs.edit { putBoolean("default_enable_multicolor", value) }
    }

    // ── Active effects (single EffectMap drives Home screen) ─────────────────
    val activeEffectsState = remember {
        mutableStateOf(
            WallpaperEffects.defaultMap()
                .withEnabled("overlay",   enableNothingState.value)
                .withEnabled("noise",     enableSnowState.value)
                .withEnabled("stripes",   enableStripesState.value)
                .withEnabled("geometric", enableGeometricState.value)
                .withEnabled("blur",      enableBlurState.value)
        )
    }
    var activeEffects by activeEffectsState

    fun updateEnableNothing(value: Boolean) {
        enableNothingState.value = value; prefs.edit { putBoolean("default_enable_nothing", value) }
        activeEffectsState.value = activeEffectsState.value.withEnabled("overlay", value)
    }
    fun updateEnableSnow(value: Boolean) {
        enableSnowState.value = value; prefs.edit { putBoolean("default_enable_snow", value) }
        activeEffectsState.value = activeEffectsState.value.withEnabled("noise", value)
    }
    fun updateEnableStripes(value: Boolean) {
        enableStripesState.value = value; prefs.edit { putBoolean("default_enable_stripes", value) }
        activeEffectsState.value = activeEffectsState.value.withEnabled("stripes", value)
    }
    fun updateEnableGeometric(value: Boolean) {
        enableGeometricState.value = value; prefs.edit { putBoolean("default_enable_geometric", value) }
        activeEffectsState.value = activeEffectsState.value.withEnabled("geometric", value)
    }
    fun updateEnableBlur(value: Boolean) {
        enableBlurState.value = value; prefs.edit { putBoolean("default_enable_blur", value) }
        activeEffectsState.value = activeEffectsState.value.withEnabled("blur", value)
    }

    // ── Home session state ────────────────────────────────────────────────────
    val homeSessionState = remember {
        WallpaperSessionState(toneMode = defaultToneMode, isMulticolor = enableMulticolorByDefault)
    }

    // ── Favourites ────────────────────────────────────────────────────────────
    val favouriteWallpapersState = remember {
        mutableStateOf(
            // Try v2 key first, fall back to legacy v1 key
            (prefs.getString(FAVOURITES_KEY, null)?.let { decodeFavourites(it) }
                ?: prefs.getString(FAVOURITES_KEY_LEGACY, null)?.let { decodeFavouritesLegacy(it) }
                ?: emptyList<FavoriteWallpaper>())
        )
    }
    var favouriteWallpapers by favouriteWallpapersState
    fun persistFavourites() {
        prefs.edit { putString(FAVOURITES_KEY, encodeFavourites(favouriteWallpapersState.value)) }
    }

    // ── History ───────────────────────────────────────────────────────────────
    val historyWallpapersState = remember {
        mutableStateOf(
            prefs.getString(HISTORY_KEY, null)?.let { decodeHistory(it) } ?: emptyList<HistoryWallpaper>()
        )
    }
    var historyWallpapers by historyWallpapersState
    fun persistHistory() {
        prefs.edit { putString(HISTORY_KEY, encodeHistory(historyWallpapersState.value)) }
    }

    fun addToHistory(wallpaper: Wallpaper, effects: EffectMap) {
        val entry = HistoryWallpaper(wallpaper, effects, System.currentTimeMillis())
        // Keep only last 50 entries to avoid bloat
        val current = historyWallpapersState.value
        val updated = (listOf(entry) + current).take(50)
        historyWallpapersState.value = updated
        persistHistory()
    }
    
    fun removeHistory(item: HistoryWallpaper) {
        historyWallpapersState.value = historyWallpapers - item
        persistHistory()
    }

    fun clearCache() {
        try {
            context.cacheDir?.deleteRecursively()
            historyWallpapersState.value = emptyList()
            persistHistory()
            android.widget.Toast.makeText(context, R.string.cache_cleared, android.widget.Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}
    }

    // Toggle from Home: snapshot current EffectMap when adding
    fun toggleFavouriteFromHome(wallpaper: Wallpaper, effects: EffectMap) {
        fun exactMatch(a: Wallpaper, b: Wallpaper) =
            a.type == b.type && a.angleDeg.compareTo(b.angleDeg) == 0 &&
                    a.colors.size == b.colors.size &&
                    a.colors.zip(b.colors).all { (x, y) -> x.toHexString() == y.toHexString() }
        fun matchIgnoreAngle(a: Wallpaper, b: Wallpaper) =
            a.type == b.type && a.colors.size == b.colors.size &&
                    a.colors.zip(b.colors).all { (x, y) -> x.toHexString() == y.toHexString() }

        val exactMatch   = favouriteWallpapers.find { exactMatch(it.wallpaper, wallpaper) }
        val angleMatch   = favouriteWallpapers.find { matchIgnoreAngle(it.wallpaper, wallpaper) }

        favouriteWallpapersState.value = when {
            exactMatch != null -> favouriteWallpapers - exactMatch
            angleMatch != null -> favouriteWallpapers - angleMatch
            else -> favouriteWallpapers + FavoriteWallpaper(wallpaper = wallpaper, effects = effects)
        }
        persistFavourites()
    }

    fun removeFavourite(fav: FavoriteWallpaper) {
        favouriteWallpapersState.value = favouriteWallpapers - fav
        persistFavourites()
    }

    fun addFavouriteDirect(fav: FavoriteWallpaper) {
        val alreadyExists = favouriteWallpapers.any { existing ->
            existing.wallpaper.type == fav.wallpaper.type &&
                    existing.wallpaper.angleDeg.compareTo(fav.wallpaper.angleDeg) == 0 &&
                    existing.wallpaper.colors.size == fav.wallpaper.colors.size &&
                    existing.wallpaper.colors.zip(fav.wallpaper.colors).all { (x, y) -> x.toHexString() == y.toHexString() } &&
                    existing.effects == fav.effects
        }
        if (!alreadyExists) { favouriteWallpapersState.value = favouriteWallpapers + fav; persistFavourites() }
    }

    // Batch import: one state write + one persist for any number of items.
    fun addFavouritesBatch(favs: List<FavoriteWallpaper>) {
        val newItems = favs.filter { fav ->
            favouriteWallpapers.none { existing ->
                existing.wallpaper.type == fav.wallpaper.type &&
                        existing.wallpaper.angleDeg.compareTo(fav.wallpaper.angleDeg) == 0 &&
                        existing.wallpaper.colors.size == fav.wallpaper.colors.size &&
                        existing.wallpaper.colors.zip(fav.wallpaper.colors).all { (x, y) -> x.toHexString() == y.toHexString() } &&
                        existing.effects == fav.effects
            }
        }
        if (newItems.isNotEmpty()) {
            favouriteWallpapersState.value = favouriteWallpapers + newItems
            persistFavourites()
        }
    }

    var currentScreen  by remember { mutableStateOf(RootScreen.HOME) }
    var isPreviewOpen  by remember { mutableStateOf(false) }

    val isDarkTheme = when (appThemeMode) {
        AppThemeMode.LIGHT  -> false
        AppThemeMode.DARK   -> true
        AppThemeMode.SYSTEM -> systemIsDark
    }
    val selectedForNav = when (currentScreen) {
        RootScreen.HOME      -> RootScreen.HOME
        RootScreen.FAVOURITES -> RootScreen.FAVOURITES
        RootScreen.MARKET -> RootScreen.MARKET
        RootScreen.SETTINGS, RootScreen.ABOUT -> RootScreen.SETTINGS
    }

    BackHandler(enabled = currentScreen != RootScreen.HOME) {
        currentScreen = when (currentScreen) {
            RootScreen.ABOUT      -> RootScreen.SETTINGS
            RootScreen.SETTINGS   -> RootScreen.HOME
            RootScreen.FAVOURITES -> RootScreen.HOME
            RootScreen.MARKET     -> RootScreen.HOME
            RootScreen.HOME       -> RootScreen.HOME
        }
    }

    WallerTheme(darkTheme = isDarkTheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (useGradientBackground) Brush.verticalGradient(listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )) else Brush.verticalGradient(listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    ))
                )
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                bottomBar = {}
            ) { innerPadding ->
                when (currentScreen) {
                    RootScreen.HOME -> {
                        WallpaperGeneratorScreen(
                            modifier = Modifier.padding(innerPadding),
                            sessionState = homeSessionState,
                            isAppDarkMode = isDarkTheme,
                            onThemeChange = {
                                updateThemeMode(when (appThemeMode) {
                                    AppThemeMode.LIGHT -> AppThemeMode.DARK
                                    AppThemeMode.DARK  -> AppThemeMode.LIGHT
                                    AppThemeMode.SYSTEM -> if (systemIsDark) AppThemeMode.LIGHT else AppThemeMode.DARK
                                })
                            },
                            defaultGradientCount    = defaultGradientCount,
                            defaultToneMode         = defaultToneMode,
                            defaultEnableMulticolor = enableMulticolorByDefault,
                            effects                 = activeEffects,
                            onEffectsChange         = { activeEffects = it },
                            favouriteWallpapers     = favouriteWallpapers,
                            onToggleFavourite       = { w, effects -> toggleFavouriteFromHome(w, effects) },
                            isPortrait              = sessionIsPortrait,
                            onOrientationChange     = { sessionIsPortraitState.value = it },
                            interactionMode         = interactionMode,
                            onPreviewVisibilityChanged = { isPreviewOpen = it },
                            onApplied               = { w, fx -> addToHistory(w, fx) }
                        )
                    }

                    RootScreen.FAVOURITES -> {
                        FavoritesScreen(
                            modifier = Modifier.padding(innerPadding),
                            isAppDarkMode = isDarkTheme,
                            onThemeChange = {
                                updateThemeMode(when (appThemeMode) {
                                    AppThemeMode.LIGHT -> AppThemeMode.DARK
                                    AppThemeMode.DARK  -> AppThemeMode.LIGHT
                                    AppThemeMode.SYSTEM -> if (systemIsDark) AppThemeMode.LIGHT else AppThemeMode.DARK
                                })
                            },
                            favourites          = favouriteWallpapers,
                            history             = historyWallpapers,
                            defaultTab          = defaultTab,
                            isPortrait          = sessionIsPortrait,
                            onOrientationChange = { sessionIsPortraitState.value = it },
                            onPreviewVisibilityChanged = { isPreviewOpen = it },
                            onRemoveFavourite   = { removeFavourite(it) },
                            onAddFavourite      = { addFavouriteDirect(it) },
                            onAddFavourites     = { addFavouritesBatch(it) },
                            onRemoveHistory     = { removeHistory(it) },
                            onApplied           = { w, fx -> addToHistory(w, fx) },
                            interactionMode     = interactionMode
                        )
                    }

                    RootScreen.MARKET -> {
                        MarketplaceScreen(
                            modifier = Modifier.padding(innerPadding),
                            favouriteWallpapers = favouriteWallpapers,
                            isPortrait = sessionIsPortrait,
                            onOrientationChange = { sessionIsPortraitState.value = it },
                            isAppDarkMode = isDarkTheme,
                            onThemeChange = {
                                updateThemeMode(when (appThemeMode) {
                                    AppThemeMode.LIGHT -> AppThemeMode.DARK
                                    AppThemeMode.DARK  -> AppThemeMode.LIGHT
                                    AppThemeMode.SYSTEM -> if (systemIsDark) AppThemeMode.LIGHT else AppThemeMode.DARK
                                })
                            },
                            interactionMode = interactionMode,
                            onPreviewVisibilityChanged = { isPreviewOpen = it },
                            onApplied = { w, fx -> addToHistory(w, fx) },
                            onToggleFavorite = { fav ->
                                val existing = favouriteWallpapers.find {
                                    it.wallpaper.type == fav.wallpaper.type &&
                                            it.wallpaper.angleDeg.compareTo(fav.wallpaper.angleDeg) == 0 &&
                                            it.wallpaper.colors.size == fav.wallpaper.colors.size &&
                                            it.wallpaper.colors.zip(fav.wallpaper.colors).all { (x, y) -> x.toHexString() == y.toHexString() } &&
                                            it.effects == fav.effects
                                }
                                if (existing != null) {
                                    removeFavourite(existing)
                                    android.widget.Toast.makeText(context, R.string.removed_from_favourites, android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    addFavouriteDirect(fav)
                                    android.widget.Toast.makeText(context, R.string.added_to_favourites, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    RootScreen.SETTINGS -> {
                        SettingsScreen(
                            modifier = Modifier.padding(innerPadding),
                            appThemeMode = appThemeMode,
                            onAppThemeModeChange = { updateThemeMode(it) },
                            useGradientBackground = useGradientBackground,
                            onUseGradientBackgroundChange = { updateUseGradientBackground(it) },
                            defaultOrientation = defaultOrientation,
                            onDefaultOrientationChange = { updateDefaultOrientation(it) },
                            defaultTab = defaultTab,
                            onDefaultTabChange = { updateDefaultTab(it) },
                            defaultGradientCount = defaultGradientCount,
                            onDefaultGradientCountChange = { updateDefaultGradientCount(it) },
                            enableNothingByDefault = enableNothingByDefault,
                            onEnableNothingByDefaultChange = { updateEnableNothing(it) },
                            enableSnowByDefault = enableSnowByDefault,
                            onEnableSnowByDefaultChange = { updateEnableSnow(it) },
                            enableStripesByDefault = enableStripesByDefault,
                            onEnableStripesByDefaultChange = { updateEnableStripes(it) },
                            enableGeometricByDefault = enableGeometricByDefault,
                            onEnableGeometricByDefaultChange = { updateEnableGeometric(it) },
                            enableBlurByDefault = enableBlurByDefault,
                            onEnableBlurByDefaultChange = { updateEnableBlur(it) },
                            onClearCache = { clearCache() },
                            defaultToneMode = defaultToneMode,
                            onDefaultToneModeChange = { updateDefaultToneMode(it) },
                            defaultEnableMulticolor = enableMulticolorByDefault,
                            onDefaultEnableMulticolorChange = { updateEnableMulticolor(it) },
                            onAboutClick = { currentScreen = RootScreen.ABOUT },
                            interactionMode = interactionMode,
                            onInteractionModeChange = { updateInteractionMode(it) },
                            showBetaUpdates = showBetaUpdates,
                            onShowBetaUpdatesChange = { updateBetaUpdates(it) },
                            hapticsEnabled = hapticsEnabled,
                            onHapticsEnabledChange = { updateHapticsEnabled(it) }
                        )
                    }

                    RootScreen.ABOUT -> {
                        AboutScreen(
                            modifier = Modifier.padding(innerPadding),
                            onBackToSettings = { currentScreen = RootScreen.SETTINGS }
                        )
                    }
                }

                if (!isPreviewOpen) {
                    FloatingNavBar(
                        selectedItem = when (selectedForNav) {
                            RootScreen.HOME       -> FloatingNavItem.HOME
                            RootScreen.FAVOURITES -> FloatingNavItem.FAVOURITES
                            RootScreen.MARKET     -> FloatingNavItem.MARKET
                            else                  -> FloatingNavItem.SETTINGS
                        },
                        defaultTab = defaultTab,
                        onItemSelected = { item ->
                            currentScreen = when (item) {
                                FloatingNavItem.HOME       -> RootScreen.HOME
                                FloatingNavItem.FAVOURITES -> RootScreen.FAVOURITES
                                FloatingNavItem.MARKET     -> RootScreen.MARKET
                                FloatingNavItem.SETTINGS   -> RootScreen.SETTINGS
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)
                    )
                }
            }

            if (showModePickerDialog) {
                ModePickerDialog(
                    initialSelection = interactionMode,
                    onChosen = { chosenMode ->
                        prefs.edit { putInt(PREF_KEY_MODE_PICKER_SHOWN_VERSION, currentVersionCode) }
                        updateInteractionMode(chosenMode)
                        showModePickerDialog = false
                    },
                    onDismiss = {
                        prefs.edit { putInt(PREF_KEY_MODE_PICKER_SHOWN_VERSION, currentVersionCode) }
                        showModePickerDialog = false
                    }
                )
            }

            if (!showModePickerDialog) {
                updateInfo?.let { info ->
                    UpdateAvailableDialog(
                        latestVersion = info.version,
                        releaseNotes  = info.notes,
                        releaseUrl    = info.url,
                        onDismiss     = { updateInfo = null }
                    )
                }
            }
        }
    }

    // ── Import .wall file ─────────────────────────────────────────────────────
    LaunchedEffect(openedWallUri) {
        openedWallUri?.let { uri ->
            val imported = WallFileManager.importWallFile(context, uri)
            imported?.let { walls ->
                val sanitized = walls.map { fav ->
                    val colors = fav.wallpaper.colors
                    fav.copy(wallpaper = fav.wallpaper.copy(
                        colors = if (colors.size == 1) listOf(colors[0], colors[0]) else colors
                    ))
                }
                val newWalls = sanitized.filter { importedFav ->
                    favouriteWallpapers.none { existing ->
                        existing.wallpaper.type == importedFav.wallpaper.type &&
                                existing.wallpaper.angleDeg.compareTo(importedFav.wallpaper.angleDeg) == 0 &&
                                existing.wallpaper.colors.size == importedFav.wallpaper.colors.size &&
                                existing.wallpaper.colors.zip(importedFav.wallpaper.colors).all { (x, y) -> x.toHexString() == y.toHexString() } &&
                                existing.effects == importedFav.effects
                    }
                }
                if (newWalls.isNotEmpty()) {
                    favouriteWallpapersState.value = favouriteWallpapers + newWalls
                    persistFavourites()
                }
                android.widget.Toast.makeText(
                    context,
                    when {
                        newWalls.isEmpty() -> context.getString(R.string.wallpaper_already_exists)
                        newWalls.size == 1 -> context.getString(R.string._1_wallpaper_imported)
                        else               -> "${newWalls.size} wallpapers imported"
                    },
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            (context as Activity).intent.data = null
        }
    }
}

// ── Favourites encode / decode (v2) ──────────────────────────────────────────
//
// Format per entry: type|hex1,hex2,...|angleInt|id:enabled:alpha,id:enabled:alpha,...
// Entries joined by ';'
//
// Example effects segment: noise:1:0.800,stripes:0:1.000,overlay:0:1.000,geometric:0:1.000,blur:0:1.000

private fun encodeFavourites(list: List<FavoriteWallpaper>): String =
    list.joinToString(";") { fav ->
        val typeName  = fav.wallpaper.type.name
        val colorsStr = fav.wallpaper.colors.joinToString(",") { it.toHexString() }
        val angleInt  = fav.wallpaper.angleDeg.roundToInt()
        val effectsStr = WallpaperEffects.ALL.joinToString(",") { def ->
            val enabled = if (fav.effects.isEnabled(def.id)) "1" else "0"
            val alpha   = String.format(Locale.US, "%.3f", fav.effects.alpha(def.id))
            "${def.id}:$enabled:$alpha"
        }
        "$typeName|$colorsStr|$angleInt|$effectsStr"
    }

private fun decodeFavourites(raw: String): List<FavoriteWallpaper> =
    raw.split(";").mapNotNull { item ->
        if (item.isBlank()) return@mapNotNull null
        val parts = item.split("|")
        if (parts.size < 4) return@mapNotNull null

        val type    = runCatching { GradientType.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
        val colors  = parts[1].split(",").mapNotNull { colorFromHexOrNull(it) }
        if (colors.isEmpty()) return@mapNotNull null
        val safeColors = if (colors.size == 1) listOf(colors[0], colors[0]) else colors
        val angleDeg   = parts[2].toFloatOrNull() ?: 0f

        val base    = WallpaperEffects.defaultMap().toMutableMap()
        parts[3].split(",").forEach { token ->
            val tk = token.split(":")
            if (tk.size >= 3) {
                val id      = tk[0]
                val enabled = tk[1] == "1"
                val alpha   = tk[2].toFloatOrNull() ?: 1f
                if (WallpaperEffects.find(id) != null) base[id] = EffectState(enabled, alpha)
            }
        }

        FavoriteWallpaper(
            wallpaper = Wallpaper(colors = safeColors, type = type, angleDeg = angleDeg),
            effects   = base
        )
    }

// ── History encode / decode ──────────────────────────────────────────────────

private fun encodeHistory(list: List<HistoryWallpaper>): String =
    list.joinToString(";") { hist ->
        val typeName  = hist.wallpaper.type.name
        val colorsStr = hist.wallpaper.colors.joinToString(",") { it.toHexString() }
        val angleInt  = hist.wallpaper.angleDeg.roundToInt()
        val effectsStr = WallpaperEffects.ALL.joinToString(",") { def ->
            val enabled = if (hist.effects.isEnabled(def.id)) "1" else "0"
            val alpha   = String.format(Locale.US, "%.3f", hist.effects.alpha(def.id))
            "${def.id}:$enabled:$alpha"
        }
        val timestamp = hist.appliedAt
        "$typeName|$colorsStr|$angleInt|$effectsStr|$timestamp"
    }

private fun decodeHistory(raw: String): List<HistoryWallpaper> =
    raw.split(";").mapNotNull { item ->
        if (item.isBlank()) return@mapNotNull null
        val parts = item.split("|")
        if (parts.size < 4) return@mapNotNull null

        val type    = runCatching { GradientType.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
        val colors  = parts[1].split(",").mapNotNull { colorFromHexOrNull(it) }
        if (colors.isEmpty()) return@mapNotNull null
        val safeColors = if (colors.size == 1) listOf(colors[0], colors[0]) else colors
        val angleDeg   = parts[2].toFloatOrNull() ?: 0f

        val base    = WallpaperEffects.defaultMap().toMutableMap()
        parts[3].split(",").forEach { token ->
            val tk = token.split(":")
            if (tk.size >= 3) {
                val id      = tk[0]
                val enabled = tk[1] == "1"
                val alpha   = tk[2].toFloatOrNull() ?: 1f
                if (WallpaperEffects.find(id) != null) base[id] = EffectState(enabled, alpha)
            }
        }
        
        val appliedAt = parts.getOrNull(4)?.toLongOrNull() ?: System.currentTimeMillis()

        HistoryWallpaper(
            wallpaper = Wallpaper(colors = safeColors, type = type, angleDeg = angleDeg),
            effects   = base,
            appliedAt = appliedAt
        )
    }

/** Decode old v1 format: type|hex1,hex2,...|flags5csv|angleInt|na|sa|oa|ga|bla */
private fun decodeFavouritesLegacy(raw: String): List<FavoriteWallpaper> =
    raw.split(";").mapNotNull { item ->
        if (item.isBlank()) return@mapNotNull null
        val parts = item.split("|")
        if (parts.size < 3) return@mapNotNull null
        val type = runCatching { GradientType.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
        val colors = parts[1].split(",").mapNotNull { colorFromHexOrNull(it) }
        if (colors.isEmpty()) return@mapNotNull null
        val safeColors = if (colors.size == 1) listOf(colors[0], colors[0]) else colors
        val flags     = parts[2].split(",")
        val angleDeg  = parts.getOrNull(3)?.toFloatOrNull() ?: 0f
        val idOrder   = listOf("noise", "stripes", "overlay", "geometric", "blur")
        val alphaIdx  = listOf(4, 5, 6, 7, 8)
        val base      = WallpaperEffects.defaultMap().toMutableMap()
        idOrder.forEachIndexed { i, id ->
            val enabled = flags.getOrNull(i) == "1"
            val alpha   = parts.getOrNull(alphaIdx[i])?.toFloatOrNull() ?: 1f
            base[id]    = EffectState(enabled, alpha)
        }
        FavoriteWallpaper(
            wallpaper = Wallpaper(colors = safeColors, type = type, angleDeg = angleDeg),
            effects   = base
        )
    }