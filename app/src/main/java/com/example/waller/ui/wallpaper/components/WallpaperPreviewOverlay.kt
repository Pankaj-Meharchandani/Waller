/**
 * WallpaperPreviewOverlay.kt
 * Fullscreen UI composables for the preview overlay (UI only).
 * - Uses PreviewRenderer.kt for heavy rendering logic (brushes/shaders).
 * - Keeps layout, chips, sliders and dialog wiring here.
 */

@file:Suppress("DEPRECATION", "COMPOSE_APPLIER_CALL_MISMATCH")
package com.example.waller.ui.wallpaper.components

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.waller.R
import com.example.waller.ui.wallpaper.ApplyDownloadDialog
import com.example.waller.ui.wallpaper.Wallpaper
import com.example.waller.ui.wallpaper.GradientType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun WallpaperPreviewOverlay(
    wallpaper: Wallpaper,
    isPortrait: Boolean,
    isFavorite: Boolean,
    globalNoise: Boolean,
    globalStripes: Boolean,
    globalOverlay: Boolean,
    // updated callback: receive the wallpaper snapshot + effect flags
    onFavoriteToggle: (wallpaper: Wallpaper, noise: Boolean, stripes: Boolean, overlay: Boolean) -> Unit,
    onDismiss: () -> Unit,
    writePermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    context: Context,
    coroutineScope: CoroutineScope
) {
    // small local helpers
    @Composable
    fun overlayTextColor(selectedForButton: Boolean = false): Color {
        val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
        return if (isLightTheme && selectedForButton) Color.Black else Color.White
    }

    var noise by remember { mutableStateOf(globalNoise) }
    var stripes by remember { mutableStateOf(globalStripes) }
    var overlay by remember { mutableStateOf(globalOverlay) }

    var noiseAlpha by remember { mutableFloatStateOf(initAlphaFor(noise, DEFAULT_NOISE_ALPHA)) }
    var stripesAlpha by remember { mutableFloatStateOf(initAlphaFor(stripes, DEFAULT_STRIPES_ALPHA)) }
    var overlayAlpha by remember { mutableFloatStateOf(initAlphaFor(overlay, DEFAULT_OVERLAY_ALPHA)) }

    var selectedGradient by remember(wallpaper) {
        mutableStateOf(
            when (wallpaper.type.name.lowercase()) {
                "angular" -> GradientType.Angular
                "radial" -> GradientType.Radial
                "diamond" -> GradientType.Diamond
                else -> GradientType.Linear
            }
        )
    }

    var gradientAngle by remember(wallpaper) { mutableFloatStateOf(wallpaper.angleDeg) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    BackHandler { onDismiss() }

    val statusBarPadding: Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val aspectRatio = if (isPortrait) 9f / 16f else 16f / 9f

    // Guard to prevent immediate self-dismiss by the opening tap
    var justOpened by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(200L)
        justOpened = false
    }

    // create a previewWallpaper snapshot that represents the user's preview choices.
    val previewWallpaper = remember(wallpaper, selectedGradient, gradientAngle) {
        wallpaper.copy(type = selectedGradient, angleDeg = gradientAngle)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // scrim (captures outside taps)
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectTapGestures { if (!justOpened) onDismiss() }
                }
                .background(Color.Black.copy(alpha = 0.75f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.32f)),
                            radius = 1000f
                        )
                    )
            )
        }

        // ==================== HEADER (X + APPLY BUTTON) ====================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close button (X) with contrast surface
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
                modifier = Modifier.height(46.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.preview_close),
                        tint = overlayTextColor()
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Apply button (right)
            TextButton(
                onClick = { if (!isBusy) showApplyDialog = true },
                modifier = Modifier.height(44.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.preview_done),
                    color = overlayTextColor()
                )
            }
        }

        // Main overlay UI (on top)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isPortrait) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val previewWidth = (screenWidth * 0.36f).coerceAtMost(420.dp)

                    Box(
                        modifier = Modifier
                            .width(previewWidth)
                            .aspectRatio(aspectRatio)
                            .clip(RoundedCornerShape(14.dp))
                            .shadow(6.dp)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        DeviceFrame(modifier = Modifier.fillMaxSize()) {
                            // Use previewWallpaper so preview + saved snapshots match
                            PreviewWallpaperRender(
                                wallpaper = previewWallpaper,
                                previewType = selectedGradient,
                                angleDeg = gradientAngle,
                                addNoise = noise,
                                addStripes = stripes,
                                addOverlay = overlay,
                                modifier = Modifier.fillMaxSize()
                            )

                            var localFav by remember { mutableStateOf(isFavorite) } // optimistic UI; parent should update real state
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            // optimistic toggle so user sees immediate feedback
                                            localFav = !localFav
                                            // propagate favorite change with current preview snapshot + effect flags
                                            onFavoriteToggle(previewWallpaper, noise, stripes, overlay)
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (localFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                            contentDescription = stringResource(id = R.string.preview_favorite),
                                            tint = if (localFav) Color(0xFFFF4D6A) else overlayTextColor()
                                        )
                                    }
                                }
                            }

                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(
                        modifier = Modifier
                            .widthIn(min = 180.dp, max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(stringResource(id = R.string.gradient_style_title), style = MaterialTheme.typography.titleMedium, color = overlayTextColor())
                        Spacer(Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            GradientTypeItemFull(
                                label = stringResource(id = R.string.gradient_style_linear),
                                selected = selectedGradient == GradientType.Linear,
                                textColor = overlayTextColor(selectedForButton = selectedGradient == GradientType.Linear)
                            ) {
                                selectedGradient = GradientType.Linear
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            }

                            GradientTypeItemFull(
                                label = stringResource(id = R.string.gradient_style_radial),
                                selected = selectedGradient == GradientType.Radial,
                                textColor = overlayTextColor(selectedForButton = selectedGradient == GradientType.Radial)
                            ) {
                                selectedGradient = GradientType.Radial
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            }

                            GradientTypeItemFull(
                                label = stringResource(id = R.string.gradient_style_angular),
                                selected = selectedGradient == GradientType.Angular,
                                textColor = overlayTextColor(selectedForButton = selectedGradient == GradientType.Angular)
                            ) {
                                selectedGradient = GradientType.Angular
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            }

                            GradientTypeItemFull(
                                label = stringResource(id = R.string.gradient_style_diamond),
                                selected = selectedGradient == GradientType.Diamond,
                                textColor = overlayTextColor(selectedForButton = selectedGradient == GradientType.Diamond)
                            ) {
                                selectedGradient = GradientType.Diamond
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${gradientAngle.toInt()}°", modifier = Modifier.width(44.dp), color = overlayTextColor())
                            Slider(
                                value = gradientAngle,
                                onValueChange = { gradientAngle = it },
                                valueRange = 0f..360f,
                                modifier = Modifier.weight(1f)
                            )
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        }
                    }
                }
            } else {
                // Landscape path (kept compact)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val previewWidth = (screenWidth * 0.72f).coerceAtMost(900.dp)
                    Box(
                        modifier = Modifier
                            .width(previewWidth)
                            .aspectRatio(aspectRatio)
                            .clip(RoundedCornerShape(14.dp))
                            .shadow(6.dp)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        DeviceFrame(modifier = Modifier.fillMaxSize()) {
                            PreviewWallpaperRender(
                                wallpaper = previewWallpaper,
                                previewType = selectedGradient,
                                angleDeg = gradientAngle,
                                addNoise = noise,
                                addStripes = stripes,
                                addOverlay = overlay,
                                modifier = Modifier.fillMaxSize()
                            )
                            var localFav by remember { mutableStateOf(isFavorite) } // optimistic UI; parent should update real state
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            // optimistic toggle so user sees immediate feedback
                                            localFav = !localFav
                                            // propagate favorite change with current preview snapshot + effect flags
                                            onFavoriteToggle(previewWallpaper, noise, stripes, overlay)
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (localFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                            contentDescription = stringResource(id = R.string.preview_favorite),
                                            tint = if (localFav) Color(0xFFFF4D6A) else overlayTextColor()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Chips row (compact)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(id = R.string.gradient_style_title), style = MaterialTheme.typography.titleMedium, color = overlayTextColor())
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            GradientTypeItemRect(
                                label = stringResource(id = R.string.gradient_style_linear),
                                selected = selectedGradient == GradientType.Linear,
                                textColor = overlayTextColor(selectedForButton = selectedGradient == GradientType.Linear)
                            ) { selectedGradient = GradientType.Linear }

                            GradientTypeItemRect(
                                label = stringResource(id = R.string.gradient_style_radial),
                                selected = selectedGradient == GradientType.Radial,
                                textColor = overlayTextColor(selectedForButton = selectedGradient == GradientType.Radial)
                            ) { selectedGradient = GradientType.Radial }

                            GradientTypeItemRect(
                                label = stringResource(id = R.string.gradient_style_angular),
                                selected = selectedGradient == GradientType.Angular,
                                textColor = overlayTextColor(selectedForButton = selectedGradient == GradientType.Angular)
                            ) { selectedGradient = GradientType.Angular }

                            GradientTypeItemRect(
                                label = stringResource(id = R.string.gradient_style_diamond),
                                selected = selectedGradient == GradientType.Diamond,
                                textColor = overlayTextColor(selectedForButton = selectedGradient == GradientType.Diamond)
                            ) { selectedGradient = GradientType.Diamond }
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${gradientAngle.toInt()}°", modifier = Modifier.width(44.dp), color = overlayTextColor())
                            Slider(
                                value = gradientAngle,
                                onValueChange = { gradientAngle = it },
                                valueRange = 0f..360f,
                                modifier = Modifier.weight(1f)
                            )
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Effects chips
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.06f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EffectChip(stringResource(id = R.string.preview_effect_nothing), overlay, textColor = overlayTextColor(selectedForButton = overlay)) {
                        overlay = !overlay
                        if (overlay && overlayAlpha <= 0f) overlayAlpha = DEFAULT_OVERLAY_ALPHA
                        if (!overlay) overlayAlpha = 0f
                    }
                    EffectChip(stringResource(id = R.string.preview_effect_snow), noise, textColor = overlayTextColor(selectedForButton = noise)) {
                        noise = !noise
                        if (noise && noiseAlpha <= 0f) noiseAlpha = DEFAULT_NOISE_ALPHA
                        if (!noise) noiseAlpha = 0f
                    }
                    EffectChip(stringResource(id = R.string.preview_effect_stripes), stripes, textColor = overlayTextColor(selectedForButton = stripes)) {
                        stripes = !stripes
                        if (stripes && stripesAlpha <= 0f) stripesAlpha = DEFAULT_STRIPES_ALPHA
                        if (!stripes) stripesAlpha = 0f
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Minimal effect sliders (kept compact)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EffectOpacitySlider(stringResource(id = R.string.preview_opacity_nothing), overlayAlpha,
                    onSliderChange = {
                        overlayAlpha = it; overlay = it > 0.0001f
                    }, labelColor = overlayTextColor())

                EffectOpacitySlider(stringResource(id = R.string.preview_opacity_snow), noiseAlpha,
                    onSliderChange = {
                        noiseAlpha = it; noise = it > 0.0001f
                    }, labelColor = overlayTextColor())

                EffectOpacitySlider(stringResource(id = R.string.preview_opacity_stripes), stripesAlpha,
                    onSliderChange = {
                        stripesAlpha = it; stripes = it > 0.0001f
                    }, labelColor = overlayTextColor())
            }
        }

        // Apply dialog — pass previewWallpaper so saved/applied output matches preview
        if (showApplyDialog) {
            ApplyDownloadDialog(
                show = true,
                wallpaper = previewWallpaper,
                isPortrait = isPortrait,
                addNoise = noise,
                addStripes = stripes,
                addOverlay = overlay,
                isWorking = isBusy,
                onWorkingChange = { isBusy = it },
                onDismiss = { showApplyDialog = false },
                writePermissionLauncher = writePermissionLauncher,
                context = context,
                coroutineScope = coroutineScope
            )
        }
    }
}

