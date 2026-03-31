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
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waller.R
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ── Design tokens ─────────────────────────────────────────────────────────────
private val GlassBg           = Color(0xCC000000)
private val GlassBorder       = Color(0x33FFFFFF)
private val SidebarBg         = Color(0x99000000)
private val TabActiveBg       = Color.White

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

    // Live animation logic
    val infiniteTransition = rememberInfiniteTransition(label = "live")
    val animatedAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1000 / liveSpeed.coerceAtLeast(0.01f)).toInt().coerceIn(1000, 100000),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

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
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
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
            Box(modifier = Modifier.fillMaxSize()) {

                // --- Top Bar ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconBtn(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    var localFav by remember { mutableStateOf(isFavorite) }
                    GlassIconBtn(onClick = {
                        Haptics.light(view); localFav = !localFav; onFavoriteToggle(previewWallpaper, localEffects)
                    }) {
                        Icon(
                            if (localFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            null, tint = if (localFav) Color(0xFFFF5F7A) else Color.White, modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    GlassIconBtn(onClick = { onOrientationChange(!isPortrait) }) {
                        Icon(
                            if (isPortrait) Icons.Default.StayCurrentLandscape else Icons.Default.StayCurrentPortrait,
                            null, tint = Color.White, modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    GlassIconBtn(onClick = { isControlsVisible = false }) {
                        Icon(Icons.Default.Visibility, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                // --- Style & Angle Sidebars ---
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = if (isPortrait) 0.dp else 80.dp) // Move down below top bar in landscape
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
                                .background(SidebarBg)
                                .border(0.5.dp, GlassBorder, CircleShape)
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            listOf(
                                GradientType.Linear,
                                GradientType.Radial,
                                GradientType.Angular,
                                GradientType.Diamond
                            ).forEach { type ->
                                val isSel = selectedGradient == type
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isSel) Color.White else Color.Transparent)
                                        .clickable { Haptics.light(view); selectedGradient = type },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = type.name.take(1),
                                        color = if (isSel) Color.Black else Color.White.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
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
                                kotlin.math.abs(newAngle - 360f) < 360f -> 360f
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
                            .background(SidebarBg)
                            .border(0.5.dp, GlassBorder, CircleShape)
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(30.dp)
                                .clip(CircleShape)
                                .background(if (isLiveEnabled) Color.White else Color.Transparent)
                                .clickable { isLiveEnabled = !isLiveEnabled; Haptics.light(view) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "LIVE",
                                color = if (isLiveEnabled) Color.Black else Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        AnimatedVisibility(
                            visible = isLiveEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(sideBarHeight - 60.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Slider(
                                    value = liveSpeed,
                                    onValueChange = { liveSpeed = it },
                                    enabled = isLiveEnabled,
                                    valueRange = 0.01f..0.2f,
                                    modifier = Modifier
                                        .graphicsLayer {
                                            rotationZ = -90f
                                        }
                                        .requiredWidth(sideBarHeight - 60.dp)
                                        .requiredHeight(48.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
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
                                prefs.edit().putString("live_wallpaper_config", Json.encodeToString(fav.toWallFavorite()))
                                    .putFloat("live_wallpaper_speed", liveSpeed).apply()
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
                    Icon(Icons.Default.VisibilityOff, null, tint = Color.White, modifier = Modifier.size(20.dp))
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
            .background(Color.Black.copy(alpha = 0.3f))
            .border(0.5.dp, GlassBorder, CircleShape)
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
                    selected -> TabActiveBg
                    applied -> Color.White.copy(alpha = 0.15f)
                    else -> Color.White.copy(alpha = 0.05f)
                }
            )
            .then(
                if (applied && !selected) Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (applied) {
                Box(Modifier.size(4.dp).clip(CircleShape).background(if (selected) Color.Black else Color.White))
            }
            Text(
                label,
                color = if (selected) Color.Black else Color.White.copy(alpha = if (applied) 1f else 0.5f),
                fontSize = 12.sp,
                fontWeight = if (selected || applied) FontWeight.Bold else FontWeight.Medium
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
            .background(GlassBg)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(32.dp))
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
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
    ) {
        Icon(Icons.Default.Check, null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            if (isLiveEnabled) "Set as Live Wallpaper" else "Apply Wallpaper",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            letterSpacing = 0.5.sp
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
            .background(SidebarBg)
            .border(0.5.dp, GlassBorder, CircleShape)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Track background dots for snapped angles (90, 180, 270)
            Column(
                modifier = Modifier.height(height - 60.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(3) {
                    Box(Modifier.size(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
                }
            }

            Slider(
                value = angle,
                onValueChange = onAngle,
                valueRange = 0f..360f,
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = -90f
                    }
                    .requiredWidth(height - 60.dp)
                    .requiredHeight(48.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
        }
        Text(
            text = "${angle.toInt()}°",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun EffectControls(alpha: Float, onAlpha: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Slider(
            value = alpha, onValueChange = onAlpha, modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
        Text(
            "${(alpha * 100).toInt()}%",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(36.dp)
        )
    }
}
