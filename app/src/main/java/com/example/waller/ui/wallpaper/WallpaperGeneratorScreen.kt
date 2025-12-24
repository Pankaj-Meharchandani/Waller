/**
 * Main screen of the app.
 *
 * Responsibilities:
 * - Holds UI state (colors, gradient types, tone, multicolor)
 * - Uses shared effect toggles (snow / stripes / glass) from WallerApp
 * - Uses shared orientation state from WallerApp (portrait / landscape)
 * - Generates wallpaper preview list
 * - Shows:
 *   - Header
 *   - Compact options panel
 *   - Info row + wallpaper grid + Refresh button
 * - Coordinates color picking dialog calls in MainActivity
 * - Opens the Apply/Download dialog when a wallpaper is clicked
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import com.example.waller.ui.wallpaper.components.WallpaperPreviewOverlay
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.waller.MainActivity
import com.example.waller.R
import com.example.waller.ui.wallpaper.components.CompactOptionsPanel
import com.example.waller.ui.wallpaper.components.Header
import com.example.waller.ui.wallpaper.components.WallpaperItemCard
import kotlinx.coroutines.launch

@Composable
fun WallpaperGeneratorScreen(
    modifier: Modifier = Modifier,
    isAppDarkMode: Boolean,
    onThemeChange: () -> Unit,
    defaultGradientCount: Int,
    defaultToneMode: ToneMode,
    defaultEnableMulticolor: Boolean,
    addNoise: Boolean,
    onAddNoiseChange: (Boolean) -> Unit,
    addStripes: Boolean,
    onAddStripesChange: (Boolean) -> Unit,
    addOverlay: Boolean,
    onAddOverlayChange: (Boolean) -> Unit,
    addGeometric: Boolean,
    onAddGeometricChange: (Boolean) -> Unit,
    favouriteWallpapers: List<FavoriteWallpaper>,
    // UPDATED: onToggleFavourite now accepts per-effect alpha floats as well
    onToggleFavourite: (wallpaper: Wallpaper, addNoise: Boolean, addStripes: Boolean, addOverlay: Boolean, addGeometric: Boolean,
                        noiseAlpha: Float, stripesAlpha: Float, overlayAlpha: Float, geometricAlpha: Float) -> Unit,
    isPortrait: Boolean,
    onOrientationChange: (Boolean) -> Unit,
    interactionMode: com.example.waller.ui.wallpaper.InteractionMode

) {
    // ----------- STATE -----------

    var toneMode by remember { mutableStateOf(defaultToneMode) }
    var showPreview by remember { mutableStateOf(false) }
    var previewWallpaper by remember { mutableStateOf<Wallpaper?>(null) }
    val selectedGradientTypes = remember { mutableStateListOf(GradientType.Linear) }
    val selectedColors = remember { mutableStateListOf<Color>() }

    // multicolor state, initial from settings
    var isMultiColor by remember { mutableStateOf(defaultEnableMulticolor) }

    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val context = LocalContext.current
    val storagePermissionDeniedMessage = stringResource(id = R.string.storage_permission_denied)

    // Permission launcher for WRITE_EXTERNAL_STORAGE (only used for API < Q)
    val writePermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                android.widget.Toast.makeText(
                    context,
                    storagePermissionDeniedMessage,
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

    val spanCount = if (isPortrait) 2 else 1
    val columns = GridCells.Fixed(spanCount)

    // ----------- WALLPAPER GENERATION -----------

    fun generateWallpapers(): List<Wallpaper> {
        val wallpapers = mutableListOf<Wallpaper>()
        var previousType: GradientType? = null

        repeat(defaultGradientCount) {
            val colors: List<Color> = if (!isMultiColor) {
                // ---------- 2-COLOR MODE ----------
                when (selectedColors.size) {
                    0 -> listOf(
                        generateRandomColor(toneMode),
                        generateRandomColor(toneMode)
                    )

                    1 -> {
                        val base = selectedColors.first()
                        val shadedBase = createShade(base, toneMode, subtle = true)
                        val secondBase = when (toneMode) {
                            ToneMode.LIGHT -> Color.White
                            ToneMode.DARK -> Color.Black
                            ToneMode.NEUTRAL -> Color.Gray
                        }
                        val shadedSecond = createShade(secondBase, toneMode, subtle = false)
                        listOf(shadedBase, shadedSecond)
                    }

                    else -> selectedColors
                        .shuffled()
                        .take(2)
                        .map { createShade(it, toneMode, subtle = true) }
                }
            } else {
                // ---------- MULTI-COLOR MODE ----------
                val targetStops = when (selectedColors.size) {
                    0 -> 3
                    1 -> 3
                    2 -> 3
                    else -> selectedColors.size.coerceIn(3, 5)
                }

                val baseList = mutableListOf<Color>()

                if (selectedColors.isEmpty()) {
                    repeat(targetStops) {
                        baseList += generateRandomColor(toneMode)
                    }
                } else {
                    val source = selectedColors.shuffled()
                    var i = 0
                    while (baseList.size < targetStops) {
                        val src = source[i % source.size]
                        // first may be slightly stronger, rest more subtle
                        val subtle = i != 0
                        baseList += createShade(src, toneMode, subtle = subtle)
                        i++
                    }
                }

                baseList.shuffled()
            }

            val gradientType = run {
                val available = when {
                    selectedGradientTypes.isEmpty() -> GradientType.entries.toList()
                    selectedGradientTypes.size == 1 -> selectedGradientTypes.toList()
                    else -> {
                        val filtered = selectedGradientTypes.filter { it != previousType }
                        filtered.ifEmpty { selectedGradientTypes.toList() }
                    }
                }
                available.random()
            }

            previousType = gradientType
            wallpapers.add(Wallpaper(colors = colors, type = gradientType))
        }
        return wallpapers
    }

    var wallpapers by remember { mutableStateOf(generateWallpapers()) }

    // ----------- DIALOG STATE -----------

    var pendingClickedWallpaper by remember { mutableStateOf<Wallpaper?>(null) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }

    // ----------- LAYOUT -----------

    LazyVerticalGrid(
        columns = columns,
        state = gridState,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with orientation chip on top-right
        item(span = { GridItemSpan(spanCount) }) {
            Header(
                onThemeChange = onThemeChange,
                isAppDarkMode = isAppDarkMode,
                showOrientationToggle = true,
                isPortrait = isPortrait,
                onOrientationChange = { newValue -> onOrientationChange(newValue) }
            )
        }

        // Compact options panel inside a card
        item(span = { GridItemSpan(spanCount) }) {
            SectionCard {
                CompactOptionsPanel(
                    toneMode = toneMode,
                    onToneChange = { newMode ->
                        toneMode = newMode
                        wallpapers = generateWallpapers()
                    },
                    selectedColors = selectedColors,
                    onAddColor = {
                        val activity = context as? MainActivity
                        if (activity != null && selectedColors.size < 5) {
                            activity.openColorDialog(null) { pickedInt ->
                                if (pickedInt != null && selectedColors.size < 5) {
                                    selectedColors.add(pickedInt.toComposeColor())
                                    wallpapers = generateWallpapers()
                                }
                            }
                        }
                    },
                    onRemoveColor = { idx ->
                        if (idx in selectedColors.indices) {
                            selectedColors.removeAt(idx)
                            wallpapers = generateWallpapers()
                        }
                    },
                    isMultiColor = isMultiColor,
                    onMultiColorChange = { newValue ->
                        isMultiColor = newValue
                        wallpapers = generateWallpapers()
                    },
                    selectedGradientTypes = selectedGradientTypes,
                    onGradientToggle = { type ->
                        if (type in selectedGradientTypes) {
                            if (selectedGradientTypes.size == 1) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Select at least one gradient style",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                selectedGradientTypes.remove(type)
                                wallpapers = generateWallpapers()
                            }
                        } else {
                            selectedGradientTypes.add(type)
                            wallpapers = generateWallpapers()
                        }
                    },
                    addNoise = addNoise,
                    onNoiseToggle = {
                        onAddNoiseChange(!addNoise)
                    },
                    addStripes = addStripes,
                    onStripesToggle = {
                        onAddStripesChange(!addStripes)
                    },
                    addOverlay = addOverlay,
                    onOverlayToggle = {
                        onAddOverlayChange(!addOverlay)
                    },
                    addGeometric = addGeometric,
                    onGeometricToggle = {
                        onAddGeometricChange(!addGeometric)
                    }
                )
            }
        }

        // Info row
        item(span = { GridItemSpan(spanCount) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val orientation =
                    if (isPortrait)
                        stringResource(id = R.string.orientation_portrait)
                    else
                        stringResource(id = R.string.orientation_landscape)

                val types =
                    if (selectedGradientTypes.isEmpty())
                        stringResource(id = R.string.all)
                    else
                        selectedGradientTypes.joinToString(", ") { it.name.lowercase() }

                Text(
                    text = stringResource(id = R.string.wallpaper_count, wallpapers.size),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.wallpaper_info,
                            orientation,
                            types
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                val refreshRotation = remember { Animatable(0f) }
                val refreshScope = rememberCoroutineScope()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable {
                            refreshScope.launch {
                                val start = refreshRotation.value
                                val target = start + 360f
                                refreshRotation.animateTo(
                                    targetValue = target,
                                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                )
                                // regenerate wallpapers after animation
                                wallpapers = generateWallpapers()
                            }
                        }
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                        contentDescription = stringResource(id = R.string.actions_refresh_all),
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(refreshRotation.value)
                    )
                }
            }
        }

        // Wallpapers grid
        items(wallpapers) { wallpaper ->
            val isFavourite = favouriteWallpapers.any { it.wallpaper == wallpaper }
            WallpaperItemCard(
                wallpaper = wallpaper,
                isPortrait = isPortrait,
                addNoise = addNoise,
                addStripes = addStripes,
                addGeometric = addGeometric,
                addOverlay = addOverlay,
                // grid-level cards don't have per-card sliders: use global-enabled -> full alpha (1f) or 0f
                noiseAlpha = if (addNoise) 1f else 0f,
                stripesAlpha = if (addStripes) 1f else 0f,
                overlayAlpha = if (addOverlay) 1f else 0f,
                geometricAlpha = if (addGeometric) 1f else 0f,
                isFavorite = isFavourite,
                onFavoriteToggle = { w, n, s, o, g, na, sa, oa, ga ->
                    onToggleFavourite(w, n, s, o, g, na, sa, oa, ga)
                },
                onClick = {
                    when (interactionMode) {
                        com.example.waller.ui.wallpaper.InteractionMode.SIMPLE -> {
                            // directly show Apply/Download dialog for this wallpaper
                            pendingClickedWallpaper = wallpaper
                            showApplyDialog = true
                        }
                        com.example.waller.ui.wallpaper.InteractionMode.ADVANCED -> {
                            // open overlay as before
                            previewWallpaper = wallpaper
                            showPreview = true
                        }
                    }
                }
            )
        }

        // Bottom "Refresh All" button
        item(span = { GridItemSpan(spanCount) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedButton(
                    onClick = {
                        wallpapers = generateWallpapers()
                        coroutineScope.launch { gridState.animateScrollToItem(2) }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(999.dp))
                ) {
                    Text(stringResource(id = R.string.actions_refresh_all))
                }
            }
        }
    }

    // Inline preview overlay (opened when user taps a wallpaper)
    // inside WallpaperGeneratorScreen where you show overlay:
    if (showPreview && previewWallpaper != null) {
        val preview = previewWallpaper!!
        WallpaperPreviewOverlay(
            wallpaper = preview,
            isPortrait = isPortrait,
            initialNoiseAlpha = if (addNoise) 1f else 0f,
            initialStripesAlpha = if (addStripes) 1f else 0f,
            initialOverlayAlpha = if (addOverlay) 1f else 0f,
            initialGeometricAlpha = if (addGeometric) 1f else 0f,
            isFavorite = favouriteWallpapers.any { it.wallpaper == preview },
            onFavoriteToggle = { wallpaperToSave, n, s, o, g, na, sa, oa, ga ->
                onToggleFavourite(wallpaperToSave, n, s, o, g, na, sa, oa, ga)
            },
            globalNoise = addNoise,
            globalStripes = addStripes,
            globalOverlay = addOverlay,
            globalGeometric = addGeometric,
            onDismiss = { showPreview = false },
            writePermissionLauncher = writePermissionLauncher,
            context = context,
            coroutineScope = coroutineScope
        )
    }

    // Apply / Download dialog (legacy — still available if you open it elsewhere)
    ApplyDownloadDialog(
        show = showApplyDialog,
        wallpaper = pendingClickedWallpaper,
        isPortrait = isPortrait,
        addNoise = addNoise,
        addStripes = addStripes,
        addOverlay = addOverlay,
        addGeometric = addGeometric,
        noiseAlpha = if (addNoise) 1f else 0f, // grid-level default: full or 0
        stripesAlpha = if (addStripes) 1f else 0f,
        overlayAlpha = if (addOverlay) 1f else 0f,
        geometricAlpha = if (addGeometric) 1f else 0f,
        isWorking = isWorking,
        onWorkingChange = { isWorking = it },
        onDismiss = {
            showApplyDialog = false
            pendingClickedWallpaper = null
        },
        writePermissionLauncher = writePermissionLauncher,
        context = context,
        coroutineScope = coroutineScope
    )
}

/* ------------------------------- Section Card ------------------------------- */

@Composable
fun SectionCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(18.dp), clip = false)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            content()
        }
    }
}