/**
 * Contains lightweight data models used across the app:
 *
 * data class Wallpaper:
 *   - Holds colors + chosen gradient type
 *
 * enum class GradientType:
 *   - Linear, Radial, Angular, Diamond
 *
 * enum class ToneMode:
 *   - DARK, NEUTRAL, LIGHT
 *
 * data class FavoriteWallpaper:
 *   - Wraps a Wallpaper + the effect flags used when it was favourited
 *   - Allows favourites screen to recreate the exact look (noise/stripes/glass/geometry)
 */

package com.example.waller.ui.wallpaper

import androidx.compose.ui.graphics.Color

data class Wallpaper(
    val colors: List<Color>,
    val type: GradientType,
    val angleDeg: Float = 0f
)

enum class GradientType {
    Linear,
    Radial,
    Angular,
    Diamond
}

// Tone mode used for random color generation and shading.
enum class ToneMode {
    DARK,
    NEUTRAL,
    LIGHT
}

/**
 * Snapshot of a favourite wallpaper at the time user tapped the heart.
 * We keep:
 * - underlying gradient `wallpaper`
 * - which effects were active: snow, stripes, glass, geometric
 */
data class FavoriteWallpaper(
    val wallpaper: Wallpaper,
    val addNoise: Boolean,
    val addStripes: Boolean,
    val addOverlay: Boolean,
    val addGeometric: Boolean,
    val noiseAlpha: Float = 1f,
    val stripesAlpha: Float = 1f,
    val overlayAlpha: Float = 1f,
    val geometricAlpha: Float = 1f
)
