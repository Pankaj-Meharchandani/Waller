/**
 * WallpaperPreviewOverlay — Done opens dialog; effects visible.
 * - Done opens ApplyDownloadDialog (does not auto-apply / auto-download).
 * - Bottom sheet has a minimum height so effect chips remain visible.
 * - Minimal single-line headers for major sections.
 */

package com.example.waller.ui.wallpaper.components

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.waller.ui.wallpaper.Wallpaper
import com.example.waller.ui.wallpaper.ApplyDownloadDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

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
    onApply: (home: Boolean, lock: Boolean, both: Boolean, noise: Boolean, stripes: Boolean, overlay: Boolean) -> Unit,
    onDownload: (noise: Boolean, stripes: Boolean, overlay: Boolean) -> Unit,
    // ADDED: to show the Apply/Download dialog from Done
    writePermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    context: Context,
    coroutineScope: CoroutineScope
) {
    // local effect states
    var noise by remember { mutableStateOf(globalNoise) }
    var stripes by remember { mutableStateOf(globalStripes) }
    var overlay by remember { mutableStateOf(globalOverlay) }

    // dialog & busy states
    var showApplyDialog by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    BackHandler { onDismiss() }

    // insets & sizing
    val statusBarPadding: Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding: Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val safeVerticalPadding = 18.dp

    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val availableHeight = (screenHeightDp - statusBarPadding - navBarPadding - safeVerticalPadding * 2).coerceAtLeast(200.dp)
    val aspectRatio = if (isPortrait) (9f / 16f) else (16f / 9f)

    // background scrim
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
        // Top actions row
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
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // DONE button now opens the dialog (does not auto apply/download)
                TextButton(
                    onClick = {
                        if (isBusy) return@TextButton
                        showApplyDialog = true
                    },
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Done")
                }
            }
        }

        // center preview (lower zIndex)
        var pressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.99f else 1f,
            animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)
        )
        val elevationDp by animateDpAsState(if (pressed) 12.dp else 24.dp, animationSpec = tween(220))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarPadding + safeVerticalPadding, bottom = navBarPadding + safeVerticalPadding)
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
                    .shadow(12.dp, RoundedCornerShape(20.dp)),
                elevation = elevationDp
            ) {
            Box(modifier = Modifier.fillMaxSize()) {
                    // Pass real favorite state & callback so the card's fav button works
                    WallpaperItemCard(
                        wallpaper = wallpaper,
                        isPortrait = isPortrait,
                        addNoise = noise,
                        addStripes = stripes,
                        addOverlay = overlay,
                        isFavorite = isFavorite,
                        onFavoriteToggle = { /* from card -> forward local preview flags */
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

        // bottom sheet with effects (kept) — ensure it has a min height so chips are visible
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(3f)
                .padding(bottom = navBarPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.10f))
                .padding(14.dp)
                .fillMaxWidth(0.96f)
                .heightIn(min = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // small handle
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.06f))
            )

            Spacer(modifier = Modifier.height(10.dp))

            // effects row placed inside bottom sheet (centered)
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
                    EffectChip(label = "Nothing", selected = overlay) {
                        overlay = !overlay
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    EffectChip(label = "Snow", selected = noise) {
                        noise = !noise
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    EffectChip(label = "Stripes", selected = stripes) {
                        stripes = !stripes
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // no action buttons here; Done opens the dialog
        }

        // Apply/Download dialog (opened when Done pressed)
        if (showApplyDialog) {
            ApplyDownloadDialog(
                show = showApplyDialog,
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
private fun DeviceFrame(modifier: Modifier = Modifier, elevation: Dp = 12.dp, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f), shape = RoundedCornerShape(20.dp))
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // optional blur hook
        }

        Box(modifier = Modifier.padding(8.dp)) {
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
        ) {}
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
