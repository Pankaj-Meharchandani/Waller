/**
 * WallpaperPreviewOverlay.kt
 *
 * Fullscreen preview screen for wallpapers.
 *
 * Responsibilities:
 * - Owns preview UI state (gradient, angle, effects, opacity)
 * - Handles portrait / landscape layout differences
 * - Wires user actions to apply, download, and favourite logic
 *
 * This file coordinates the preview feature but does not perform
 * low-level rendering or drawing.
 */

@file:Suppress("DEPRECATION", "COMPOSE_APPLIER_CALL_MISMATCH")
package com.example.waller.ui.wallpaper.components.previewOverlay

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.waller.R
import com.example.waller.ui.wallpaper.ApplyDownloadDialog
import com.example.waller.ui.wallpaper.Wallpaper
import com.example.waller.ui.wallpaper.GradientType
import kotlinx.coroutines.CoroutineScope
import kotlin.math.abs

private enum class EffectType { OVERLAY, NOISE, STRIPES, GEOMETRIC }

private data class EffectConfig(
    val type: EffectType,
    val labelRes: Int,
    val isEnabled: () -> Boolean,
    val setEnabled: (Boolean) -> Unit,
    val alpha: () -> Float,
    val setAlpha: (Float) -> Unit
)

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun WallpaperPreviewOverlay(
    wallpaper: Wallpaper,
    isPortrait: Boolean,
    isFavorite: Boolean,
    globalNoise: Boolean,
    globalStripes: Boolean,
    globalOverlay: Boolean,
    globalGeometric: Boolean,
    initialNoiseAlpha: Float = initAlphaFor(globalNoise, DEFAULT_NOISE_ALPHA),
    initialStripesAlpha: Float = initAlphaFor(globalStripes, DEFAULT_STRIPES_ALPHA),
    initialOverlayAlpha: Float = initAlphaFor(globalOverlay, DEFAULT_OVERLAY_ALPHA),
    initialGeometricAlpha: Float = initAlphaFor(globalGeometric, DEFAULT_GEOMETRIC_ALPHA),
    onFavoriteToggle: (
        wallpaper: Wallpaper,
        noise: Boolean,
        stripes: Boolean,
        overlay: Boolean,
        geometric: Boolean,
        noiseAlpha: Float,
        stripesAlpha: Float,
        overlayAlpha: Float,
        geometricAlpha: Float
    ) -> Unit,
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
    var geometric by remember { mutableStateOf(globalGeometric) }
    var activeEffect by remember { mutableStateOf<EffectType?>(null) }

    // per-effect opacity state (already present, just used more thoroughly now)
    var noiseAlpha by remember { mutableFloatStateOf(initialNoiseAlpha) }
    var stripesAlpha by remember { mutableFloatStateOf(initialStripesAlpha) }
    var overlayAlpha by remember { mutableFloatStateOf(initialOverlayAlpha) }
    var geometricAlpha by remember { mutableFloatStateOf(initialGeometricAlpha) }

    val effects = listOf(
        EffectConfig(
            type = EffectType.OVERLAY,
            labelRes = R.string.preview_effect_nothing,
            isEnabled = { overlay },
            setEnabled = { overlay = it },
            alpha = { overlayAlpha },
            setAlpha = { overlayAlpha = it }
        ),
        EffectConfig(
            type = EffectType.NOISE,
            labelRes = R.string.preview_effect_snow,
            isEnabled = { noise },
            setEnabled = { noise = it },
            alpha = { noiseAlpha },
            setAlpha = { noiseAlpha = it }
        ),
        EffectConfig(
            type = EffectType.STRIPES,
            labelRes = R.string.preview_effect_stripes,
            isEnabled = { stripes },
            setEnabled = { stripes = it },
            alpha = { stripesAlpha },
            setAlpha = { stripesAlpha = it }
        ),
        EffectConfig(
            type = EffectType.GEOMETRIC,
            labelRes = R.string.effect_geometric,
            isEnabled = { geometric },
            setEnabled = { geometric = it },
            alpha = { geometricAlpha },
            setAlpha = { geometricAlpha = it }
        )
    )


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

    // angle initialized from wallpaper (so overlay respects stored angle)
    var gradientAngle by remember(wallpaper) { mutableFloatStateOf(wallpaper.angleDeg) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        if (activeEffect == null) {
            activeEffect = when {
                overlay -> EffectType.OVERLAY
                noise -> EffectType.NOISE
                stripes -> EffectType.STRIPES
                geometric -> EffectType.GEOMETRIC
                else -> null
            }
        }
    }

    BackHandler { onDismiss() }

    val statusBarPadding: Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val aspectRatio = if (isPortrait) 9f / 16f else 16f / 9f

    // preview snapshot that reflects current style + angle
    val previewWallpaper = remember(wallpaper, selectedGradient, gradientAngle) {
        wallpaper.copy(type = selectedGradient, angleDeg = gradientAngle)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // scrim (captures outside taps)
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                        }
                    }
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
                        PreviewFrame(
                            previewWallpaper = previewWallpaper,
                            selectedGradient = selectedGradient,
                            gradientAngle = gradientAngle,
                            isFavorite = isFavorite,
                            isBusy = isBusy,
                            addNoise = noise,
                            addStripes = stripes,
                            addOverlay = overlay,
                            addGeometric = geometric,
                            noiseAlpha = noiseAlpha,
                            stripesAlpha = stripesAlpha,
                            overlayAlpha = overlayAlpha,
                            geometricAlpha = geometricAlpha,
                            overlayTextColor = { overlayTextColor() }
                        ) {
                            onFavoriteToggle(
                                previewWallpaper,
                                noise,
                                stripes,
                                overlay,
                                geometric,
                                noiseAlpha,
                                stripesAlpha,
                                overlayAlpha,
                                geometricAlpha
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(
                        modifier = Modifier
                            .widthIn(min = 180.dp, max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            stringResource(id = R.string.gradient_style_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = overlayTextColor()
                        )
                        Spacer(Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            GradientTypeItemFull(
                                label = stringResource(id = R.string.gradient_style_linear),
                                selected = selectedGradient == GradientType.Linear,
                                textColor = overlayTextColor(selectedForButton = selectedGradient == GradientType.Linear)
                            ) {
                                selectedGradient = GradientType.Linear
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }

                            GradientTypeItemFull(
                                label = stringResource(id = R.string.gradient_style_radial),
                                selected = selectedGradient == GradientType.Radial,
                                textColor = overlayTextColor(selectedForButton = selectedGradient == GradientType.Radial)
                            ) {
                                selectedGradient = GradientType.Radial
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }

                            GradientTypeItemFull(
                                label = stringResource(id = R.string.gradient_style_angular),
                                selected = selectedGradient == GradientType.Angular,
                                textColor = overlayTextColor(selectedForButton = selectedGradient == GradientType.Angular)
                            ) {
                                selectedGradient = GradientType.Angular
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }

                            GradientTypeItemFull(
                                label = stringResource(id = R.string.gradient_style_diamond),
                                selected = selectedGradient == GradientType.Diamond,
                                textColor = overlayTextColor(selectedForButton = selectedGradient == GradientType.Diamond)
                            ) {
                                selectedGradient = GradientType.Diamond
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${gradientAngle.toInt()}°",
                                modifier = Modifier.width(44.dp),
                                color = overlayTextColor()
                            )
                            Slider(
                                value = gradientAngle,
                                onValueChange = { gradientAngle = it },
                                valueRange = 0f..360f,
                                onValueChangeFinished = {
                                    val checkpoints = listOf(0f, 90f, 180f, 270f)
                                    val nearest = checkpoints.minByOrNull { abs(it - gradientAngle) } ?: 0f
                                    if (abs(nearest - gradientAngle) <= 8f) {
                                        gradientAngle = nearest
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
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
                        PreviewFrame(
                            previewWallpaper = previewWallpaper,
                            selectedGradient = selectedGradient,
                            gradientAngle = gradientAngle,
                            isFavorite = isFavorite,
                            isBusy = isBusy,
                            addNoise = noise,
                            addStripes = stripes,
                            addOverlay = overlay,
                            addGeometric = geometric,
                            noiseAlpha = noiseAlpha,
                            stripesAlpha = stripesAlpha,
                            overlayAlpha = overlayAlpha,
                            geometricAlpha = geometricAlpha,
                            overlayTextColor = { overlayTextColor() }
                        ) {
                            onFavoriteToggle(
                                previewWallpaper,
                                noise,
                                stripes,
                                overlay,
                                geometric,
                                noiseAlpha,
                                stripesAlpha,
                                overlayAlpha,
                                geometricAlpha
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Chips row (compact)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(id = R.string.gradient_style_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = overlayTextColor()
                        )
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
                            Text(
                                "${gradientAngle.toInt()}°",
                                modifier = Modifier.width(44.dp),
                                color = overlayTextColor()
                            )
                            Slider(
                                value = gradientAngle,
                                onValueChange = { gradientAngle = it },
                                valueRange = 0f..360f,
                                onValueChangeFinished = {
                                    val checkpoints = listOf(0f, 90f, 180f, 270f)
                                    val nearest = checkpoints.minByOrNull { abs(it - gradientAngle) } ?: 0f
                                    if (abs(nearest - gradientAngle) <= 8f) {
                                        gradientAngle = nearest
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    effects.forEach { effect ->
                        EffectChip(
                            label = stringResource(effect.labelRes),
                            selected = effect.isEnabled(),
                            fillProgress = effect.alpha(),
                            isActive = activeEffect == effect.type,
                            textColor = overlayTextColor(selectedForButton = effect.isEnabled()),
                            modifier = Modifier.weight(1f)
                        ) {
                            when {
                                !effect.isEnabled() -> {
                                    effect.setEnabled(true)
                                    activeEffect = effect.type
                                }
                                activeEffect != effect.type -> {
                                    activeEffect = effect.type
                                }
                                else -> {
                                    effect.setEnabled(false)
                                    effect.setAlpha(0f)
                                    activeEffect = null
                                }
                            }
                        }
                    }

                }
            }

            Spacer(Modifier.height(12.dp))

// Active effect slider (only ONE at a time)
            when (activeEffect) {

                EffectType.OVERLAY -> {
                    EffectOpacitySlider(
                        label = stringResource(id = R.string.preview_opacity_nothing),
                        value = overlayAlpha,
                        onSliderChange = {
                            overlayAlpha = it
                            overlay = it > 0.001f
                        },
                        labelColor = overlayTextColor()
                    )
                }

                EffectType.NOISE -> {
                    EffectOpacitySlider(
                        label = stringResource(id = R.string.preview_opacity_snow),
                        value = noiseAlpha,
                        onSliderChange = {
                            noiseAlpha = it
                            noise = it > 0.001f
                        },
                        labelColor = overlayTextColor()
                    )
                }

                EffectType.STRIPES -> {
                    EffectOpacitySlider(
                        label = stringResource(id = R.string.preview_opacity_stripes),
                        value = stripesAlpha,
                        onSliderChange = {
                            stripesAlpha = it
                            stripes = it > 0.001f
                        },
                        labelColor = overlayTextColor()
                    )
                }

                EffectType.GEOMETRIC -> {
                    EffectOpacitySlider(
                        label = stringResource(id = R.string.preview_opacity_geometric),
                        value = geometricAlpha,
                        onSliderChange = {
                            geometricAlpha = it
                            geometric = it > 0.001f
                        },
                        labelColor = overlayTextColor()
                    )
                }

                else -> Unit
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
                addGeometric = geometric,
                noiseAlpha = noiseAlpha,
                stripesAlpha = stripesAlpha,
                overlayAlpha = overlayAlpha,
                geometricAlpha = geometricAlpha,
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
private fun PreviewFrame(
    previewWallpaper: Wallpaper,
    selectedGradient: GradientType,
    gradientAngle: Float,
    isFavorite: Boolean,
    isBusy: Boolean,
    addNoise: Boolean,
    addStripes: Boolean,
    addOverlay: Boolean,
    addGeometric: Boolean,
    noiseAlpha: Float,
    stripesAlpha: Float,
    overlayAlpha: Float,
    geometricAlpha: Float,
    overlayTextColor: @Composable () -> Color,
    onFavoriteToggle: () -> Unit
) {
    DeviceFrame(modifier = Modifier.fillMaxSize()) {

        PreviewWallpaperRender(
            wallpaper = previewWallpaper,
            previewType = selectedGradient,
            angleDeg = gradientAngle,
            addNoise = addNoise,
            addStripes = addStripes,
            addOverlay = addOverlay,
            addGeometric = addGeometric,
            noiseAlpha = noiseAlpha,
            stripesAlpha = stripesAlpha,
            overlayAlpha = overlayAlpha,
            geometricAlpha = geometricAlpha,
            modifier = Modifier.fillMaxSize(),
            showTypeLabel = false
        )

        var localFav by remember { mutableStateOf(isFavorite) }

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
                IconButton(onClick = {
                    localFav = !localFav
                    onFavoriteToggle()
                }) {
                    Icon(
                        imageVector = if (localFav)
                            Icons.Filled.Favorite
                        else
                            Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
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
                strokeWidth = 2.dp
            )
        }
    }
}