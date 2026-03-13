/**
 * Main screen of the app.
 *
 * Responsibilities:
 * - Holds UI state (colors, gradient types, tone, multicolor)
 * - Uses shared effect state (EffectMap) from WallerApp
 * - Uses shared orientation state from WallerApp (portrait / landscape)
 * - Generates wallpaper preview list
 * - Shows: Header, CompactOptionsPanel, info row, wallpaper grid, Refresh button
 * - Coordinates color picking dialog calls in MainActivity
 * - Opens the Apply/Download dialog when a wallpaper is clicked
 *
 * Adding a new effect requires NO change here.
 */

@file:Suppress("EnumValuesSoftDeprecate", "UNUSED_VALUE")

package com.example.waller.ui.wallpaper

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.waller.MainActivity
import com.example.waller.R
import com.example.waller.ui.wallpaper.components.CompactOptionsPanel
import com.example.waller.ui.wallpaper.components.Header
import com.example.waller.ui.wallpaper.components.WallpaperItemCard
import com.example.waller.ui.wallpaper.components.Actions
import com.example.waller.ui.wallpaper.components.previewOverlay.WallpaperPreviewOverlay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun WallpaperGeneratorScreen(
    modifier: Modifier = Modifier,
    sessionState: WallpaperSessionState,
    isAppDarkMode: Boolean,
    onPreviewVisibilityChanged: (Boolean) -> Unit,
    onThemeChange: () -> Unit,
    defaultGradientCount: Int,
    defaultToneMode: ToneMode,
    defaultEnableMulticolor: Boolean,
    effects: EffectMap,
    onEffectsChange: (EffectMap) -> Unit,
    favouriteWallpapers: List<FavoriteWallpaper>,
    onToggleFavourite: (wallpaper: Wallpaper, effects: EffectMap) -> Unit,
    isPortrait: Boolean,
    onOrientationChange: (Boolean) -> Unit,
    interactionMode: InteractionMode
) {
    val view = LocalView.current

    // ── Session state ─────────────────────────────────────────────────────────
    var toneMode by remember { mutableStateOf(sessionState.toneMode) }
    var showPreview by remember { mutableStateOf(false) }
    var previewWallpaper by remember { mutableStateOf<Wallpaper?>(null) }
    val selectedColors = remember(sessionState) {
        mutableStateListOf<Color>().apply { addAll(sessionState.selectedColors) }
    }
    val selectedGradientTypes = remember(sessionState) {
        mutableStateListOf<GradientType>().apply { addAll(sessionState.selectedGradientTypes) }
    }
    var isMultiColor by remember { mutableStateOf(sessionState.isMulticolor) }

    val coroutineScope  = rememberCoroutineScope()
    val gridState       = rememberLazyGridState(initialFirstVisibleItemIndex = sessionState.scrollIndex)
    val context         = LocalContext.current
    val storagePermissionDeniedMessage = stringResource(id = R.string.storage_permission_denied)

    val writePermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) android.widget.Toast.makeText(
                context, storagePermissionDeniedMessage, android.widget.Toast.LENGTH_SHORT
            ).show()
        }

    val spanCount = if (isPortrait) 2 else 1
    val columns   = GridCells.Fixed(spanCount)

    // ── Wallpaper generation ──────────────────────────────────────────────────
    fun generateWallpapers(): List<Wallpaper> {
        val wallpapers = mutableListOf<Wallpaper>()
        var previousType: GradientType? = null

        repeat(defaultGradientCount) {
            val colors: List<Color> = if (!isMultiColor) {
                when (selectedColors.size) {
                    0    -> listOf(generateRandomColor(toneMode), generateRandomColor(toneMode))
                    1    -> {
                        val base = selectedColors.first()
                        val shadedBase = createShade(base, toneMode, subtle = true)
                        val secondBase = when (toneMode) {
                            ToneMode.LIGHT   -> Color.White
                            ToneMode.DARK    -> Color.Black
                            ToneMode.NEUTRAL -> Color.Gray
                        }
                        listOf(shadedBase, createShade(secondBase, toneMode, subtle = false))
                    }
                    else -> selectedColors.shuffled().take(2).map { createShade(it, toneMode, subtle = true) }
                }
            } else {
                val targetStops = when (selectedColors.size) {
                    0, 1, 2 -> 3
                    else    -> selectedColors.size.coerceIn(3, 5)
                }
                val baseList = mutableListOf<Color>()
                if (selectedColors.isEmpty()) {
                    repeat(targetStops) { baseList += generateRandomColor(toneMode) }
                } else {
                    val source = selectedColors.shuffled(); var i = 0
                    while (baseList.size < targetStops) {
                        baseList += createShade(source[i % source.size], toneMode, subtle = i != 0); i++
                    }
                }
                baseList.shuffled()
            }

            val gradientType = run {
                val available = when {
                    selectedGradientTypes.isEmpty()  -> GradientType.entries.toList()
                    selectedGradientTypes.size == 1  -> selectedGradientTypes.toList()
                    else -> selectedGradientTypes.filter { it != previousType }
                        .ifEmpty { selectedGradientTypes.toList() }
                }
                available.random()
            }
            previousType = gradientType
            wallpapers.add(Wallpaper(colors = colors, type = gradientType))
        }
        return wallpapers
    }

    var wallpapers by remember { mutableStateOf(sessionState.wallpapers) }

    LaunchedEffect(toneMode, isMultiColor, selectedColors.size, selectedGradientTypes.size) {
        if (wallpapers.isEmpty()) {
            val generated = generateWallpapers()
            wallpapers = generated; sessionState.wallpapers = generated
        }
    }

    // ── Dialog state ──────────────────────────────────────────────────────────
    var pendingClickedWallpaper by remember { mutableStateOf<Wallpaper?>(null) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { gridState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index -> sessionState.scrollIndex = index }
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    LazyVerticalGrid(
        columns = columns,
        state = gridState,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp + 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item(span = { GridItemSpan(spanCount) }) {
            Header(
                onThemeChange = onThemeChange,
                isAppDarkMode = isAppDarkMode,
                showOrientationToggle = true,
                isPortrait = isPortrait,
                onOrientationChange = { onOrientationChange(it) }
            )
        }

        item(span = { GridItemSpan(spanCount) }) {
            SectionCard {
                CompactOptionsPanel(
                    toneMode = toneMode,
                    onToneChange = { newMode ->
                        toneMode = newMode; sessionState.toneMode = newMode
                        wallpapers = generateWallpapers(); sessionState.wallpapers = wallpapers
                    },
                    selectedColors = selectedColors,
                    onAddColor = {
                        val activity = context as? MainActivity
                        if (activity != null && selectedColors.size < 5) {
                            activity.openColorDialog(null) { pickedInt ->
                                if (pickedInt != null && selectedColors.size < 5) {
                                    selectedColors.add(pickedInt.toComposeColor())
                                    sessionState.selectedColors = selectedColors.toList()
                                    wallpapers = generateWallpapers(); sessionState.wallpapers = wallpapers
                                }
                            }
                        }
                    },
                    onRemoveColor = { idx ->
                        if (idx in selectedColors.indices) {
                            selectedColors.removeAt(idx)
                            sessionState.selectedColors = selectedColors.toList()
                            wallpapers = generateWallpapers(); sessionState.wallpapers = wallpapers
                        }
                    },
                    isMultiColor = isMultiColor,
                    onMultiColorChange = { newValue ->
                        isMultiColor = newValue; sessionState.isMulticolor = newValue
                        wallpapers = generateWallpapers(); sessionState.wallpapers = wallpapers
                    },
                    selectedGradientTypes = selectedGradientTypes,
                    onGradientToggle = { type ->
                        if (type in selectedGradientTypes) {
                            if (selectedGradientTypes.size == 1) {
                                android.widget.Toast.makeText(
                                    context, "Select at least one gradient style", android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                selectedGradientTypes.remove(type)
                                sessionState.selectedGradientTypes = selectedGradientTypes.toList()
                                wallpapers = generateWallpapers(); sessionState.wallpapers = wallpapers
                            }
                        } else {
                            selectedGradientTypes.add(type)
                            sessionState.selectedGradientTypes = selectedGradientTypes.toList()
                            wallpapers = generateWallpapers(); sessionState.wallpapers = wallpapers
                        }
                    },
                    effects = effects,
                    onEffectToggle = { newEffects -> onEffectsChange(newEffects) }
                )
            }
        }

        // Info row
        item(span = { GridItemSpan(spanCount) }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val orientation = if (isPortrait) stringResource(R.string.orientation_portrait)
                else            stringResource(R.string.orientation_landscape)
                val types = if (selectedGradientTypes.isEmpty()) stringResource(R.string.all)
                else selectedGradientTypes.joinToString(", ") { it.name.lowercase() }

                Text(
                    text = stringResource(R.string.wallpaper_count, wallpapers.size),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.wallpaper_info, orientation, types),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                val refreshRotation = remember { Animatable(0f) }
                val refreshScope    = rememberCoroutineScope()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            Haptics.light(view)
                            refreshScope.launch {
                                val target = refreshRotation.value + 360f
                                refreshRotation.animateTo(
                                    targetValue = target,
                                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                )
                                wallpapers = generateWallpapers(); sessionState.wallpapers = wallpapers
                            }
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.actions_refresh_all),
                        modifier = Modifier.size(20.dp).rotate(refreshRotation.value)
                    )
                }
            }
        }

        // Wallpaper grid
        items(wallpapers) { wallpaper ->
            val isFavourite = favouriteWallpapers.any { it.wallpaper == wallpaper }
            WallpaperItemCard(
                wallpaper       = wallpaper,
                isPortrait      = isPortrait,
                effects         = effects,
                isFavorite      = isFavourite,
                onFavoriteToggle = { w, fx -> onToggleFavourite(w, fx) },
                onClick = {
                    when (interactionMode) {
                        InteractionMode.SIMPLE -> {
                            pendingClickedWallpaper = wallpaper
                            showApplyDialog = true
                        }
                        InteractionMode.ADVANCED -> {
                            previewWallpaper = wallpaper
                            showPreview = true
                            onPreviewVisibilityChanged(true)
                        }
                    }
                },
                onLongClick = {
                    pendingClickedWallpaper = wallpaper
                    showApplyDialog = true
                }
            )
        }

        item(span = { GridItemSpan(spanCount) }) {
            Actions(
                onRefreshClick = {
                    Haptics.longPress(view)
                    wallpapers = generateWallpapers(); sessionState.wallpapers = wallpapers
                    coroutineScope.launch { gridState.animateScrollToItem(2) }
                }
            )
        }
    }

    // ── Preview overlay ───────────────────────────────────────────────────────
    if (showPreview && previewWallpaper != null) {
        val preview = previewWallpaper!!
        WallpaperPreviewOverlay(
            wallpaper       = preview,
            isPortrait      = isPortrait,
            isFavorite      = favouriteWallpapers.any { it.wallpaper == preview },
            initialEffects  = effects,
            onFavoriteToggle = { w, fx -> onToggleFavourite(w, fx) },
            onDismiss = { showPreview = false; onPreviewVisibilityChanged(false) },
            writePermissionLauncher = writePermissionLauncher,
            context         = context,
            coroutineScope  = coroutineScope
        )
    }

    // ── Apply dialog (Simple mode / long-press) ───────────────────────────────
    ApplyDownloadDialog(
        interactionMode  = interactionMode,
        show             = showApplyDialog,
        wallpaper        = pendingClickedWallpaper,
        isPortrait       = isPortrait,
        effects          = effects,
        isWorking        = isWorking,
        onWorkingChange  = { isWorking = it },
        onDismiss        = { showApplyDialog = false; pendingClickedWallpaper = null },
        writePermissionLauncher = writePermissionLauncher,
        context          = context,
        coroutineScope   = coroutineScope
    )
}

/* ── Section card ─────────────────────────────────────────────────── */
@Composable
fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(18.dp), clip = false)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            content()
        }
    }
}