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
 * Effect state is held in a single EffectMap — no per-effect state vars.
 * Adding a new effect requires NO change here.
 */

@file:Suppress("DEPRECATION", "COMPOSE_APPLIER_CALL_MISMATCH")
package com.example.waller.ui.wallpaper.components.previewOverlay

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.waller.R
import com.example.waller.ui.wallpaper.ApplyDownloadDialog
import com.example.waller.ui.wallpaper.EffectMap
import com.example.waller.ui.wallpaper.EffectState
import com.example.waller.ui.wallpaper.FavoriteWallpaper
import com.example.waller.ui.wallpaper.GradientType
import com.example.waller.ui.wallpaper.Haptics
import com.example.waller.ui.wallpaper.InteractionMode
import com.example.waller.ui.wallpaper.Wallpaper
import com.example.waller.ui.wallpaper.WallpaperEffects
import com.example.waller.ui.wallpaper.alpha
import com.example.waller.ui.wallpaper.isEnabled
import com.example.waller.ui.wallpaper.withAlpha
import com.example.waller.ui.wallpaper.withEnabled
import kotlinx.coroutines.CoroutineScope
import kotlin.math.abs
import androidx.compose.ui.res.stringResource as str

/** Inline labels for effects whose labelRes == 0. */
private val EFFECT_PREVIEW_LABELS = mapOf(
    "blur" to "Blur"
)

