/**
 * Handles exporting and sharing .wall files.
 * A .wall file contains serialized wallpaper configurations.
 */

package com.example.waller.ui.wallfile

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.waller.ui.wallpaper.FavoriteWallpaper
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object WallFileManager {

    private val json = Json {
        prettyPrint = true
    }

    fun exportSingle(
        context: Context,
        fav: FavoriteWallpaper
    ): File {

        val wallFile = WallFile(
            walls = listOf(fav.toWallFavorite())
        )

        val file = File(
            context.cacheDir,
            "waller_${System.currentTimeMillis()}.wall"
        )

        file.writeText(json.encodeToString(wallFile))

        return file
    }

    fun shareWall(
        context: Context,
        fav: FavoriteWallpaper
    ) {

        val file = exportSingle(context, fav)

        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(intent, "Share wallpaper")
        )
    }

    fun importWallFile(
        context: Context,
        uri: Uri
    ): List<FavoriteWallpaper>? {

        return try {

            val input = context.contentResolver.openInputStream(uri)
                ?: return null

            val jsonString = input.bufferedReader().use { it.readText() }

            val wallFile = json.decodeFromString<WallFile>(jsonString)

            wallFile.walls.map { it.toFavoriteWallpaper() }

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportFavorites(
        context: Context,
        favourites: List<FavoriteWallpaper>
    ): File {

        val wallFile = WallFile(
            walls = favourites.map { it.toWallFavorite() }
        )

        val file = File(
            context.cacheDir,
            "waller_favourites_${System.currentTimeMillis()}.wall"
        )

        file.writeText(json.encodeToString(wallFile))

        return file
    }

    fun shareFavorites(
        context: Context,
        favourites: List<FavoriteWallpaper>
    ) {

        val file = exportFavorites(context, favourites)

        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(intent, "Share favourites")
        )
    }
}