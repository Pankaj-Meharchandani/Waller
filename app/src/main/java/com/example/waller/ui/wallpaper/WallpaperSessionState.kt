package com.example.waller.ui.wallpaper

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

@Stable
data class WallpaperSessionState(
    var wallpapers: List<Wallpaper> = emptyList(),
    var selectedColors: List<Color> = emptyList(),
    var selectedGradientTypes: List<GradientType> = listOf(GradientType.Linear),
    var toneMode: ToneMode = ToneMode.LIGHT,
    var isMulticolor: Boolean = false,
    var scrollIndex: Int = 0
)
