package com.example.waller.ui.wallpaper

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ApplyDownloadDialog(
    show: Boolean,
    wallpaper: Wallpaper?,
    isPortrait: Boolean,
    addNoise: Boolean,
    addStripes: Boolean,
    addOverlay: Boolean,
    isWorking: Boolean,
    onWorkingChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    writePermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    context: android.content.Context,
    coroutineScope: CoroutineScope
) {
    if (!show || wallpaper == null) return

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
            ),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)) {

                Text(
                    "Apply / Download",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Choose where to apply or save this wallpaper.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Both screens
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    onClick = {
                        onWorkingChange(true)
                        coroutineScope.launch(Dispatchers.IO) {
                            val bmp = createGradientBitmap(
                                context,
                                wallpaper,
                                isPortrait,
                                addNoise,
                                addStripes,
                                addOverlay
                            )
                            val flags =
                                android.app.WallpaperManager.FLAG_SYSTEM or getLockFlag()
                            val success = tryApplyWallpaper(context, bmp, flags)
                            withContext(Dispatchers.Main) {
                                onWorkingChange(false)
                                Toast.makeText(
                                    context,
                                    if (success) "Applied to home & lock screen" else "Failed to apply",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                            }
                        }
                    }
                ) {
                    Text("Both home & lock screen")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Home only
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    onClick = {
                        onWorkingChange(true)
                        coroutineScope.launch(Dispatchers.IO) {
                            val bmp = createGradientBitmap(
                                context,
                                wallpaper,
                                isPortrait,
                                addNoise,
                                addStripes,
                                addOverlay
                            )
                            val success =
                                tryApplyWallpaper(
                                    context,
                                    bmp,
                                    android.app.WallpaperManager.FLAG_SYSTEM
                                )
                            withContext(Dispatchers.Main) {
                                onWorkingChange(false)
                                Toast.makeText(
                                    context,
                                    if (success) "Applied to home screen" else "Failed to apply",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                            }
                        }
                    }
                ) {
                    Text("Home screen")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Lock only (or fallback)
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    onClick = {
                        onWorkingChange(true)
                        coroutineScope.launch(Dispatchers.IO) {
                            val bmp = createGradientBitmap(
                                context,
                                wallpaper,
                                isPortrait,
                                addNoise,
                                addStripes,
                                addOverlay
                            )
                            val flagLock = getLockFlag()
                            val success =
                                if (flagLock != 0) tryApplyWallpaper(
                                    context,
                                    bmp,
                                    flagLock
                                ) else tryApplyWallpaper(
                                    context,
                                    bmp,
                                    android.app.WallpaperManager.FLAG_SYSTEM
                                )
                            withContext(Dispatchers.Main) {
                                onWorkingChange(false)
                                Toast.makeText(
                                    context,
                                    if (success) "Applied to lock screen" else "Failed to apply",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                            }
                        }
                    }
                ) {
                    Text("Lock screen")
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Download
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    onClick = {
                        val sdkTooOld =
                            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                        if (sdkTooOld &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            Toast.makeText(
                                context,
                                "Please grant storage permission and tap Download again",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            onWorkingChange(true)
                            coroutineScope.launch(Dispatchers.IO) {
                                val bmp = createGradientBitmap(
                                    context,
                                    wallpaper,
                                    isPortrait,
                                    addNoise,
                                    addStripes,
                                    addOverlay
                                )
                                val filename =
                                    "waller_${System.currentTimeMillis()}.png"
                                val saved =
                                    saveBitmapToMediaStore(context, bmp, filename)
                                withContext(Dispatchers.Main) {
                                    onWorkingChange(false)
                                    Toast.makeText(
                                        context,
                                        if (saved) "Saved to Pictures" else "Save failed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onDismiss()
                                }
                            }
                        }
                    }
                ) {
                    Text("Download")
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onDismiss() }
                ) {
                    Text("Cancel")
                }

                if (isWorking) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                    )
                }
            }
        }
    }
}
