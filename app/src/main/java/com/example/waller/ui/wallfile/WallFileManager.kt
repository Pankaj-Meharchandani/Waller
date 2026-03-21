/**
 * Handles exporting and sharing .wall files.
 * A .wall file contains serialized wallpaper configurations.
 *
 * Import is lenient: handles both v1 (named effect fields) and v2 (effectIds lists).
 * ignoreUnknownKeys = true so future format changes don't break old app versions.
 */

package com.example.waller.ui.wallfile

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.waller.ui.wallpaper.FavoriteWallpaper
import kotlinx.serialization.json.Json
import java.io.File

object WallFileManager {

    private val json = Json {
        prettyPrint        = true
        ignoreUnknownKeys  = true   // survive unknown fields in old/future formats
        encodeDefaults     = false  // don't write default-value fields (keeps files small)
    }

    fun exportSingle(context: Context, fav: FavoriteWallpaper): File {
        val file = File(context.cacheDir, "waller_${System.currentTimeMillis()}.wall")
        file.writeText(json.encodeToString(WallFile(walls = listOf(fav.toWallFavorite()))))
        return file
    }

    fun shareWall(context: Context, fav: FavoriteWallpaper) {
        val file = exportSingle(context, fav)
        share(context, file, "Share wallpaper")
    }

    fun importWallFile(context: Context, uri: Uri): List<FavoriteWallpaper>? {
        return try {
            val jsonString = context.contentResolver
                .openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: return null

            json.decodeFromString<WallFile>(jsonString)
                .walls
                .map { it.toFavoriteWallpaper() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportFavorites(context: Context, favourites: List<FavoriteWallpaper>): File {
        val file = File(context.cacheDir, "waller_favourites_${System.currentTimeMillis()}.wall")
        file.writeText(json.encodeToString(WallFile(walls = favourites.map { it.toWallFavorite() })))
        return file
    }

    fun shareFavorites(context: Context, favourites: List<FavoriteWallpaper>) {
        share(context, exportFavorites(context, favourites), "Share favourites")
    }

    private fun share(context: Context, file: File, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                chooserTitle
            )
        )
    }
}