/** Opacity slider labels per effect id. */
private val EFFECT_SLIDER_LABELS = mapOf(
    "overlay"   to R.string.preview_opacity_nothing,
    "noise"     to R.string.preview_opacity_snow,
    "stripes"   to R.string.preview_opacity_stripes,
    "geometric" to R.string.preview_opacity_geometric
)
private val EFFECT_SLIDER_INLINE = mapOf(
    "blur" to "Blur Intensity"
)

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun WallpaperPreviewOverlay(
    wallpaper: Wallpaper,
    isPortrait: Boolean,
    isFavorite: Boolean,
    /** Initial effect state to seed the preview from the global toggles. */
    initialEffects: EffectMap,
    onFavoriteToggle: (wallpaper: Wallpaper, effects: EffectMap) -> Unit,
    onDismiss: () -> Unit,
    writePermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    context: Context,
    coroutineScope: CoroutineScope
) {
    @Composable
    fun overlayTextColor(selectedForButton: Boolean = false): Color = Color.White

    val view = LocalView.current

    // Single EffectMap drives all effect state
    var localEffects by remember { mutableStateOf(initialEffects) }
    // Which effect's slider is currently visible (by id), null = none
    var activeEffectId by remember { mutableStateOf<String?>(null) }

    // Init active effect to the first enabled one (mirrors original behaviour)
    LaunchedEffect(Unit) {
        if (activeEffectId == null) {
            activeEffectId = WallpaperEffects.ALL.firstOrNull { localEffects.isEnabled(it.id) }?.id
        }
    }

    var selectedGradient by remember(wallpaper) {
        mutableStateOf(when (wallpaper.type.name.lowercase()) {
            "angular" -> GradientType.Angular
            "radial"  -> GradientType.Radial
            "diamond" -> GradientType.Diamond
            else      -> GradientType.Linear
        })
    }

    var gradientAngle by remember(wallpaper) { mutableFloatStateOf(wallpaper.angleDeg) }
    var lastAngleCheckpoint by remember { mutableStateOf<Int?>(null) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }

    val statusBarPadding: Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val aspectRatio = if (isPortrait) 9f / 16f else 16f / 9f

    val previewWallpaper = remember(wallpaper, selectedGradient, gradientAngle) {
        wallpaper.copy(type = selectedGradient, angleDeg = gradientAngle)
    }

    BackHandler { onDismiss() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent() } } }
                .background(Color.Black.copy(alpha = 0.85f))
        ) {
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.32f)),
                    radius = 1000f
                )
            ))
        }

        // ── Header (X + Apply) ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
                modifier = Modifier.height(46.dp)
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(46.dp)) {
                    Icon(Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.preview_close),
                        tint = overlayTextColor())
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = { if (!isBusy) showApplyDialog = true },
                modifier = Modifier.height(44.dp)
            ) {
                Text(text = stringResource(id = R.string.preview_done), color = overlayTextColor())
            }
        }

        // ── Main content ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isPortrait) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val previewWidth = (screenWidth * 0.36f).coerceAtMost(420.dp)

                    Box(
                        modifier = Modifier
                            .width(previewWidth).aspectRatio(aspectRatio)
                            .clip(RoundedCornerShape(14.dp)).shadow(6.dp)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        PreviewFrame(
                            previewWallpaper = previewWallpaper,
                            selectedGradient = selectedGradient,
                            gradientAngle    = gradientAngle,
                            isFavorite       = isFavorite,
                            isBusy           = isBusy,
                            effects          = localEffects,
                            overlayTextColor = { overlayTextColor() },
                            onFavoriteToggle = { onFavoriteToggle(previewWallpaper, localEffects) }
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(
                        modifier = Modifier
                            .widthIn(min = 180.dp, max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(stringResource(id = R.string.gradient_style_title),
                            style = MaterialTheme.typography.titleMedium, color = overlayTextColor())
                        Spacer(Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(
                                GradientType.Linear  to stringResource(R.string.gradient_style_linear),
                                GradientType.Radial  to stringResource(R.string.gradient_style_radial),
                                GradientType.Angular to stringResource(R.string.gradient_style_angular),
                                GradientType.Diamond to stringResource(R.string.gradient_style_diamond)
                            ).forEach { (type, label) ->
                                GradientTypeItemFull(
                                    label    = label,
                                    selected = selectedGradient == type,
                                    textColor = overlayTextColor(selectedForButton = selectedGradient == type)
                                ) { selectedGradient = type; Haptics.light(view) }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        AngleSliderRow(gradientAngle, overlayTextColor(), view) { v, cp ->
                            gradientAngle = v
                            if (cp != null && cp != lastAngleCheckpoint) {
                                lastAngleCheckpoint = cp
                                Haptics.light(view)
                            }
                        }
                    }
                }
            } else {
                // Landscape
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val previewWidth = (screenWidth * 0.72f).coerceAtMost(900.dp)
                    Box(
                        modifier = Modifier
                            .width(previewWidth).aspectRatio(aspectRatio)
                            .clip(RoundedCornerShape(14.dp)).shadow(6.dp)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        PreviewFrame(
                            previewWallpaper = previewWallpaper,
                            selectedGradient = selectedGradient,
                            gradientAngle    = gradientAngle,
                            isFavorite       = isFavorite,
                            isBusy           = isBusy,
                            effects          = localEffects,
                            overlayTextColor = { overlayTextColor() },
                            onFavoriteToggle = { onFavoriteToggle(previewWallpaper, localEffects) }
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(id = R.string.gradient_style_title),
                            style = MaterialTheme.typography.titleMedium, color = overlayTextColor())
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            listOf(
                                GradientType.Linear  to stringResource(R.string.gradient_style_linear),
                                GradientType.Radial  to stringResource(R.string.gradient_style_radial),
                                GradientType.Angular to stringResource(R.string.gradient_style_angular),
                                GradientType.Diamond to stringResource(R.string.gradient_style_diamond)
                            ).forEach { (type, label) ->
                                GradientTypeItemRect(
                                    label    = label,
                                    selected = selectedGradient == type,
                                    textColor = overlayTextColor(selectedForButton = selectedGradient == type)
                                ) { selectedGradient = type }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        AngleSliderRow(gradientAngle, overlayTextColor(), view) { v, cp ->
                            gradientAngle = v
                            if (cp != null && cp != lastAngleCheckpoint) {
                                lastAngleCheckpoint = cp
                                Haptics.light(view)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Effect chips (loop — no per-effect code) ──────────────────────
            val allDefs = WallpaperEffects.ALL
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.06f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Row 1: first 3 effects
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        allDefs.take(3).forEach { def ->
                            val label = if (def.labelRes != 0) stringResource(def.labelRes)
                            else EFFECT_PREVIEW_LABELS[def.id] ?: def.id
                            EffectChip(
                                label        = label,
                                selected     = localEffects.isEnabled(def.id),
                                fillProgress = localEffects.alpha(def.id),
                                isActive     = activeEffectId == def.id,
                                textColor    = overlayTextColor(selectedForButton = localEffects.isEnabled(def.id)),
                                modifier     = Modifier.weight(1f)
                            ) {
                                localEffects = when {
                                    !localEffects.isEnabled(def.id) -> {
                                        activeEffectId = def.id
                                        localEffects.withEnabled(def.id, true)
                                    }
                                    activeEffectId != def.id -> { activeEffectId = def.id; localEffects }
                                    else -> {
                                        activeEffectId = null
                                        localEffects.withEnabled(def.id, false).withAlpha(def.id, 0f)
                                    }
                                }
                            }
                        }
                    }
                    // Row 2: remaining effects, centered
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(Modifier.weight(0.5f))
                        allDefs.drop(3).forEach { def ->
                            val label = if (def.labelRes != 0) stringResource(def.labelRes)
                            else EFFECT_PREVIEW_LABELS[def.id] ?: def.id
                            EffectChip(
                                label        = label,
                                selected     = localEffects.isEnabled(def.id),
                                fillProgress = localEffects.alpha(def.id),
                                isActive     = activeEffectId == def.id,
                                textColor    = overlayTextColor(selectedForButton = localEffects.isEnabled(def.id)),
                                modifier     = Modifier.weight(1f)
                            ) {
                                localEffects = when {
                                    !localEffects.isEnabled(def.id) -> {
                                        activeEffectId = def.id
                                        localEffects.withEnabled(def.id, true)
                                    }
                                    activeEffectId != def.id -> { activeEffectId = def.id; localEffects }
                                    else -> {
                                        activeEffectId = null
                                        localEffects.withEnabled(def.id, false).withAlpha(def.id, 0f)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.weight(0.5f))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Active effect slider (loop replaces the `when` block) ─────────
            activeEffectId?.let { id ->
                val sliderLabelRes = EFFECT_SLIDER_LABELS[id]
                val sliderLabel = if (sliderLabelRes != null) stringResource(sliderLabelRes)
                else EFFECT_SLIDER_INLINE[id] ?: id

                EffectOpacitySlider(
                    label         = sliderLabel,
                    value         = localEffects.alpha(id),
                    onSliderChange = { v ->
                        localEffects = localEffects
                            .withAlpha(id, v)
                            .withEnabled(id, v > 0.001f)
                    },
                    labelColor = overlayTextColor()
                )
            }
        }

        // ── Apply dialog ──────────────────────────────────────────────────────
        if (showApplyDialog) {
            ApplyDownloadDialog(
                interactionMode     = InteractionMode.ADVANCED,
                show                = true,
                wallpaper           = previewWallpaper,
                isPortrait          = isPortrait,
                effects             = localEffects,
                isWorking           = isBusy,
                onWorkingChange     = { isBusy = it },
                onDismiss           = { showApplyDialog = false },
                writePermissionLauncher = writePermissionLauncher,
                context             = context,
                coroutineScope      = coroutineScope
            )
        }
    }
}

// ── Angle slider extracted to reduce duplication ──────────────────────────────
@Composable
private fun AngleSliderRow(
    gradientAngle: Float,
    labelColor: Color,
    view: android.view.View,
    onAngleChange: (Float, Int?) -> Unit
) {
    var lastCheckpoint by remember { mutableStateOf<Int?>(null) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("${gradientAngle.toInt()}°", modifier = Modifier.width(44.dp), color = labelColor)
        Slider(
            value = gradientAngle,
            onValueChange = { value ->
                val checkpoints = listOf(0, 90, 180, 270, 360)
                val crossed = checkpoints.firstOrNull { abs(it - value) <= 3f }
                onAngleChange(value, if (crossed != null && crossed != lastCheckpoint) crossed else null)
                if (crossed != null) lastCheckpoint = crossed
            },
            onValueChangeFinished = {
                val checkpoints = listOf(0f, 90f, 180f, 270f, 360f)
                val nearest = checkpoints.minByOrNull { abs(it - gradientAngle) } ?: return@Slider
                if (abs(nearest - gradientAngle) <= 8f) {
                    onAngleChange(nearest, null)
                    Haptics.light(view)
                }
                lastCheckpoint = null
            },
            valueRange = 0f..360f,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(10.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

// ── PreviewFrame ─────────────────────────────────────────────────────────────
@Composable
private fun PreviewFrame(
    previewWallpaper: Wallpaper,
    selectedGradient: GradientType,
    gradientAngle: Float,
    isFavorite: Boolean,
    isBusy: Boolean,
    effects: EffectMap,
    overlayTextColor: @Composable () -> Color,
    onFavoriteToggle: () -> Unit
) {
    DeviceFrame(modifier = Modifier.fillMaxSize()) {

        PreviewWallpaperRender(
            wallpaper    = previewWallpaper,
            previewType  = selectedGradient,
            angleDeg     = gradientAngle,
            effects      = effects,
            modifier     = Modifier.fillMaxSize(),
            showTypeLabel = false
        )

        var localFav by remember { mutableStateOf(isFavorite) }
        val view = LocalView.current

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.30f))
                .clickable {
                    Haptics.confirm(view)
                    localFav = !localFav
                    onFavoriteToggle()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (localFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (localFav) Color(0xFFFF4D6A) else Color.White
            )
        }

        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(18.dp),
                strokeWidth = 2.dp
            )
        }
    }
}