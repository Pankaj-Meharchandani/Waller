/**
 * Main screen of the app.
 *
 * Responsibilities:
 * - Holds all UI state (colors, gradient types, toggles, orientation)
 * - Generates wallpaper preview list
 * - Displays the full UI layout using multiple reusable components
 * - Coordinates color picking dialog calls in MainActivity
 * - Opens the Apply/Download dialog when a wallpaper is clicked
 *
 * This file orchestrates the entire wallpaper creation experience.
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.waller.MainActivity
import com.example.waller.R
import com.example.waller.ui.settings.DefaultOrientation
import com.example.waller.ui.wallpaper.components.Actions
import com.example.waller.ui.wallpaper.components.ColorSelector
import com.example.waller.ui.wallpaper.components.EffectsSelector
import com.example.waller.ui.wallpaper.components.GradientStylesSelector
import com.example.waller.ui.wallpaper.components.Header
import com.example.waller.ui.wallpaper.components.OrientationSelector
import com.example.waller.ui.wallpaper.components.WallpaperItemCard
import com.example.waller.ui.wallpaper.components.WallpaperThemeSelector
import kotlinx.coroutines.launch

@Composable
fun WallpaperGeneratorScreen(
    modifier: Modifier = Modifier,
    isAppDarkMode: Boolean,
    onThemeChange: () -> Unit,
    defaultOrientation: DefaultOrientation,
    defaultGradientCount: Int,
    defaultEnableNothing: Boolean,
    defaultEnableSnow: Boolean,
    defaultEnableStripes: Boolean
) {
    // Initial orientation based on settings (AUTO treated as portrait by default here)
    var isPortrait by remember {
        mutableStateOf(
            when (defaultOrientation) {
                DefaultOrientation.LANDSCAPE -> false
                DefaultOrientation.AUTO, DefaultOrientation.PORTRAIT -> true
            }
        )
    }

    var isLightTones by remember { mutableStateOf(true) }
    val selectedGradientTypes = remember { mutableStateListOf(GradientType.Linear) }
    val selectedColors = remember { mutableStateListOf<androidx.compose.ui.graphics.Color>() }

    // Initial effects from settings
    var addNoise by remember { mutableStateOf(defaultEnableSnow) }
    var addStripes by remember { mutableStateOf(defaultEnableStripes) }
    var addOverlay by remember { mutableStateOf(defaultEnableNothing) }

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

    // dynamic columns/spans
    val spanCount = if (isPortrait) 2 else 1
    val columns = GridCells.Fixed(spanCount)

    // generate wallpapers function (respects defaultGradientCount)
    fun generateWallpapers(): List<Wallpaper> {
        val wallpapers = mutableListOf<Wallpaper>()
        var previousType: GradientType? = null
        repeat(defaultGradientCount) {
            val colors = when (selectedColors.size) {
                0 -> listOf(
                    generateRandomColor(isLightTones),
                    generateRandomColor(isLightTones)
                )

                1 -> {
                    val base = selectedColors.first()
                    val shadedBase = createShade(base, isLightTones)
                    val secondBase =
                        if (isLightTones) androidx.compose.ui.graphics.Color.White
                        else androidx.compose.ui.graphics.Color.Black
                    val shadedSecond = createShade(secondBase, isLightTones)
                    listOf(shadedBase, shadedSecond)
                }

                else -> selectedColors.shuffled().take(2).map { createShade(it, isLightTones) }
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

    // ----- open SimpleColorDialog when editingColorIndex changes -----
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
                    }
                    editingColorIndex = null
                }
            }
        }
    }

    // ---------------- pending click / dialog states ----------------
    var pendingClickedWallpaper by remember { mutableStateOf<Wallpaper?>(null) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }

    LazyVerticalGrid(
        columns = columns,
        state = gridState,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(spanCount) }) {
            Header(onThemeChange = onThemeChange, isAppDarkMode = isAppDarkMode)
        }

        item(span = { GridItemSpan(spanCount) }) {
            SectionCard {
                OrientationSelector(
                    isPortrait = isPortrait,
                    onOrientationChange = { isPortrait = it }
                )
            }
        }

        item(span = { GridItemSpan(spanCount) }) {
            SectionCard {
                WallpaperThemeSelector(
                    isLightTones = isLightTones,
                    onThemeChange = { isLightTones = it }
                )
            }
        }

        item(span = { GridItemSpan(spanCount) }) {
            SectionCard {
                GradientStylesSelector(
                    selectedGradientTypes = selectedGradientTypes,
                    onStyleChange = { style ->
                        if (style in selectedGradientTypes) {
                            if (selectedGradientTypes.size > 1) selectedGradientTypes.remove(style)
                        } else selectedGradientTypes.add(style)
                    },
                    onSelectAll = {
                        selectedGradientTypes.clear()
                        selectedGradientTypes.addAll(GradientType.entries)
                    },
                    onClear = {
                        selectedGradientTypes.clear()
                        selectedGradientTypes.add(GradientType.Linear)
                    }
                )
            }
        }

        item(span = { GridItemSpan(spanCount) }) {
            SectionCard {
                ColorSelector(
                    selectedColors = selectedColors,
                    onAddColor = { editingColorIndex = -1 },
                    onEditColor = { idx -> editingColorIndex = idx },
                    onRemoveColor = { idx ->
                        if (idx in selectedColors.indices) selectedColors.removeAt(idx)
                    }
                )
            }
        }

        item(span = { GridItemSpan(spanCount) }) {
            SectionCard {
                EffectsSelector(
                    addNoise = addNoise,
                    onNoiseChange = { addNoise = it },
                    addStripes = addStripes,
                    onStripesChange = { addStripes = it },
                    addOverlay = addOverlay,
                    onOverlayChange = { addOverlay = it }
                )
            }
        }

        item(span = { GridItemSpan(spanCount) }) {
            SectionCard {
                Actions(onRefreshClick = { wallpapers = generateWallpapers() })
            }
        }

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

        items(wallpapers) { wallpaper ->
            WallpaperItemCard(
                wallpaper = wallpaper,
                isPortrait = isPortrait,
                addNoise = addNoise,
                addStripes = addStripes,
                addOverlay = addOverlay,
                onClick = { clicked ->
                    pendingClickedWallpaper = clicked
                    showApplyDialog = true
                }
            )
        }

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
                        coroutineScope.launch { gridState.animateScrollToItem(6) }
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