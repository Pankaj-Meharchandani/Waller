/**
 * Represents a .wall file used for sharing wallpapers.
 *
 * - version allows future format upgrades
 * - walls contains a list of exported wallpapers
 * - colors are stored as ARGB Ints because Compose Color is not serializable
 *
 * WallFavorite.effectIds / effectAlphas store enabled effects as parallel lists
 * so the file format stays serializable without a custom serializer for Map.
 * Adding a new effect requires no change here.
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
    // Parallel lists: index N in effectIds corresponds to index N in effectEnabled / effectAlphas.
    // Unknown ids in older files are silently ignored when converting back.
    val effectIds: List<String> = emptyList(),
    val effectEnabled: List<Boolean> = emptyList(),
    val effectAlphas: List<Float> = emptyList()
)