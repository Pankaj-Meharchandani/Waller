/**
 * WallpaperPreviewOverlay.kt
 *
 * Fullscreen preview screen for wallpapers.
 *
 * Responsibilities:
 * - Full-screen background preview
 * - Redesigned UI with floating vertical style sidebar (left)
 * - Tabbed effect & live wallpaper controls (bottom)
 * - High-visibility visibility toggle
 */

@file:Suppress("DEPRECATION", "COMPOSE_APPLIER_CALL_MISMATCH")
package com.example.waller.ui.wallpaper.components.previewOverlay

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waller.ui.wallpaper.ApplyDownloadDialog
import com.example.waller.ui.wallpaper.EffectMap
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
import com.example.waller.ui.wallfile.toWallFavorite
import com.example.waller.ui.wallpaper.LiveWallpaperService
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import androidx.core.content.edit
import com.example.waller.R

// ── Design tokens ─────────────────────────────────────────────────────────────
// No hardcoded colors — using MaterialTheme.colorScheme

// ── Main Composable ───────────────────────────────────────────────────────────

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun WallpaperPreviewOverlay(
    wallpaper: Wallpaper,
    isPortrait: Boolean,
    onOrientationChange: (Boolean) -> Unit,
    isFavorite: Boolean,
    initialEffects: EffectMap,
    onFavoriteToggle: (wallpaper: Wallpaper, effects: EffectMap) -> Unit,
    onDismiss: () -> Unit,
    onApplied: (Wallpaper, EffectMap) -> Unit = { _, _ -> },
    writePermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    context: Context,
    coroutineScope: CoroutineScope
) {
    val view = LocalView.current
    val prefs = remember { context.getSharedPreferences("waller_prefs", Context.MODE_PRIVATE) }

    var localEffects    by remember { mutableStateOf(initialEffects) }
    var activeEffectId  by remember { mutableStateOf<String?>(WallpaperEffects.ALL.firstOrNull()?.id) }
    var isControlsVisible by remember { mutableStateOf(true) }

    // Live Wallpaper State
    var isLiveEnabled by remember { mutableStateOf(false) }
    var liveSpeed     by remember { mutableFloatStateOf(prefs.getFloat("live_wallpaper_speed", 0.05f)) }

    var selectedGradient by remember(wallpaper) { mutableStateOf(wallpaper.type) }
    var gradientAngle    by remember(wallpaper) { mutableFloatStateOf(wallpaper.angleDeg) }

    // Live animation logic: Manual accumulation for smooth speed changes
    var animatedAngle by remember { mutableFloatStateOf(0f) }
    val currentSpeed = rememberUpdatedState(liveSpeed)
    LaunchedEffect(isLiveEnabled) {
        if (isLiveEnabled) {
            var lastTime = withFrameNanos { it }
            while (true) {
                withFrameNanos { now ->
                    val deltaSeconds = (now - lastTime) / 1_000_000_000f
                    // Speed integration: speed * 360 = degrees per second
                    val degPerSec = currentSpeed.value * 360f
                    animatedAngle = (animatedAngle + degPerSec * deltaSeconds) % 360f
                    lastTime = now
                }
            }
        }
    }

    val displayAngle = if (isLiveEnabled) (gradientAngle + animatedAngle) % 360f else gradientAngle
    val previewWallpaper = remember(wallpaper, selectedGradient, displayAngle) {
        wallpaper.copy(type = selectedGradient, angleDeg = displayAngle)
    }

    var showApplyDialog by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }

    BackHandler { onDismiss() }

    val sideBarHeight = if (isPortrait) 240.dp else 180.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { /* Consume all touches to prevent leaking to list underneath */ }
            .background(Color.Black)
    ) {

        // 1. Background Render
        if (isPortrait) {
            PreviewWallpaperRender(
                wallpaper = previewWallpaper,
                previewType = selectedGradient,
                angleDeg = displayAngle,
                effects = localEffects,
                modifier = Modifier.fillMaxSize(),
                showTypeLabel = false
            )
        } else {
            // Landscape preview on Portrait screen: Centered 16:9 box
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // Dimmed background
                PreviewWallpaperRender(
                    wallpaper = previewWallpaper,
                    previewType = selectedGradient,
                    angleDeg = displayAngle,
                    effects = localEffects,
                    modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.4f),
                    showTypeLabel = false
                )
                // Focused landscape box
                PreviewWallpaperRender(
                    wallpaper = previewWallpaper,
                    previewType = selectedGradient,
                    angleDeg = displayAngle,
                    effects = localEffects,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.77f)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    showTypeLabel = false
                )
            }
        }

        // 2. Control Layers
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // If the screen is too short in portrait, shift the sidebars upward 
                // to prevent them from overlapping with the bottom control card.
                val sidebarVerticalOffset = if (isPortrait && maxHeight < 800.dp) {
                    val shortfall = 800.dp - maxHeight
                    -(shortfall / 2f).coerceAtMost(90.dp)
                } else 0.dp

                // --- Top Bar ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconBtn(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    var localFav by remember { mutableStateOf(isFavorite) }
                    GlassIconBtn(onClick = {
                        Haptics.light(view); localFav = !localFav; onFavoriteToggle(previewWallpaper, localEffects)
                    }) {
                        Icon(
                            if (localFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            null, tint = if (localFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    GlassIconBtn(onClick = { onOrientationChange(!isPortrait) }) {
                        Icon(
                            if (isPortrait) Icons.Default.StayCurrentLandscape else Icons.Default.StayCurrentPortrait,
                            null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    GlassIconBtn(onClick = { isControlsVisible = false }) {
                        Icon(Icons.Default.Visibility, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                    }
                }

                // --- Style & Angle Sidebars ---
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = if (isPortrait) 0.dp else 140.dp)
                        .offset(y = sidebarVerticalOffset)
                ) {
                    // Left Sidebar: Style & Angle
                    Row(
                        modifier = Modifier
                            .align(if (isPortrait) Alignment.CenterStart else Alignment.TopStart)
                            .padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Style Selector
                        Column(
                            modifier = Modifier
                                .width(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("TYPE", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            Spacer(Modifier.height(2.dp))
                            listOf(
                                GradientType.Linear to Icons.Default.LinearScale,
                                GradientType.Radial to Icons.Default.RadioButtonChecked,
                                GradientType.Angular to Icons.Default.DonutLarge,
                                GradientType.Diamond to Icons.Default.Diamond,
                                GradientType.Pastels to Icons.Default.BlurCircular
                            ).forEach { (type, icon) ->
                                val isSel = selectedGradient == type
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { Haptics.light(view); selectedGradient = type },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = type.name,
                                        tint = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        VerticalAngleSlider(gradientAngle, sideBarHeight) { newAngle ->
                            val snapped = when {
                                kotlin.math.abs(newAngle - 0f) < 8f -> 0f
                                kotlin.math.abs(newAngle - 90f) < 8f -> 90f
                                kotlin.math.abs(newAngle - 180f) < 8f -> 180f
                                kotlin.math.abs(newAngle - 270f) < 8f -> 270f
                                kotlin.math.abs(newAngle - 360f) < 8f -> 360f
                                else -> newAngle
                            }
                            gradientAngle = snapped
                        }
                    }

                    // Right Sidebar: Live Control
                    Column(
                        modifier = Modifier
                            .align(if (isPortrait) Alignment.CenterEnd else Alignment.TopEnd)
                            .padding(end = 12.dp)
                            .width(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("LIVE", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isLiveEnabled) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { isLiveEnabled = !isLiveEnabled; Haptics.light(view) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isLiveEnabled) Icons.Default.Pause else Icons.Default.PlayArrow,
                                null,
                                tint = if (isLiveEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (isLiveEnabled) {
                            Spacer(Modifier.height(16.dp))
                            Text("SPEED", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            Box(
                                modifier = Modifier
                                    .height(sideBarHeight - 120.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Slider(
                                    value = liveSpeed,
                                    onValueChange = { 
                                        liveSpeed = it
                                        // Save speed immediately so applied live wallpaper reacts in real-time
                                        prefs.edit { putFloat("live_wallpaper_speed", it) }
                                    },
                                    enabled = isLiveEnabled,
                                    valueRange = 0.01f..0.2f,
                                    modifier = Modifier
                                        .graphicsLayer {
                                            rotationZ = -90f
                                            transformOrigin = TransformOrigin.Center
                                        }
                                        .requiredWidth(sideBarHeight - 120.dp)
                                        .requiredHeight(48.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    )
                                )
                            }
                        }
                    }
                }

                // --- Bottom Controls ---
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = if (isPortrait) 20.dp else 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EffectsConfigCard(
                        localEffects = localEffects,
                        activeEffectId = activeEffectId,
                        onEffectChange = { localEffects = it },
                        onActiveEffectIdChange = { activeEffectId = it }
                    )
                    ApplyButton(
                        isLiveEnabled = isLiveEnabled,
                        onClick = {
                            Haptics.confirm(view)
                            if (isLiveEnabled) {
                                val fav = FavoriteWallpaper(previewWallpaper, localEffects)
                                prefs.edit {
                                    putString(
                                        "live_wallpaper_config",
                                        Json.encodeToString(fav.toWallFavorite())
                                    )
                                        .putFloat("live_wallpaper_speed", liveSpeed)
                                }
                                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                    putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, LiveWallpaperService::class.java))
                                }
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            } else {
                                showApplyDialog = true
                            }
                        }
                    )
                }
            }
        }

        // 3. Hidden Mode Eye (always present when UI hidden)
        if (!isControlsVisible) {
            Box(Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { isControlsVisible = true })
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                GlassIconBtn(onClick = { isControlsVisible = true }) {
                    Icon(Icons.Default.VisibilityOff, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (showApplyDialog) {
            ApplyDownloadDialog(
                interactionMode = InteractionMode.ADVANCED,
                show = true,
                wallpaper = previewWallpaper,
                isPortrait = isPortrait,
                effects = localEffects,
                isWorking = isBusy,
                onWorkingChange = { isBusy = it },
                onDismiss = { showApplyDialog = false },
                onApplied = onApplied,
                writePermissionLauncher = writePermissionLauncher,
                context = context,
                coroutineScope = coroutineScope
            )
        }
    }
}

// ── Sub-components ───────────────────────────────────────────────────────────

@Composable
private fun GlassIconBtn(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun EffectTab(label: String, selected: Boolean, applied: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(
                when {
                    selected -> MaterialTheme.colorScheme.primary
                    applied -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            )
            .then(
                if (applied && !selected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (applied) {
                Box(Modifier.size(4.dp).clip(CircleShape).background(if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary))
            }
            Text(
                label,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = if (applied) 1f else 0.5f),
                fontSize = 12.sp,
                fontWeight = if (selected || applied) FontWeight.Bold else FontWeight.Medium,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun EffectsConfigCard(
    localEffects: EffectMap,
    activeEffectId: String?,
    onEffectChange: (EffectMap) -> Unit,
    onActiveEffectIdChange: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
            .padding(16.dp)
    ) {
        // Tabs for Effects
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WallpaperEffects.ALL.forEach { def ->
                val isEnabled = localEffects.isEnabled(def.id)
                EffectTab(
                    label = if (def.labelRes != 0) stringResource(def.labelRes) else def.id,
                    selected = activeEffectId == def.id,
                    applied = isEnabled
                ) { onActiveEffectIdChange(def.id) }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Tab Content
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
            val id = activeEffectId
            if (id != null) {
                EffectControls(
                    alpha = if (localEffects.isEnabled(id)) localEffects.alpha(id) else 0f,
                    onAlpha = {
                        onEffectChange(localEffects.withAlpha(id, it).withEnabled(id, it > 0.01f))
                    }
                )
            }
        }
    }
}

@Composable
private fun ApplyButton(
    isLiveEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(bottom = 4.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
    ) {
        Icon(Icons.Default.Check, null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            if (isLiveEnabled) stringResource(R.string.set_as_live_wallpaper) else "Apply Wallpaper",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            letterSpacing = 0.5.sp,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun VerticalAngleSlider(angle: Float, height: androidx.compose.ui.unit.Dp, onAngle: (Float) -> Unit) {
    Column(
        modifier = Modifier
            .width(48.dp)
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ANGLE", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Track background dots for snapped angles (90, 180, 270)
            Column(
                modifier = Modifier.height(height - 100.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(3) {
                    Box(Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)))
                }
            }

            Slider(
                value = angle,
                onValueChange = onAngle,
                valueRange = 0f..360f,
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = -90f
                        transformOrigin = TransformOrigin.Center
                    }
                    .requiredWidth(height - 100.dp)
                    .requiredHeight(48.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${angle.toInt()}°",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun EffectControls(alpha: Float, onAlpha: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Slider(
            value = alpha, onValueChange = onAlpha, modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        )
        Text(
            "${(alpha * 100).toInt()}%",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(36.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}