@Composable
private fun PreviewWallpaperRender(
    wallpaper: Wallpaper,
    previewType: GradientType,
    angleDeg: Float,
    addNoise: Boolean,
    addStripes: Boolean,
    addOverlay: Boolean,
    modifier: Modifier = Modifier
) {
    val cornerRadius = 14.dp
    Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius))) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthDp = maxWidth
            val heightDp = maxHeight
            val density = LocalDensity.current
            val widthPx = with(density) { widthDp.toPx() }
            val heightPx = with(density) { heightDp.toPx() }

            val androidColors = wallpaper.colors.map { it.toArgb() }.toIntArray()

            val brush = remember(wallpaper.colors, previewType, angleDeg, widthPx, heightPx) {
                createBrushForPreview(wallpaper.colors, previewType, widthPx, heightPx, angleDeg)
            }

            if (previewType == GradientType.Angular) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val sweep = createRotatedSweepShader(size.width, size.height, androidColors, angleDeg)
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        shader = sweep
                    }
                    drawContext.canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)

                    // noise
                    if (addNoise) {
                        val noiseSize = 1.dp.toPx().coerceAtLeast(1f)
                        val numNoisePoints = (size.width * size.height / (noiseSize * noiseSize) * 0.02f).toInt()
                        repeat(numNoisePoints) {
                            val x = kotlin.random.Random.nextFloat() * size.width
                            val y = kotlin.random.Random.nextFloat() * size.height
                            val alpha = kotlin.random.Random.nextFloat() * 0.15f
                            drawCircle(Color.White.copy(alpha = alpha), radius = noiseSize, center = Offset(x, y))
                        }
                    }

                    // stripes
                    if (addStripes) {
                        val stripeCount = 18
                        val stripeWidth = size.width / (stripeCount * 2f)
                        for (i in 0 until stripeCount) {
                            val left = i * stripeWidth * 2f
                            drawRect(Color.White.copy(alpha = 0.10f), topLeft = Offset(left, 0f), size = Size(stripeWidth, size.height))
                        }
                    }
                }

                if (addOverlay) {
                    Image(painter = painterResource(id = R.drawable.overlay_stripes), contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
                }
            } else {
                Box(modifier = Modifier.matchParentSize().background(brush)) {
                    if (addNoise) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val noiseSize = 1.dp.toPx().coerceAtLeast(1f)
                            val numNoisePoints = (size.width * size.height / (noiseSize * noiseSize) * 0.02f).toInt()
                            repeat(numNoisePoints) {
                                val x = kotlin.random.Random.nextFloat() * size.width
                                val y = kotlin.random.Random.nextFloat() * size.height
                                val alpha = kotlin.random.Random.nextFloat() * 0.15f
                                drawCircle(Color.White.copy(alpha = alpha), radius = noiseSize, center = Offset(x, y))
                            }
                        }
                    }

                    if (addStripes) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val stripeCount = 18
                            val stripeWidth = size.width / (stripeCount * 2f)
                            for (i in 0 until stripeCount) {
                                val left = i * stripeWidth * 2f
                                drawRect(Color.White.copy(alpha = 0.10f), topLeft = Offset(left, 0f), size = Size(stripeWidth, size.height))
                            }
                        }
                    }

                    if (addOverlay) {
                        Image(painter = painterResource(id = R.drawable.overlay_stripes), contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
                    }
                }
            }

            // bottom tag
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.36f), shape = RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = previewType.name.lowercase().replaceFirstChar { it.uppercase() }, color = Color.White)
                Spacer(Modifier.width(8.dp))
                wallpaper.colors.forEachIndexed { index, color ->
                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
                    if (index != wallpaper.colors.lastIndex) Spacer(Modifier.width(6.dp))
                }
            }
        }
    }
}

