/**
 * Fullscreen overlay for previewing a wallpaper before saving or applying.
 * Shows a live device-frame preview with controls for gradient style, angle,
 * and visual effects such as noise, stripes, and overlay opacity.
 * Designed to provide an interactive, real-time editing experience
 * while keeping the UI consistent in both portrait and landscape modes.
 */

@file:Suppress("DEPRECATION")

package com.example.waller.ui.wallpaper.components

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.text.style.TextAlign
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
    // helper for text color rules
    @Composable
    fun overlayTextColor(selectedForButton: Boolean = false): Color {
        val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
        return if (isLightTheme && selectedForButton) Color.Black else Color.White
    }

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

    // auto-select gradient based on wallpaper.type
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
                    Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.preview_close), tint = overlayTextColor())
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(
                onClick = {
                    if (!isBusy) showApplyDialog = true
                },
                modifier = Modifier.height(44.dp)
            ) {
                Text(stringResource(id = R.string.preview_done), color = overlayTextColor())
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
            // Responsive placement: portrait = side-by-side, landscape = stacked
            if (isPortrait) {
                // Portrait: left preview + right control panel (vertical list of gradient items)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val previewWidth = (screenWidth * 0.36f).coerceAtMost(420.dp)

                    // Preview box (portrait)
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

                            if (isBusy) {
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
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Gradient panel (portrait)
                    Column(
                        modifier = Modifier
                            .widthIn(min = 180.dp, max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(stringResource(id = R.string.gradient_style_title), style = MaterialTheme.typography.titleMedium, color = overlayTextColor())
                        Spacer(modifier = Modifier.height(8.dp))

                        // Portrait: full width, vertical order (Linear, Radial, Angular, Diamond)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            GradientTypeItemFull(
                                label = stringResource(id = R.string.gradient_style_linear),
                                selected = selectedGradient == GradientType.LINEAR,
                                textColor = overlayTextColor(selectedForButton = selectedGradient == GradientType.LINEAR)
                            ) {
                                selectedGradient = GradientType.LINEAR
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }

                            GradientTypeItemFull(
                                label = stringResource(id = R.string.gradient_style_radial),
                                selected = selectedGradient == GradientType.RADIAL,
                                textColor = overlayTextColor(selectedForButton = selectedGradient == GradientType.RADIAL)
                            ) {
                                selectedGradient = GradientType.RADIAL
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }

                            GradientTypeItemFull(
                                label = stringResource(id = R.string.gradient_style_angular),
                                selected = selectedGradient == GradientType.ANGULAR,
                                textColor = overlayTextColor(selectedForButton = selectedGradient == GradientType.ANGULAR)
                            ) {
                                selectedGradient = GradientType.ANGULAR
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }

                            GradientTypeItemFull(
                                label = stringResource(id = R.string.gradient_style_diamond),
                                selected = selectedGradient == GradientType.DIAMOND,
                                textColor = overlayTextColor(selectedForButton = selectedGradient == GradientType.DIAMOND)
                            ) {
                                selectedGradient = GradientType.DIAMOND
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${gradientAngle.toInt()}°", modifier = Modifier.width(44.dp), color = overlayTextColor())
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
            } else {
                // Landscape: preview top, controls below. Gradient chips are hug-content, centered, with more padding & spacing.
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

                            if (isBusy) {
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
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Gradient title + centered hug-row of chips
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(id = R.string.gradient_style_title), style = MaterialTheme.typography.titleMedium, color = overlayTextColor())
                        Spacer(modifier = Modifier.height(12.dp))

                        // Centered row with hugging chips & slightly larger gaps/padding
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(horizontal = 8.dp)
                            ) {
                                GradientTypeItemRect(
                                    label = stringResource(id = R.string.gradient_style_linear),
                                    selected = selectedGradient == GradientType.LINEAR,
                                    textColor = overlayTextColor(selectedForButton = selectedGradient == GradientType.LINEAR)
                                ) {
                                    selectedGradient = GradientType.LINEAR
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }

                                GradientTypeItemRect(
                                    label = stringResource(id = R.string.gradient_style_radial),
                                    selected = selectedGradient == GradientType.RADIAL,
                                    textColor = overlayTextColor(selectedForButton = selectedGradient == GradientType.RADIAL)
                                ) {
                                    selectedGradient = GradientType.RADIAL
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }

                                GradientTypeItemRect(
                                    label = stringResource(id = R.string.gradient_style_angular),
                                    selected = selectedGradient == GradientType.ANGULAR,
                                    textColor = overlayTextColor(selectedForButton = selectedGradient == GradientType.ANGULAR)
                                ) {
                                    selectedGradient = GradientType.ANGULAR
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }

                                GradientTypeItemRect(
                                    label = stringResource(id = R.string.gradient_style_diamond),
                                    selected = selectedGradient == GradientType.DIAMOND,
                                    textColor = overlayTextColor(selectedForButton = selectedGradient == GradientType.DIAMOND)
                                ) {
                                    selectedGradient = GradientType.DIAMOND
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${gradientAngle.toInt()}°", modifier = Modifier.width(44.dp), color = overlayTextColor())
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
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Effects center row (chips)
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.06f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EffectChip(stringResource(id = R.string.preview_effect_nothing), overlay, textColor = overlayTextColor(selectedForButton = overlay)) {
                        overlay = !overlay
                        if (overlay && overlayAlpha <= 0f) overlayAlpha = DEFAULT_OVERLAY
                        if (!overlay) overlayAlpha = 0f
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    EffectChip(stringResource(id = R.string.preview_effect_snow), noise, textColor = overlayTextColor(selectedForButton = noise)) {
                        noise = !noise
                        if (noise && noiseAlpha <= 0f) noiseAlpha = DEFAULT_NOISE
                        if (!noise) noiseAlpha = 0f
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    EffectChip(stringResource(id = R.string.preview_effect_stripes), stripes, textColor = overlayTextColor(selectedForButton = stripes)) {
                        stripes = !stripes
                        if (stripes && stripesAlpha <= 0f) stripesAlpha = DEFAULT_STRIPES
                        if (!stripes) stripesAlpha = 0f
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
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
                        overlay = v > 0.0001f
                    },
                    labelColor = overlayTextColor()
                )

                // Noise slider
                EffectOpacitySlider(
                    label = stringResource(id = R.string.preview_opacity_snow),
                    value = noiseAlpha,
                    selected = noise,
                    onSliderChange = { v ->
                        noiseAlpha = v
                        noise = v > 0.0001f
                    },
                    labelColor = overlayTextColor()
                )

                // Stripes slider
                EffectOpacitySlider(
                    label = stringResource(id = R.string.preview_opacity_stripes),
                    value = stripesAlpha,
                    selected = stripes,
                    onSliderChange = { v ->
                        stripesAlpha = v
                        stripes = v > 0.0001f
                    },
                    labelColor = overlayTextColor()
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

/** Portrait: full-width gradient list item (used in portrait side panel) */
@Composable
private fun GradientTypeItemFull(label: String, selected: Boolean, textColor: Color, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        tonalElevation = if (selected) 6.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
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
            Text(label, color = textColor)
        }
    }
}

/** Landscape: rectangular hugging button — content-sized, centered, increased padding & gap slightly */
@Composable
private fun GradientTypeItemRect(label: String, selected: Boolean, textColor: Color, onClick: () -> Unit) {
    val targetBg = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val bg by animateColorAsState(targetBg, tween(220))
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        tonalElevation = if (selected) 6.dp else 0.dp,
        border = if (!selected) ButtonDefaults.outlinedButtonBorder else null,
        modifier = Modifier
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 1
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
    onSliderChange: (Float) -> Unit,
    labelColor: Color
) {
    Text(label, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, color = labelColor)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(id = R.string.preview_off), modifier = Modifier.width(40.dp), color = labelColor)
        Slider(
            value = value,
            onValueChange = { onSliderChange(it.coerceIn(0f, 1f)) },
            modifier = Modifier.weight(1f),
            valueRange = 0f..1f
        )
        Text(stringResource(id = R.string.preview_high), modifier = Modifier.width(40.dp), color = labelColor)
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
private fun EffectChip(label: String, selected: Boolean, textColor: Color, onToggle: () -> Unit) {
    val unselectedBg =
        if (MaterialTheme.colorScheme.background.luminance() > 0.5f)
            MaterialTheme.colorScheme.surface.copy(alpha = 0.06f)
        else Color.Transparent

    val targetBg =
        if (selected) MaterialTheme.colorScheme.primaryContainer else unselectedBg

    val bg by animateColorAsState(targetBg, tween(220))

    val elev by androidx.compose.animation.core.animateDpAsState(if (selected) 6.dp else 0.dp)

    Surface(
        shape = RoundedCornerShape(999.dp),
        tonalElevation = elev,
        color = bg,
        border = if (!selected) ButtonDefaults.outlinedButtonBorder else null
    ) {
        Row(
            modifier = Modifier.clickable { onToggle() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = textColor)
        }
    }
}
