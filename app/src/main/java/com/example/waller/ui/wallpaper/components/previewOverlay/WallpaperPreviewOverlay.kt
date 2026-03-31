/**
 * WallpaperPreviewOverlay.kt
 *
 * Fullscreen preview screen for wallpapers.
 *
 * Responsibilities:
 * - Full-screen background preview
 * - Redesigned UI with visibility toggle (eye icon)
 * - Live Wallpaper controls (Toggle + Speed)
 * - "Apply" pill button
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.waller.ui.wallfile.toWallFavorite
import com.example.waller.ui.wallpaper.LiveWallpaperService
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs

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

    var localEffects by remember { mutableStateOf(initialEffects) }
    var activeEffectId by remember { mutableStateOf<String?>(null) }
    var isControlsVisible by remember { mutableStateOf(true) }
    
    // Live Wallpaper State
    var isLiveEnabled by remember { mutableStateOf(false) }
    var liveSpeed by remember { mutableFloatStateOf(prefs.getFloat("live_wallpaper_speed", 0.05f)) }

    var selectedGradient by remember(wallpaper) {
        mutableStateOf(wallpaper.type)
    }
    var gradientAngle by remember(wallpaper) { mutableFloatStateOf(wallpaper.angleDeg) }
    
    // Animation for Live Preview
    val infiniteTransition = rememberInfiniteTransition(label = "livePreview")
    val animatedAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1000 / liveSpeed.coerceAtLeast(0.01f)).toInt().coerceIn(1000, 100000),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "angleAnimation"
    )

    val displayAngle = if (isLiveEnabled) (gradientAngle + animatedAngle) % 360f else gradientAngle

    val previewWallpaper = remember(wallpaper, selectedGradient, displayAngle) {
        wallpaper.copy(type = selectedGradient, angleDeg = displayAngle)
    }

    var showApplyDialog by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }

    BackHandler { onDismiss() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fullscreen Preview Background
        PreviewWallpaperRender(
            wallpaper = previewWallpaper,
            previewType = selectedGradient,
            angleDeg = displayAngle,
            effects = localEffects,
            modifier = Modifier.fillMaxSize(),
            showTypeLabel = false
        )

        // UI Layer
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn() + fadeIn(),
            exit = fadeOut()
        ) {
            // Scrim for readability
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
        }

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Fav Button
            var localFav by remember { mutableStateOf(isFavorite) }
            IconButton(
                onClick = {
                    Haptics.light(view)
                    localFav = !localFav
                    onFavoriteToggle(previewWallpaper, localEffects)
                },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = if (localFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (localFav) Color(0xFFFF4D6A) else Color.White
                )
            }

            IconButton(
                onClick = { 
                    Haptics.light(view)
                    isControlsVisible = !isControlsVisible 
                },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    if (isControlsVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle UI",
                    tint = Color.White
                )
            }
        }

        // Controls and Apply Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tab Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "Styles" Tab
                        FilterChip(
                            selected = activeEffectId == null,
                            onClick = { 
                                Haptics.light(view)
                                activeEffectId = null 
                            },
                            label = { Text(stringResource(R.string.style_label)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        
                        // Effect Tabs
                        WallpaperEffects.ALL.forEach { def ->
                            val isEnabled = localEffects.isEnabled(def.id)
                            FilterChip(
                                selected = activeEffectId == def.id,
                                onClick = { 
                                    Haptics.light(view)
                                    activeEffectId = def.id 
                                },
                                label = { 
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(if (def.labelRes != 0) stringResource(def.labelRes) else def.id)
                                        if (isEnabled) {
                                            Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                        }
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                        
                        // "Live" Tab
                        FilterChip(
                            selected = activeEffectId == "live",
                            onClick = { 
                                Haptics.light(view)
                                activeEffectId = "live" 
                            },
                            label = { 
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(stringResource(R.string.live_wallpaper_label))
                                    if (isLiveEnabled) {
                                        Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                    }
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Content Area
                    Box(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                        when (activeEffectId) {
                            null -> {
                                // Styles & Angle
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf(
                                            GradientType.Linear to R.string.gradient_style_linear,
                                            GradientType.Radial to R.string.gradient_style_radial,
                                            GradientType.Angular to R.string.gradient_style_angular,
                                            GradientType.Diamond to R.string.gradient_style_diamond
                                        ).forEach { (type, labelRes) ->
                                            val isSelected = selectedGradient == type
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { 
                                                        Haptics.light(view)
                                                        selectedGradient = type 
                                                    },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary 
                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                            ) {
                                                Text(
                                                    stringResource(labelRes),
                                                    modifier = Modifier.padding(vertical = 10.dp),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Angle", style = MaterialTheme.typography.labelMedium)
                                            Text("${gradientAngle.toInt()}°", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = gradientAngle,
                                            onValueChange = { gradientAngle = it },
                                            valueRange = 0f..360f,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                            "live" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(stringResource(R.string.set_as_live_wallpaper), style = MaterialTheme.typography.titleSmall)
                                            Text("Animate gradient angle automatically", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(
                                            checked = isLiveEnabled, 
                                            onCheckedChange = { 
                                                Haptics.light(view)
                                                isLiveEnabled = it 
                                            },
                                            thumbContent = if (isLiveEnabled) {
                                                { Icon(modifier = Modifier.size(SwitchDefaults.IconSize), imageVector = Icons.Default.Close, contentDescription = null) }
                                            } else null
                                        )
                                    }
                                    
                                    if (isLiveEnabled) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(stringResource(R.string.live_wallpaper_speed), style = MaterialTheme.typography.labelMedium)
                                                val speedPct = ((liveSpeed - 0.01f) / (0.2f - 0.01f) * 100).toInt()
                                                Text("$speedPct%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            }
                                            Slider(
                                                value = liveSpeed,
                                                onValueChange = { liveSpeed = it },
                                                valueRange = 0.01f..0.2f,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                // Effect Opacity Slider
                                val id = activeEffectId!!
                                val isEnabled = localEffects.isEnabled(id)
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val def = WallpaperEffects.find(id)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(if (def?.labelRes != 0 && def != null) stringResource(def.labelRes) else id, style = MaterialTheme.typography.titleSmall)
                                            if (def?.subtitleRes != 0 && def != null) {
                                                Text(stringResource(def.subtitleRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Switch(
                                            checked = isEnabled, 
                                            onCheckedChange = { 
                                                Haptics.light(view)
                                                localEffects = localEffects.withEnabled(id, it) 
                                            }
                                        )
                                    }
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.graphicsLayer(alpha = if (isEnabled) 1f else 0.5f)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Opacity", style = MaterialTheme.typography.labelMedium)
                                            Text("${(localEffects.alpha(id) * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = localEffects.alpha(id),
                                            onValueChange = { localEffects = localEffects.withAlpha(id, it) },
                                            valueRange = 0f..1f,
                                            enabled = isEnabled,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Apply Pill Button
            Button(
                onClick = { 
                    Haptics.confirm(view)
                    if (isLiveEnabled) {
                        // Apply as Live
                        val fav = FavoriteWallpaper(wallpaper = previewWallpaper, effects = localEffects)
                        prefs.edit()
                            .putString("live_wallpaper_config", Json.encodeToString(fav.toWallFavorite()))
                            .putFloat("live_wallpaper_speed", liveSpeed)
                            .apply()

                        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                            putExtra(
                                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                ComponentName(context, LiveWallpaperService::class.java)
                            )
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Live wallpaper not supported", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Apply as Static
                        showApplyDialog = true 
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    if (isLiveEnabled) "Set as Live Wallpaper" else "Apply Wallpaper", 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Apply Dialog (for static wallpaper selection)
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
