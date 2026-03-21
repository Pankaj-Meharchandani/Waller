/**
 * Represents a .wall file used for sharing wallpapers.
 *
 * Backward compatible: old files used named boolean/float fields (addNoise etc).
 * New files use parallel effectIds/effectEnabled/effectAlphas lists.
 * Both formats are handled in WallConverters.toFavoriteWallpaper().
 */

package com.example.waller.ui.wallfile

import com.example.waller.ui.wallpaper.GradientType
import kotlinx.serialization.Serializable

@Serializable
data class WallFile(
    val version: Int = 2,
    val walls: List<WallFavorite>
)

@Serializable
data class WallFavorite(
    val colors: List<Int>,
    val gradientType: GradientType,
    val angleDeg: Float,

    // ── New format (v2): generic effect lists ─────────────────────────────────
    val effectIds: List<String> = emptyList(),
    val effectEnabled: List<Boolean> = emptyList(),
    val effectAlphas: List<Float> = emptyList(),

    // ── Old format (v1): named fields — kept for backward compat, defaulted ──
    val addNoise: Boolean = false,
    val addStripes: Boolean = false,
    val addOverlay: Boolean = false,
    val addGeometric: Boolean = false,
    val addBlur: Boolean = false,
    val noiseAlpha: Float = 1f,
    val stripesAlpha: Float = 1f,
    val overlayAlpha: Float = 1f,
    val geometricAlpha: Float = 1f,
    val blurAlpha: Float = 1f
)