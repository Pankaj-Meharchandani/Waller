package com.example.waller.ui

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.Surface
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.waller.ui.wallpaper.InteractionMode
import kotlin.math.roundToInt
import android.content.pm.PackageManager
import com.example.waller.ui.onboarding.ModePickerDialog

// Which top-level screen is shown.
private enum class RootScreen { HOME, FAVOURITES, SETTINGS, ABOUT }

private const val FAVOURITES_KEY = "favourites_v1"
private const val PREF_KEY_INTERACTION_MODE = "interaction_mode_v1"
private const val PREF_KEY_LOCKED_ORIENTATION = "locked_orientation_v1"

// New: remember which app version we've shown the mode picker for
private const val PREF_KEY_MODE_PICKER_SHOWN_VERSION = "mode_picker_shown_version_v1"

@Composable
fun WallerApp() {
    val systemIsDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val activity = context as Activity

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
        prefs.edit { putBoolean("use_gradient_bg", value) }
    }

    // --- PERSISTED INTERACTION MODE (Simple / Advanced) ---
    val initialInteractionMode = remember {
        when (prefs.getString(PREF_KEY_INTERACTION_MODE, InteractionMode.SIMPLE.name)) {
            InteractionMode.ADVANCED.name -> InteractionMode.ADVANCED
            else -> InteractionMode.SIMPLE
        }
    }
    var interactionMode by remember { mutableStateOf(initialInteractionMode) }
    fun updateInteractionMode(mode: InteractionMode) {
        interactionMode = mode
        prefs.edit { putString(PREF_KEY_INTERACTION_MODE, mode.name) }

        if (mode == InteractionMode.ADVANCED) {
            // compute lockMode based on saved lock or current rotation and persist it
            val savedLock = prefs.getInt(PREF_KEY_LOCKED_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)

            val lockMode = if (savedLock != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                // reapply exact saved lock
                savedLock
            } else {
                val rotation = try {
                    @Suppress("DEPRECATION")
                    activity.windowManager.defaultDisplay.rotation
                } catch (_: Exception) {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) Surface.ROTATION_0 else Surface.ROTATION_90
                }
                val computed = when (rotation) {
                    Surface.ROTATION_0 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                prefs.edit { putInt(PREF_KEY_LOCKED_ORIENTATION, computed) }
                computed
            }

            activity.requestedOrientation = lockMode
        } else {
            // SIMPLE mode → restore normal rotation and clear saved lock
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            prefs.edit { remove(PREF_KEY_LOCKED_ORIENTATION) }
        }
    }

    // --- ONE-TIME MODE PICKER DIALOG WIRING ---
    var showModePickerDialog by remember { mutableStateOf(false) }

    // compute current app versionCode safely (fallback to 1)
    val currentVersionCode: Int = try {
        val pi = context.packageManager.getPackageInfo(context.packageName, 0)
        @Suppress("DEPRECATION")
        pi.versionCode
    } catch (e: Exception) {
        1
    }

    // decide whether to show dialog (first run or first run after update)
    LaunchedEffect(Unit) {
        val shownFor = prefs.getInt(PREF_KEY_MODE_PICKER_SHOWN_VERSION, -1)
        if (shownFor != currentVersionCode) {
            showModePickerDialog = true
        }
    }

    // Reapply orientation lock on app startup based on saved values.
    LaunchedEffect(Unit) {
        val savedModeName = prefs.getString(PREF_KEY_INTERACTION_MODE, InteractionMode.SIMPLE.name)
        val savedMode = if (savedModeName == InteractionMode.ADVANCED.name) InteractionMode.ADVANCED else InteractionMode.SIMPLE
        interactionMode = savedMode

        if (savedMode == InteractionMode.ADVANCED) {
            val savedLock = prefs.getInt(PREF_KEY_LOCKED_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
            if (savedLock != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                // reapply the exact saved lock
                activity.requestedOrientation = savedLock
            } else {
                // fallback compute and persist
                val rotation = try {
                    @Suppress("DEPRECATION")
                    activity.windowManager.defaultDisplay.rotation
                } catch (_: Exception) {
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) Surface.ROTATION_0 else Surface.ROTATION_90
                }
                val computed = when (rotation) {
                    Surface.ROTATION_0 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                activity.requestedOrientation = computed
                prefs.edit { putInt(PREF_KEY_LOCKED_ORIENTATION, computed) }
            }
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

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
    // Matching strategy: try exact match (colors+type+angle), fallback to colors+type (ignore angle)
    fun toggleFavouriteFromHome(
        wallpaper: Wallpaper,
        addNoise: Boolean,
        addStripes: Boolean,
        addOverlay: Boolean,
        noiseAlpha: Float = 1f,
        stripesAlpha: Float = 1f,
        overlayAlpha: Float = 1f
    ) {
        // exact compare (type + angle + color stops)
        fun exactMatch(a: Wallpaper, b: Wallpaper): Boolean =
            a.type == b.type &&
                    a.angleDeg.compareTo(b.angleDeg) == 0 &&
                    a.colors.size == b.colors.size &&
                    a.colors.zip(b.colors).all { (x, y) -> x.toHexString() == y.toHexString() }

        // match ignoring angle (back-compat)
        fun matchIgnoringAngle(a: Wallpaper, b: Wallpaper): Boolean =
            a.type == b.type &&
                    a.colors.size == b.colors.size &&
                    a.colors.zip(b.colors).all { (x, y) -> x.toHexString() == y.toHexString() }

        val existingExact = favouriteWallpapers.find { exactMatch(it.wallpaper, wallpaper) }
        val existingIgnoreAngle = favouriteWallpapers.find { matchIgnoringAngle(it.wallpaper, wallpaper) }

        favouriteWallpapers = when {
            existingExact != null -> favouriteWallpapers - existingExact
            existingIgnoreAngle != null -> favouriteWallpapers - existingIgnoreAngle
            else -> {
                favouriteWallpapers + FavoriteWallpaper(
                    wallpaper = wallpaper,
                    addNoise = addNoise,
                    addStripes = addStripes,
                    addOverlay = addOverlay,
                    noiseAlpha = noiseAlpha,
                    stripesAlpha = stripesAlpha,
                    overlayAlpha = overlayAlpha
                )
            }
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
                        onScreenSelected = { screen -> currentScreen = screen }
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
                            defaultToneMode = defaultToneMode,
                            defaultEnableMulticolor = enableMulticolorByDefault,
                            addNoise = snowEffectEnabled,
                            onAddNoiseChange = { snowEffectEnabled = it },
                            addStripes = stripesEffectEnabled,
                            onAddStripesChange = { stripesEffectEnabled = it },
                            addOverlay = overlayEffectEnabled,
                            onAddOverlayChange = { overlayEffectEnabled = it },
                            favouriteWallpapers = favouriteWallpapers,
                            onToggleFavourite = { w, n, s, o, na, sa, oa ->
                                toggleFavouriteFromHome(w, n, s, o, na, sa, oa)
                            },
                            isPortrait = sessionIsPortrait,
                            onOrientationChange = { sessionIsPortrait = it },
                            interactionMode = interactionMode
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
                            onRemoveFavourite = { fav -> removeFavourite(fav) },
                            onAddFavourite = { fav ->
                                toggleFavouriteFromHome(
                                    fav.wallpaper,
                                    fav.addNoise,
                                    fav.addStripes,
                                    fav.addOverlay,
                                    fav.noiseAlpha,
                                    fav.stripesAlpha,
                                    fav.overlayAlpha
                                )
                            },
                            interactionMode = interactionMode
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
                            onAboutClick = { currentScreen = RootScreen.ABOUT },
                            interactionMode = interactionMode,
                            onInteractionModeChange = { updateInteractionMode(it) }
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

            // Render the one-time ModePickerDialog as an overlay when needed
            if (showModePickerDialog) {
                ModePickerDialog(
                    initialSelection = interactionMode,
                    onChosen = { chosenMode ->
                        // persist shown-version so we don't show again for the same app version
                        prefs.edit { putInt(PREF_KEY_MODE_PICKER_SHOWN_VERSION, currentVersionCode) }
                        // apply chosen mode (this handles orientation lock and persistence)
                        updateInteractionMode(chosenMode)
                        showModePickerDialog = false
                    },
                    onDismiss = {
                        // user dismissed -> mark as shown for this version so we won't nag again
                        prefs.edit { putInt(PREF_KEY_MODE_PICKER_SHOWN_VERSION, currentVersionCode) }
                        showModePickerDialog = false
                    }
                )
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

/** Encodes favourites as:
 * type|hex1,hex2,...|flagsCsv|angleInt|noiseAlpha|stripesAlpha|overlayAlpha
 * (joined by ';' between items)
 */
private fun encodeFavourites(list: List<FavoriteWallpaper>): String =
    list.joinToString(";") { fav ->
        val typeName = fav.wallpaper.type.name
        val colorsStr = fav.wallpaper.colors.joinToString(",") { it.toHexString() }
        val flagsStr = listOf(fav.addNoise, fav.addStripes, fav.addOverlay)
            .joinToString(",") { if (it) "1" else "0" }
        val angleInt = fav.wallpaper.angleDeg.roundToInt()
        val na = String.format("%.3f", fav.noiseAlpha)
        val sa = String.format("%.3f", fav.stripesAlpha)
        val oa = String.format("%.3f", fav.overlayAlpha)
        listOf(typeName, colorsStr, flagsStr, angleInt.toString(), na, sa, oa).joinToString("|")
    }

/** Decodes both new (7-part) and old (4-part without alphas) formats. */
private fun decodeFavourites(raw: String): List<FavoriteWallpaper> =
    raw.split(";")
        .mapNotNull { item ->
            if (item.isBlank()) return@mapNotNull null
            val parts = item.split("|")
            if (parts.size < 3) return@mapNotNull null

            val type = runCatching { GradientType.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null

            val colors = parts.getOrNull(1)
                ?.split(",")
                ?.mapNotNull { token -> colorFromHexOrNull(token) }
                ?.takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null

            val flagTokens = parts.getOrNull(2)?.split(",") ?: listOf()
            val addNoise = flagTokens.getOrNull(0) == "1"
            val addStripes = flagTokens.getOrNull(1) == "1"
            val addOverlay = flagTokens.getOrNull(2) == "1"

            val angleDeg = parts.getOrNull(3)?.toFloatOrNull() ?: 0f
            val noiseAlpha = parts.getOrNull(4)?.toFloatOrNull() ?: 1f
            val stripesAlpha = parts.getOrNull(5)?.toFloatOrNull() ?: 1f
            val overlayAlpha = parts.getOrNull(6)?.toFloatOrNull() ?: 1f

            FavoriteWallpaper(
                wallpaper = Wallpaper(colors = colors, type = type, angleDeg = angleDeg),
                addNoise = addNoise,
                addStripes = addStripes,
                addOverlay = addOverlay,
                noiseAlpha = noiseAlpha,
                stripesAlpha = stripesAlpha,
                overlayAlpha = overlayAlpha
            )
        }
