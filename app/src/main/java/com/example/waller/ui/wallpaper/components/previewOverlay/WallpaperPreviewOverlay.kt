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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
private val GlassBg           = Color(0xB3000000)
private val GlassBorder       = Color(0x1AFFFFFF)
private val SidebarBg         = Color(0x4D000000)
private val TabActiveBg       = Color(0x26FFFFFF)

// ── Main Composable ───────────────────────────────────────────────────────────

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun WallpaperPreviewOverlay(
    wallpaper: Wallpaper,
    isPortrait: Boolean,
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
    var activeEffectId  by remember { mutableStateOf<String?>(null) }
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

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. Background Render
        PreviewWallpaperRender(
            wallpaper   = previewWallpaper,
            previewType = selectedGradient,
            angleDeg    = displayAngle,
            effects     = localEffects,
            modifier    = Modifier.fillMaxSize(),
            showTypeLabel = false
        )

        // 2. Control Layers
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn() + slideInVertically { it / 10 },
            exit = fadeOut() + slideOutVertically { it / 10 }
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
                    GlassIconBtn(onClick = { isControlsVisible = false }) {
                        Icon(Icons.Default.Visibility, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                // --- Left Floating Sidebar: Gradient Styles ---
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(SidebarBg)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(24.dp))
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                .size(52.dp)
                                .clickable { Haptics.light(view); selectedGradient = type },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSel) {
                                Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)))
                            }
                            Text(
                                text = type.name.take(1),
                                color = if (isSel) Color.White else Color.White.copy(alpha = 0.4f),
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                // --- Bottom Controls ---
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    
                    // Main Config Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(GlassBg)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(28.dp))
                            .padding(16.dp)
                    ) {
                        // Tabs for Effects / Live
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EffectTab("Angle", activeEffectId == null) { activeEffectId = null }
                            WallpaperEffects.ALL.forEach { def ->
                                EffectTab(
                                    if (def.labelRes != 0) stringResource(def.labelRes) else def.id,
                                    activeEffectId == def.id
                                ) { activeEffectId = def.id }
                            }
                            EffectTab("Live", activeEffectId == "live") { activeEffectId = "live" }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Tab Content
                        Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                            when (activeEffectId) {
                                null -> AngleSlider(gradientAngle) { gradientAngle = it }
                                "live" -> LiveControls(isLiveEnabled, liveSpeed, { isLiveEnabled = it }, { liveSpeed = it })
                                else -> {
                                    val id = activeEffectId!!
                                    EffectControls(
                                        enabled = localEffects.isEnabled(id),
                                        alpha = localEffects.alpha(id),
                                        onToggle = { localEffects = localEffects.withEnabled(id, it) },
                                        onAlpha = { localEffects = localEffects.withAlpha(id, it) }
                                    )
                                }
                            }
                        }
                    }

                    // Apply Button
                    Button(
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
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(if (isLiveEnabled) "Set as Live Wallpaper" else "Apply Wallpaper", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
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
private fun EffectTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) TabActiveBg else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AngleSlider(angle: Float, onAngle: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("${angle.toInt()}°", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
        Slider(
            value = angle, onValueChange = onAngle, valueRange = 0f..360f, modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.2f))
        )
    }
}

@Composable
private fun EffectControls(enabled: Boolean, alpha: Float, onToggle: (Boolean) -> Unit, onAlpha: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Switch(
            checked = enabled, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color.White)
        )
        Slider(
            value = alpha, onValueChange = onAlpha, enabled = enabled, modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.2f))
        )
        Text("${(alpha * 100).toInt()}%", color = if (enabled) Color.White else Color.White.copy(alpha = 0.3f), fontSize = 12.sp, modifier = Modifier.width(36.dp))
    }
}

@Composable
private fun LiveControls(enabled: Boolean, speed: Float, onToggle: (Boolean) -> Unit, onSpeed: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Switch(
            checked = enabled, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color.White)
        )
        Column(Modifier.weight(1f)) {
            Text("Animation Speed", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
            Slider(
                value = speed, onValueChange = onSpeed, enabled = enabled, valueRange = 0.01f..0.2f,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.2f))
            )
        }
    }
}
