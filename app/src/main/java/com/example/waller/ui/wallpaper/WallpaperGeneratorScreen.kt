/**
 * Main screen of the app.
 *
 * Responsibilities:
 * - Holds UI state (colors, gradient types, tone)
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    defaultEnableNothing: Boolean,
    defaultEnableSnow: Boolean,
    defaultEnableStripes: Boolean,
    defaultToneMode: ToneMode,
    addNoise: Boolean,
    onAddNoiseChange: (Boolean) -> Unit,
    addStripes: Boolean,
    onAddStripesChange: (Boolean) -> Unit,
    addOverlay: Boolean,
    onAddOverlayChange: (Boolean) -> Unit,
    favouriteWallpapers: List<FavoriteWallpaper>,
    onToggleFavourite: (wallpaper: Wallpaper, addNoise: Boolean, addStripes: Boolean, addOverlay: Boolean) -> Unit,
    isPortrait: Boolean,
    onOrientationChange: (Boolean) -> Unit
) {
    // ----------- STATE -----------

    var toneMode by remember { mutableStateOf(defaultToneMode) }

    val selectedGradientTypes = remember { mutableStateListOf(GradientType.Linear) }
    val selectedColors = remember { mutableStateListOf<Color>() }

    var editingColorIndex by remember { mutableStateOf<Int?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val context = LocalContext.current

    // Permission launcher for WRITE_EXTERNAL_STORAGE (only used for API < Q)
    val writePermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.storage_permission_denied),
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
            val colors = when (selectedColors.size) {
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

    // ----------- COLOR PICKER BRIDGE -----------

    if (editingColorIndex != null) {
        val idx = editingColorIndex!!
        LaunchedEffect(idx) {
            val activity = context as? MainActivity
            val initialColor =
                if (idx >= 0 && idx < selectedColors.size) selectedColors[idx] else null

            if (activity == null) {
                editingColorIndex = null
            } else {
                activity.openColorDialog(initialColor?.toArgbInt()) { pickedInt ->
                    if (pickedInt != null) {
                        val pickedColor = pickedInt.toComposeColor()
                        if (idx >= 0 && idx < selectedColors.size) {
                            selectedColors[idx] = pickedColor
                        } else if (selectedColors.size < 5) {
                            selectedColors.add(pickedColor)
                        }
                        wallpapers = generateWallpapers()
                    }
                    editingColorIndex = null
                }
            }
        }
    }

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
                    isPortrait = isPortrait,
                    onOrientationChange = { newValue ->
                        onOrientationChange(newValue)
                    },
                    toneMode = toneMode,
                    onToneChange = { newMode ->
                        toneMode = newMode
                        wallpapers = generateWallpapers()
                    },
                    selectedColors = selectedColors,
                    onAddColor = { editingColorIndex = -1 },
                    onRemoveColor = { idx ->
                        if (idx in selectedColors.indices) {
                            selectedColors.removeAt(idx)
                            wallpapers = generateWallpapers()
                        }
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
                        wallpapers = generateWallpapers()
                    },
                    addStripes = addStripes,
                    onStripesToggle = {
                        onAddStripesChange(!addStripes)
                        wallpapers = generateWallpapers()
                    },
                    addOverlay = addOverlay,
                    onOverlayToggle = {
                        onAddOverlayChange(!addOverlay)
                        wallpapers = generateWallpapers()
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
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.wallpaper_info,
                            orientation,
                            types
                        ),
                        style = MaterialTheme.typography.bodySmall
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
                addOverlay = addOverlay,
                isFavorite = isFavourite,
                onFavoriteToggle = {
                    onToggleFavourite(wallpaper, addNoise, addStripes, addOverlay)
                },
                onClick = {
                    pendingClickedWallpaper = wallpaper
                    showApplyDialog = true
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

    // Apply / Download dialog
    ApplyDownloadDialog(
        show = showApplyDialog,
        wallpaper = pendingClickedWallpaper,
        isPortrait = isPortrait,
        addNoise = addNoise,
        addStripes = addStripes,
        addOverlay = addOverlay,
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
