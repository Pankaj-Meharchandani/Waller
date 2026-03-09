/**
 * ShareUtils.kt
 *
 * Helpers for sharing generated wallpapers.
 *
 * - Saves bitmap to app cache
 * - Launches system share sheet
 *
 * Supported formats: PNG, SVG, CSS
 */

package com.example.waller.ui.wallpaper

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.waller.ui.wallfile.SvgExporter
import java.io.File
import java.io.FileOutputStream

fun shareBitmapAsPng(context: Context, bitmap: Bitmap) {
    val cacheDir = File(context.cacheDir, "shared_wallpapers").apply { mkdirs() }
    val file = File(cacheDir, "waller_share_${System.currentTimeMillis()}.png")

    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    shareFile(context, file, "image/png", "Share wallpaper")
}

fun shareAsSvg(context: Context, fav: FavoriteWallpaper) {
    val file = SvgExporter.exportSvg(context, fav)
    shareFile(context, file, "image/svg+xml", "Share as SVG")
}

fun shareAsCss(context: Context, fav: FavoriteWallpaper) {
    val file = SvgExporter.exportCss(context, fav)
    shareFile(context, file, "text/css", "Share as CSS")
}

private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(intent, chooserTitle))
}