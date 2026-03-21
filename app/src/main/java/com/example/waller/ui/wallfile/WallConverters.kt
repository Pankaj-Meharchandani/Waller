/**
 * Converts between FavoriteWallpaper and WallFavorite.
 *
 * toFavoriteWallpaper() handles both:
 *   - New format: effectIds / effectEnabled / effectAlphas parallel lists
 *   - Old format: addNoise / addStripes / addOverlay / addGeometric / addBlur named fields
 */

package com.example.waller.ui.wallfile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.waller.ui.wallpaper.*

fun FavoriteWallpaper.toWallFavorite(): WallFavorite {
    val ids     = WallpaperEffects.ALL.map { it.id }
    val enabled = ids.map { effects.isEnabled(it) }
    val alphas  = ids.map { effects.alpha(it) }
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

    if (effectIds.isNotEmpty()) {
        // New format: read from parallel lists
        effectIds.forEachIndexed { i, id ->
            if (WallpaperEffects.find(id) != null) {
                base[id] = EffectState(
                    enabled = effectEnabled.getOrElse(i) { false },
                    alpha   = effectAlphas.getOrElse(i) { 1f }
                )
            }
        }
    } else {
        // Old format: map named fields to effect ids
        base["noise"]     = EffectState(addNoise,    noiseAlpha)
        base["stripes"]   = EffectState(addStripes,  stripesAlpha)
        base["overlay"]   = EffectState(addOverlay,  overlayAlpha)
        base["geometric"] = EffectState(addGeometric, geometricAlpha)
        base["blur"]      = EffectState(addBlur,     blurAlpha)
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