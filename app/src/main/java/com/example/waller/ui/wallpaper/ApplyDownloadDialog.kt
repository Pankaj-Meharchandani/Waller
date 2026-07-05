/**
 * Dialog shown when the user taps a wallpaper.
 * Provides actions:
 * - Apply to home screen
 * - Apply to lock screen
 * - Apply to both
 * - Download as PNG
 * - Share as PNG / .wall / SVG / CSS
 *
 * Uses EffectMap — adding a new effect requires no change here.
 */

package com.example.waller.ui.wallpaper

import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.waller.R
import com.example.waller.data.network.TelegramMarketplaceService
import com.example.waller.ui.wallfile.WallFileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ApplyDownloadDialog(
    interactionMode: InteractionMode,
    show: Boolean,
    wallpaper: Wallpaper?,
    isPortrait: Boolean,
    effects: EffectMap,
    isWorking: Boolean,
    onWorkingChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    writePermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    context: android.content.Context,
    coroutineScope: CoroutineScope,
) {
    if (!show || wallpaper == null) return
    var showShareOptions by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(
                    width = 3.dp,
                    color = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    else        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(20.dp)
                ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.apply_download_title),
                            fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.apply_download_subtitle),
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    if (interactionMode == InteractionMode.ADVANCED) {
                                        showShareOptions = true
                                    } else {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            val bmp = createGradientBitmap(context, wallpaper, isPortrait, effects)
                                            withContext(Dispatchers.Main) { shareBitmapAsPng(context, bmp) }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.share_wallpaper),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Apply to both screens
                Button(
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    onClick = {
                        onWorkingChange(true)
                        coroutineScope.launch(Dispatchers.IO) {
                            val bmp = createGradientBitmap(context, wallpaper, isPortrait, effects)
                            val success = tryApplyWallpaper(
                                context, bmp,
                                android.app.WallpaperManager.FLAG_SYSTEM or getLockFlag()
                            )
                            withContext(Dispatchers.Main) {
                                onWorkingChange(false)
                                Toast.makeText(
                                    context,
                                    if (success) context.getString(R.string.apply_success_both)
                                    else         context.getString(R.string.apply_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                            }
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.apply_both_screens), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    // Apply to home screen
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            onWorkingChange(true)
                            coroutineScope.launch(Dispatchers.IO) {
                                val bmp = createGradientBitmap(context, wallpaper, isPortrait, effects)
                                val success = tryApplyWallpaper(context, bmp, android.app.WallpaperManager.FLAG_SYSTEM)
                                withContext(Dispatchers.Main) {
                                    onWorkingChange(false)
                                    Toast.makeText(
                                        context,
                                        if (success) context.getString(R.string.apply_success_home)
                                        else         context.getString(R.string.apply_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onDismiss()
                                }
                            }
                        }
                    ) {
                        Text(text = stringResource(R.string.apply_home_screen), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                    // Apply to lock screen
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            onWorkingChange(true)
                            coroutineScope.launch(Dispatchers.IO) {
                                val bmp = createGradientBitmap(context, wallpaper, isPortrait, effects)
                                val flagLock = getLockFlag()
                                val success = if (flagLock != 0) tryApplyWallpaper(context, bmp, flagLock)
                                else tryApplyWallpaper(context, bmp, android.app.WallpaperManager.FLAG_SYSTEM)
                                withContext(Dispatchers.Main) {
                                    onWorkingChange(false)
                                    Toast.makeText(
                                        context,
                                        if (success) context.getString(R.string.apply_success_lock)
                                        else         context.getString(R.string.apply_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onDismiss()
                                }
                            }
                        }
                    ) {
                        Text(text = stringResource(R.string.apply_lock_screen), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Download
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        onWorkingChange(true)
                        coroutineScope.launch(Dispatchers.IO) {
                            val bmp = createGradientBitmap(context, wallpaper, isPortrait, effects)
                            val saved = saveBitmapToMediaStore(context, bmp, "waller_${System.currentTimeMillis()}.png")
                            withContext(Dispatchers.Main) {
                                onWorkingChange(false)
                                Toast.makeText(
                                    context,
                                    if (saved) context.getString(R.string.save_success)
                                    else       context.getString(R.string.save_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                            }
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.download), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }

                TextButton(modifier = Modifier.fillMaxWidth(), onClick = { onDismiss() }) {
                    Text(text = stringResource(R.string.cancel), fontSize = 14.sp)
                }

                if (isWorking) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }

    // ── Share options dialog — same card style as Apply dialog ──────────────────
    if (showShareOptions && wallpaper != null) {
        Dialog(onDismissRequest = { showShareOptions = false }) {
            val isDarkShare = MaterialTheme.colorScheme.background.luminance() < 0.5f

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .border(
                        width = 3.dp,
                        color = if (isDarkShare) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        else             MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    // Title
                    Text(
                        text = stringResource(R.string.share_wallpaper),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp
                    )

                    Spacer(Modifier.height(4.dp))

                    // ── Primary: .wall — matches Button style ─────────────────
                    Button(
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        onClick = {
                            showShareOptions = false
                            WallFileManager.shareWall(context, FavoriteWallpaper(wallpaper, effects))
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.share_wall_file),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.share_wall_file_subtitle),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }

                    // ── Secondary: PNG — matches FilledTonalButton style ──────
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        onClick = {
                            showShareOptions = false
                            coroutineScope.launch(Dispatchers.IO) {
                                val bmp = createGradientBitmap(context, wallpaper, isPortrait, effects)
                                withContext(Dispatchers.Main) { shareBitmapAsPng(context, bmp) }
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.share_png),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = stringResource(R.string.share_png_subtitle),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // ── Marketplace: Upload ──
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        onClick = {
                            showShareOptions = false
                            onWorkingChange(true)
                            coroutineScope.launch(Dispatchers.IO) {
                                val fav = FavoriteWallpaper(wallpaper, effects)
                                // Generate a small preview for the marketplace
                                val previewBmp = createGradientBitmap(context, wallpaper, isPortrait, effects)
                                
                                val result = TelegramMarketplaceService.uploadWallpaper(fav, previewBmp)
                                withContext(Dispatchers.Main) {
                                    onWorkingChange(false)
                                    if (result.isSuccess) {
                                        Toast.makeText(context, context.getString(R.string.publish_success), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "${context.getString(R.string.publish_failed)}: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.share_to_market),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = stringResource(R.string.share_to_market_subtitle),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // ── Compact pair: SVG + CSS — matches OutlinedButton style ─
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

                        OutlinedButton(
                            onClick = {
                                showShareOptions = false
                                coroutineScope.launch(Dispatchers.IO) {
                                    withContext(Dispatchers.Main) {
                                        shareAsSvg(context, FavoriteWallpaper(wallpaper, effects))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.share_svg_label),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = stringResource(R.string.share_svg_sublabel),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                showShareOptions = false
                                coroutineScope.launch(Dispatchers.IO) {
                                    withContext(Dispatchers.Main) {
                                        shareAsCss(context, FavoriteWallpaper(wallpaper, effects))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.share_css_label),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = stringResource(R.string.share_css_sublabel),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Cancel
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showShareOptions = false }
                    ) {
                        Text(text = stringResource(R.string.cancel), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}