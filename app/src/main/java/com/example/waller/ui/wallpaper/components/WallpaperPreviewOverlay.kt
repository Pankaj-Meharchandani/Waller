/**
 * WallpaperPreviewOverlay — Centered preview + gradient block.
 * Background slightly less transparent, individual opacity sliders always visible.
 *
 * Behavior:
 * - Sliders are always shown but may be at 0 when effect is off.
 * - Moving a slider from 0 -> >0 will enable that effect automatically.
 * - Tapping an effect chip toggles the effect; enabling moves the slider to a default high value.
 */

@file:Suppress("DEPRECATION")

package com.example.waller.ui.wallpaper.components

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.waller.R
import com.example.waller.ui.wallpaper.ApplyDownloadDialog
import com.example.waller.ui.wallpaper.Wallpaper
import kotlinx.coroutines.CoroutineScope

enum class GradientType { ANGULAR, LINEAR, DIAMOND, RADIAL }

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun WallpaperPreviewOverlay(
    wallpaper: Wallpaper,
    isPortrait: Boolean,
    isFavorite: Boolean,
    globalNoise: Boolean,
    globalStripes: Boolean,
    globalOverlay: Boolean,
    onFavoriteToggle: (noise: Boolean, stripes: Boolean, overlay: Boolean) -> Unit,
    onDismiss: () -> Unit,
    writePermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    context: Context,
    coroutineScope: CoroutineScope
) {
    // local selected flags (start from shared/global values)
    var noise by remember { mutableStateOf(globalNoise) }
    var stripes by remember { mutableStateOf(globalStripes) }
    var overlay by remember { mutableStateOf(globalOverlay) }

    // alpha values (0..1). Defaults used when enabling from off.
    var noiseAlpha by remember { mutableFloatStateOf(if (noise) 1f else 0f) }
    var stripesAlpha by remember { mutableFloatStateOf(if (stripes) 1f else 0f) }
    var overlayAlpha by remember { mutableFloatStateOf(if (overlay) 1f else 0f) }

    // default "high" values to move slider to when enabling via chip
    val DEFAULT_NOISE = 1f
    val DEFAULT_STRIPES = 1f
    val DEFAULT_OVERLAY = 1f

    var selectedGradient by remember(wallpaper) {
        mutableStateOf(
            when (wallpaper.type.name.lowercase()) {
                "angular" -> GradientType.ANGULAR
                "radial"  -> GradientType.RADIAL
                "diamond" -> GradientType.DIAMOND
                else      -> GradientType.LINEAR
            }
        )
    }
    var gradientAngle by remember { mutableFloatStateOf(45f) }

    var showApplyDialog by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    BackHandler { onDismiss() }

    val statusBarPadding: Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val aspectRatio = if (isPortrait) 9f / 16f else 16f / 9f

    // Background scrim
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
            .background(Color.Black.copy(alpha = 0.75f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.32f)),
                        radius = 1000f
                    )
                )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                modifier = Modifier.height(46.dp)
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(46.dp)) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.preview_close))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(
                onClick = {
                    if (!isBusy) showApplyDialog = true
                },
                modifier = Modifier.height(44.dp)
            ) {
                Text(stringResource(id = R.string.preview_done))
            }
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Row: Preview + Gradient options
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val previewWidth = (screenWidth * 0.36f).coerceAtMost(420.dp)

                // Preview box
                Box(
                    modifier = Modifier
                        .width(previewWidth)
                        .aspectRatio(aspectRatio)
                        .clip(RoundedCornerShape(14.dp))
                        .shadow(6.dp)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    DeviceFrame(modifier = Modifier.fillMaxSize()) {
                        WallpaperItemCard(
                            wallpaper = wallpaper,
                            isPortrait = isPortrait,
                            addNoise = noise,
                            addStripes = stripes,
                            addOverlay = overlay,
                            isFavorite = isFavorite,
                            onFavoriteToggle = { onFavoriteToggle(noise, stripes, overlay) },
                            onClick = {},
                            isPreview = true
                        )

                        androidx.compose.animation.AnimatedVisibility(
                            visible = isBusy,
                            enter = fadeIn(), exit = fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                                    .padding(6.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Gradient panel
                Column(
                    modifier = Modifier
                        .widthIn(min = 180.dp, max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(stringResource(id = R.string.gradient_style_title), fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GradientTypeItem(stringResource(id = R.string.gradient_style_angular), selectedGradient == GradientType.ANGULAR) {
                            selectedGradient = GradientType.ANGULAR
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        GradientTypeItem(stringResource(id = R.string.gradient_style_linear), selectedGradient == GradientType.LINEAR) {
                            selectedGradient = GradientType.LINEAR
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        GradientTypeItem(stringResource(id = R.string.gradient_style_diamond), selectedGradient == GradientType.DIAMOND) {
                            selectedGradient = GradientType.DIAMOND
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        GradientTypeItem(stringResource(id = R.string.gradient_style_radial), selectedGradient == GradientType.RADIAL) {
                            selectedGradient = GradientType.RADIAL
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${gradientAngle.toInt()}°", modifier = Modifier.width(44.dp))
                        Slider(
                            value = gradientAngle,
                            onValueChange = { gradientAngle = it },
                            valueRange = 0f..360f,
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

            Spacer(modifier = Modifier.height(18.dp))

            // Effects center row (chips)
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.06f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // NOTE: chips toggle selection AND also update slider value accordingly
                EffectChip(stringResource(id = R.string.preview_effect_nothing), overlay) {
                    // toggle overlay
                    overlay = !overlay
                    if (overlay) {
                        if (overlayAlpha <= 0f) overlayAlpha = DEFAULT_OVERLAY
                    } else {
                        overlayAlpha = 0f
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                EffectChip(stringResource(id = R.string.preview_effect_snow), noise) {
                    noise = !noise
                    if (noise) {
                        if (noiseAlpha <= 0f) noiseAlpha = DEFAULT_NOISE
                    } else {
                        noiseAlpha = 0f
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                EffectChip(stringResource(id = R.string.preview_effect_stripes), stripes) {
                    stripes = !stripes
                    if (stripes) {
                        if (stripesAlpha <= 0f) stripesAlpha = DEFAULT_STRIPES
                    } else {
                        stripesAlpha = 0f
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Effect sliders: ALWAYS VISIBLE. Changing slider enables/disables corresponding effect.
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Overlay slider (Nothing)
                EffectOpacitySlider(
                    label = stringResource(id = R.string.preview_opacity_nothing),
                    value = overlayAlpha,
                    selected = overlay,
                    onSliderChange = { v ->
                        overlayAlpha = v
                        // any non-zero slider -> enable effect
                        overlay = v > 0.0001f
                    }
                )

                // Noise slider
                EffectOpacitySlider(
                    label = stringResource(id = R.string.preview_opacity_snow),
                    value = noiseAlpha,
                    selected = noise,
                    onSliderChange = { v ->
                        noiseAlpha = v
                        noise = v > 0.0001f
                    }
                )

                // Stripes slider
                EffectOpacitySlider(
                    label = stringResource(id = R.string.preview_opacity_stripes),
                    value = stripesAlpha,
                    selected = stripes,
                    onSliderChange = { v ->
                        stripesAlpha = v
                        stripes = v > 0.0001f
                    }
                )
            }
        }

        if (showApplyDialog) {
            ApplyDownloadDialog(
                show = true,
                wallpaper = wallpaper,
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

/** Slider row that reports slider changes and exposes the current selected state */
@Composable
private fun EffectOpacitySlider(
    label: String,
    value: Float,
    selected: Boolean,
    onSliderChange: (Float) -> Unit
) {
    Text(label, fontWeight = FontWeight.Medium)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(id = R.string.preview_off), modifier = Modifier.width(40.dp))
        Slider(
            value = value,
            onValueChange = { onSliderChange(it.coerceIn(0f, 1f)) },
            modifier = Modifier.weight(1f),
            valueRange = 0f..1f
        )

        // Show "High" on the right (keeps same small width)
        Text(stringResource(id = R.string.preview_high), modifier = Modifier.width(40.dp))
    }
}

@Composable
private fun GradientTypeItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor =
        if (selected && bg.luminance() > 0.5f) Color.Black else MaterialTheme.colorScheme.onSurface

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        tonalElevation = if (selected) 6.dp else 0.dp,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, color = contentColor)
        }
    }
}

@Composable
private fun DeviceFrame(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
    ) {
        Box(modifier = Modifier.padding(6.dp)) {
            content()
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
                .width(48.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.06f))
        )
    }
}

@Composable
private fun EffectChip(label: String, selected: Boolean, onToggle: () -> Unit) {
    val unselectedBg =
        if (MaterialTheme.colorScheme.background.luminance() > 0.5f)
            MaterialTheme.colorScheme.surface.copy(alpha = 0.06f)
        else Color.Transparent

    val targetBg =
        if (selected) MaterialTheme.colorScheme.primaryContainer else unselectedBg

    val bg by animateColorAsState(targetBg, tween(220))

    val contentColor =
        if (selected && bg.luminance() > 0.5f) Color.Black else Color.White

    val elev by androidx.compose.animation.core.animateDpAsState(if (selected) 6.dp else 0.dp)

    Surface(
        shape = RoundedCornerShape(999.dp),
        tonalElevation = elev,
        color = bg,
        border = if (!selected) ButtonDefaults.outlinedButtonBorder else null
    ) {
        Row(
            modifier = Modifier.clickable { onToggle() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = contentColor)
        }
    }
}
