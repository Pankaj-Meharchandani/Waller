/**
 * Converts between FavoriteWallpaper and WallFavorite.
 * Handles Color <-> ARGB Int conversion.
 *
 * Adding a new effect requires NO change here — effects are stored as
 * generic id/enabled/alpha lists, not individual named fields.
 */

package com.example.waller.ui.wallfile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.waller.ui.wallpaper.*

fun FavoriteWallpaper.toWallFavorite(): WallFavorite {
    val ids      = effects.keys.toList()
    val enabled  = ids.map { effects.isEnabled(it) }
    val alphas   = ids.map { effects.alpha(it) }
    return WallFavorite(
        colors        = wallpaper.colors.map { it.toArgb() },
        gradientType  = wallpaper.type,
        angleDeg      = wallpaper.angleDeg,
        effectIds     = ids,
        effectEnabled = enabled,
        effectAlphas  = alphas
    )
}

fun WallFavorite.toFavoriteWallpaper(): FavoriteWallpaper {
    val base = WallpaperEffects.defaultMap().toMutableMap()
    effectIds.forEachIndexed { i, id ->
        if (WallpaperEffects.find(id) != null) {
            base[id] = EffectState(
                enabled = effectEnabled.getOrNull(i) ?: false,
                alpha   = effectAlphas.getOrNull(i) ?: 1f
            )
        }
    }
    return FavoriteWallpaper(
        wallpaper = Wallpaper(
            colors   = colors.map { Color(it) },
            type     = gradientType,
            angleDeg = angleDeg
        ),
        effects = base
    )
}