/* --- Small UI helpers kept compact --- */

@Composable
private fun GradientTypeItemFull(label: String, selected: Boolean, textColor: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        tonalElevation = if (selected) 6.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)))
            Spacer(Modifier.width(12.dp))
            Text(label, color = textColor)
        }
    }
}

@Composable
private fun GradientTypeItemRect(label: String, selected: Boolean, textColor: Color, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, tween(220))
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        tonalElevation = if (selected) 6.dp else 0.dp,
        border = if (!selected) ButtonDefaults.outlinedButtonBorder else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, color = textColor, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
    }
}

@Composable
private fun EffectChip(label: String, selected: Boolean, textColor: Color, onToggle: () -> Unit) {
    val unselectedBg = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) MaterialTheme.colorScheme.surface.copy(alpha = 0.06f) else Color.Transparent
    val targetBg = if (selected) MaterialTheme.colorScheme.primaryContainer else unselectedBg
    val bg by animateColorAsState(targetBg, tween(220))
    val elev by androidx.compose.animation.core.animateDpAsState(if (selected) 6.dp else 0.dp)
    Surface(shape = RoundedCornerShape(999.dp), tonalElevation = elev, color = bg, border = if (!selected) ButtonDefaults.outlinedButtonBorder else null) {
        Row(modifier = Modifier.clickable { onToggle() }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = textColor)
        }
    }
}

@Composable
private fun EffectOpacitySlider(label: String, value: Float,
                                onSliderChange: (Float) -> Unit, labelColor: Color) {
    Text(label, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, color = labelColor)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(id = R.string.preview_off), modifier = Modifier.width(40.dp), color = labelColor)
        Slider(value = value, onValueChange = { onSliderChange(it.coerceIn(0f, 1f)) }, modifier = Modifier.weight(1f), valueRange = 0f..1f)
        Text(stringResource(id = R.string.preview_high), modifier = Modifier.width(40.dp), color = labelColor)
    }
}

@Composable
private fun DeviceFrame(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f), RoundedCornerShape(14.dp))) {
        Box(modifier = Modifier.padding(6.dp)) { content() }
        Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp).width(48.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.06f)))
    }
}
