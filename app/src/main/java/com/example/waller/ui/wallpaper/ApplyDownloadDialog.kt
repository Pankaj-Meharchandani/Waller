/**
 * Dialog shown when the user taps a wallpaper.
 * Provides actions:
 * - Apply to home screen
 * - Apply to lock screen
 * - Apply to both
 * - Download as PNG
 *
 * Includes a loading indicator for heavy bitmap operations.
 * Uses BitmapUtils for actual generation and storage tasks.
 */

package com.example.waller.ui.wallpaper

import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.waller.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment

@Composable
fun ApplyDownloadDialog(
    show: Boolean,
    wallpaper: Wallpaper?,
    isPortrait: Boolean,
    addNoise: Boolean,
    addStripes: Boolean,
    addOverlay: Boolean,
    addGeometric: Boolean,
    isWorking: Boolean,
    noiseAlpha: Float = 1f,
    stripesAlpha: Float = 1f,
    overlayAlpha: Float = 1f,
    geometricAlpha: Float = 1f,
    onWorkingChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    writePermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    context: android.content.Context,
    coroutineScope: CoroutineScope
) {
    if (!show || wallpaper == null) return

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(26.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.apply_download_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.apply_download_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val bmp = createGradientBitmap(
                                            context,
                                            wallpaper,
                                            isPortrait,
                                            addNoise,
                                            addStripes,
                                            addOverlay,
                                            addGeometric,
                                            noiseAlpha,
                                            stripesAlpha,
                                            overlayAlpha,
                                            geometricAlpha
                                        )
                                        withContext(Dispatchers.Main) {
                                            shareBitmapAsPng(context, bmp)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                /* ───────── Primary action ───────── */

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
//                    shape = RoundedCornerShape(22.dp),
                    onClick = {
                        onWorkingChange(true)
                        coroutineScope.launch(Dispatchers.IO) {
                            val bmp = createGradientBitmap(
                                context,
                                wallpaper,
                                isPortrait,
                                addNoise,
                                addStripes,
                                addOverlay,
                                addGeometric,
                                noiseAlpha,
                                stripesAlpha,
                                overlayAlpha,
                                geometricAlpha
                            )
                            val success = tryApplyWallpaper(
                                context,
                                bmp,
                                android.app.WallpaperManager.FLAG_SYSTEM or getLockFlag()
                            )
                            withContext(Dispatchers.Main) {
                                onWorkingChange(false)
                                Toast.makeText(
                                    context,
                                    if (success)
                                        context.getString(R.string.apply_success_both)
                                    else
                                        context.getString(R.string.apply_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                            }
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.apply_both_screens),
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                /* ───────── Secondary actions ───────── */

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    FilledTonalButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
//                        shape = RoundedCornerShape(20.dp),
                        onClick = {
                            onWorkingChange(true)
                            coroutineScope.launch(Dispatchers.IO) {
                                val bmp = createGradientBitmap(
                                    context,
                                    wallpaper,
                                    isPortrait,
                                    addNoise,
                                    addStripes,
                                    addOverlay,
                                    addGeometric,
                                    noiseAlpha,
                                    stripesAlpha,
                                    overlayAlpha,
                                    geometricAlpha
                                )
                                val success = tryApplyWallpaper(
                                    context,
                                    bmp,
                                    android.app.WallpaperManager.FLAG_SYSTEM
                                )
                                withContext(Dispatchers.Main) {
                                    onWorkingChange(false)
                                    Toast.makeText(
                                        context,
                                        if (success)
                                            context.getString(R.string.apply_success_home)
                                        else
                                            context.getString(R.string.apply_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onDismiss()
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.apply_home_screen))
                    }

                    FilledTonalButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
//                        shape = RoundedCornerShape(20.dp),
                        onClick = {
                            onWorkingChange(true)
                            coroutineScope.launch(Dispatchers.IO) {
                                val bmp = createGradientBitmap(
                                    context,
                                    wallpaper,
                                    isPortrait,
                                    addNoise,
                                    addStripes,
                                    addOverlay,
                                    addGeometric,
                                    noiseAlpha,
                                    stripesAlpha,
                                    overlayAlpha,
                                    geometricAlpha
                                )
                                val flagLock = getLockFlag()
                                val success =
                                    if (flagLock != 0)
                                        tryApplyWallpaper(context, bmp, flagLock)
                                    else
                                        tryApplyWallpaper(
                                            context,
                                            bmp,
                                            android.app.WallpaperManager.FLAG_SYSTEM
                                        )

                                withContext(Dispatchers.Main) {
                                    onWorkingChange(false)
                                    Toast.makeText(
                                        context,
                                        if (success)
                                            context.getString(R.string.apply_success_lock)
                                        else
                                            context.getString(R.string.apply_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onDismiss()
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.apply_lock_screen))
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                /* ───────── Utility actions ───────── */

                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
//                    shape = RoundedCornerShape(20.dp),
                    onClick = {
                        onWorkingChange(true)
                        coroutineScope.launch(Dispatchers.IO) {
                            val bmp = createGradientBitmap(
                                context,
                                wallpaper,
                                isPortrait,
                                addNoise,
                                addStripes,
                                addOverlay,
                                addGeometric,
                                noiseAlpha,
                                stripesAlpha,
                                overlayAlpha,
                                geometricAlpha
                            )
                            val saved = saveBitmapToMediaStore(
                                context,
                                bmp,
                                "waller_${System.currentTimeMillis()}.png"
                            )
                            withContext(Dispatchers.Main) {
                                onWorkingChange(false)
                                Toast.makeText(
                                    context,
                                    if (saved)
                                        context.getString(R.string.save_success)
                                    else
                                        context.getString(R.string.save_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.download))
                }

                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onDismiss() }
                ) {
                    Text(text = stringResource(R.string.cancel))
                }

                /* ───────── Progress ───────── */

                if (isWorking) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(999.dp))
                    )
                }
            }
        }
    }
}
