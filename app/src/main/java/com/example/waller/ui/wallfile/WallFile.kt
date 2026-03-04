/**
 * Represents a .wall file used for sharing wallpapers.
 *
 * - version allows future format upgrades
 * - walls contains a list of exported wallpapers
 * - colors are stored as ARGB Ints because Compose Color is not serializable
 */

package com.example.waller.ui.wallfile

import com.example.waller.ui.wallpaper.GradientType
import kotlinx.serialization.Serializable

@Serializable
data class WallFile(
    val version: Int = 1,
    val walls: List<WallFavorite>
)

data class WallFavorite(
    val colors: List<Int>,
    val gradientType: GradientType,
    val angleDeg: Float,

    val addNoise: Boolean,
    val addStripes: Boolean,
    val addOverlay: Boolean,
    val addGeometric: Boolean,

    val noiseAlpha: Float,
    val stripesAlpha: Float,
    val overlayAlpha: Float,
    val geometricAlpha: Float
)