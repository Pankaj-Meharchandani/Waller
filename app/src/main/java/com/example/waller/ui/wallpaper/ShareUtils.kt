/**
 * ShareUtils.kt
 *
 * Helpers for sharing generated wallpapers.
 *
 * - Saves bitmap to app cache
 * - Launches system share sheet
 *
 * PNG-only for now.
 */

package com.example.waller.ui.wallpaper

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

fun shareBitmapAsPng(context: Context, bitmap: Bitmap) {
    val cacheDir = File(context.cacheDir, "shared_wallpapers").apply { mkdirs() }
    val file = File(cacheDir, "waller_share_${System.currentTimeMillis()}.png")

    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(intent, "Share wallpaper")
    )
}
