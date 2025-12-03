/**
 * Contains lightweight data models used across the app:
 *
 * data class Wallpaper:
 *   - Holds 2 colors + chosen gradient type
 *
 * enum class GradientType:
 *   - Linear, Radial, Angular, Diamond
 *
 * enum class ToneMode:
 *   - DARK, NEUTRAL, LIGHT (used for color generation and shading)
 *
 * These models are shared by previews and bitmap generation.
 */

package com.example.waller.ui.wallpaper

import androidx.compose.ui.graphics.Color

data class Wallpaper(
    val colors: List<Color>,
    val type: GradientType
)

enum class GradientType {
    Linear,
    Radial,
    Angular,
    Diamond
}

enum class ToneMode {
    DARK,
    NEUTRAL,
    LIGHT
}
