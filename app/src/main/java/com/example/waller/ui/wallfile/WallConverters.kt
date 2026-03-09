/**
 * Converts between FavoriteWallpaper and WallFavorite.
 * Handles Color <-> ARGB Int conversion.
 */

package com.example.waller.ui.wallfile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.waller.ui.wallpaper.*


fun FavoriteWallpaper.toWallFavorite(): WallFavorite {
    return WallFavorite(
        colors = wallpaper.colors.map { it.toArgb() },
        gradientType = wallpaper.type,
        angleDeg = wallpaper.angleDeg,

        addNoise = addNoise,
        addStripes = addStripes,
        addOverlay = addOverlay,
        addGeometric = addGeometric,
        addBlur = addBlur,

        noiseAlpha = noiseAlpha,
        stripesAlpha = stripesAlpha,
        overlayAlpha = overlayAlpha,
        geometricAlpha = geometricAlpha,
        blurAlpha = blurAlpha
    )
}

fun WallFavorite.toFavoriteWallpaper(): FavoriteWallpaper {
    return FavoriteWallpaper(
        wallpaper = Wallpaper(
            colors = colors.map { Color(it) },
            type = gradientType,
            angleDeg = angleDeg
        ),
        addNoise = addNoise,
        addStripes = addStripes,
        addOverlay = addOverlay,
        addGeometric = addGeometric,
        addBlur = addBlur,
        noiseAlpha = noiseAlpha,
        stripesAlpha = stripesAlpha,
        overlayAlpha = overlayAlpha,
        geometricAlpha = geometricAlpha,
        blurAlpha = blurAlpha
    )
}