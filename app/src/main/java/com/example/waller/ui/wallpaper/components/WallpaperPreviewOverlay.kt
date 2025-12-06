/**
 * WallpaperPreviewOverlay — Done opens dialog; effects visible.
 * - Done opens ApplyDownloadDialog (does not auto-apply / auto-download).
 * - Bottom sheet has a minimum height so effect chips remain visible.
 * - Minimal single-line headers for major sections.
 */

@file:Suppress("DEPRECATION")

package com.example.waller.ui.wallpaper.components

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.waller.ui.wallpaper.Wallpaper
import com.example.waller.ui.wallpaper.ApplyDownloadDialog
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.waller.R

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
    var noise by remember { mutableStateOf(globalNoise) }
    var stripes by remember { mutableStateOf(globalStripes) }
    var overlay by remember { mutableStateOf(globalOverlay) }

    var showApplyDialog by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    BackHandler { onDismiss() }

    val statusBarPadding: Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val safeBottomPadding = 14.dp
    val safeVerticalPadding = 18.dp
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    (screenHeightDp - statusBarPadding - safeVerticalPadding * 2).coerceAtLeast(200.dp)
    val aspectRatio = if (isPortrait) (9f / 16f) else (16f / 9f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
            .background(Color.Black.copy(alpha = 0.60f))
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

    Box(modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier.fillMaxWidth()) {
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
                    tonalElevation = 6.dp,
                    modifier = Modifier.height(46.dp)
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(46.dp)) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.preview_close))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        if (isBusy) return@TextButton
                        showApplyDialog = true
                    },
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(stringResource(id = R.string.preview_done))
                }
            }
        }

        var pressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.99f else 1f,
            animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarPadding + safeVerticalPadding, bottom = safeBottomPadding + safeVerticalPadding)
                .zIndex(1f),
            contentAlignment = Alignment.Center
        ) {
            DeviceFrame(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(aspectRatio)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        })
                    }
                    .shadow(12.dp, RoundedCornerShape(20.dp))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    WallpaperItemCard(
                        wallpaper = wallpaper,
                        isPortrait = isPortrait,
                        addNoise = noise,
                        addStripes = stripes,
                        addOverlay = overlay,
                        isFavorite = isFavorite,
                        onFavoriteToggle = {
                            onFavoriteToggle(noise, stripes, overlay)
                        },
                        onClick = {},
                        isPreview = true
                    )

                    AnimatedVisibility(visible = isBusy, enter = fadeIn(), exit = fadeOut()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                                .padding(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(3f)
                .padding(bottom = safeBottomPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(18.dp))
                .padding(14.dp)
                .fillMaxWidth(0.96f)
                .heightIn(min = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .zIndex(5f),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.18f))
                        .shadow(4.dp, RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EffectChip(label = stringResource(id = R.string.preview_effect_nothing), selected = overlay) {
                        overlay = !overlay
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    EffectChip(label = stringResource(id = R.string.preview_effect_snow), selected = noise) {
                        noise = !noise
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    EffectChip(label = stringResource(id = R.string.preview_effect_stripes), selected = stripes) {
                        stripes = !stripes
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
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
                onWorkingChange = { isWorking -> isBusy = isWorking },
                onDismiss = { showApplyDialog = false },
                writePermissionLauncher = writePermissionLauncher,
                context = context,
                coroutineScope = coroutineScope
            )
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
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f), shape = RoundedCornerShape(20.dp))
    ) {

        Box(modifier = Modifier.padding(8.dp)) { content() }

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
    val targetBg = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val bg by androidx.compose.animation.animateColorAsState(targetBg, animationSpec = tween(220))
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val elev by animateDpAsState(if (selected) 6.dp else 0.dp)

    Surface(
        shape = RoundedCornerShape(999.dp),
        tonalElevation = elev,
        color = bg,
        border = if (selected) null else androidx.compose.material3.ButtonDefaults.outlinedButtonBorder,
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = contentColor)
        }
    }
}
