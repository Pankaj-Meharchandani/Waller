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
