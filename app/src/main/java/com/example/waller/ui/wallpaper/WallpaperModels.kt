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
 * object WallpaperEffects:
 *   - Single source of truth for ALL effects in the app.
 *   - To add a new effect: add one entry to ALL list. That's it.
 *   - Every other file reads from this list; none of them need to change.
 *
 * data class EffectState:
 *   - Holds enabled + alpha for a single effect, keyed by effect id.
 *
 * data class FavoriteWallpaper:
 *   - Wraps a Wallpaper + a Map<effectId, EffectState> snapshot.
 *
 * ─── HOW TO ADD A NEW EFFECT ───────────────────────────────────────────────
 *  1. Add an entry to WallpaperEffects.ALL  (this file)
 *  2. Add a `when (id) { "yourId" -> ... }` branch in BitmapUtils.kt
 *  That's it. No other file needs touching.
 * ───────────────────────────────────────────────────────────────────────────
 */

package com.example.waller.ui.wallpaper

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

data class Wallpaper(
    val colors: List<Color>,
    val type: GradientType,
    val angleDeg: Float = 0f
)

@Serializable
enum class GradientType {
    Linear,
    Radial,
    Angular,
    Diamond,
    Pastels
}

// Tone mode used for random color generation and shading.
enum class ToneMode {
    DARK,
    NEUTRAL,
    LIGHT
}

// ─────────────────────────────────────────────────────────────────────────────
// Effect registry — single source of truth
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Metadata for one visual effect.
 * @param id          Stable string key used in maps / serialization.
 * @param labelRes    String resource for the UI label.
 * @param subtitleRes String resource for the UI subtitle (0 = none).
 * @param defaultAlpha Alpha to use when the effect is first enabled.
 */
data class EffectDef(
    val id: String,
    val labelRes: Int,
    val subtitleRes: Int = 0,
    val defaultAlpha: Float = 1f
)

object WallpaperEffects {
    /**
     * Ordered list of all effects.
     * ADD A NEW EFFECT HERE — nowhere else (except BitmapUtils rendering).
     */
    val ALL: List<EffectDef> = listOf(
        EffectDef(
            id          = "noise",
            labelRes    = com.example.waller.R.string.effects_snow_effect,
            subtitleRes = com.example.waller.R.string.effects_snow_effect_subtitle
        ),
        EffectDef(
            id          = "stripes",
            labelRes    = com.example.waller.R.string.effects_stripes_overlay,
            subtitleRes = com.example.waller.R.string.effects_stripes_overlay_subtitle
        ),
        EffectDef(
            id          = "overlay",
            labelRes    = com.example.waller.R.string.effects_nothing_style,
            subtitleRes = com.example.waller.R.string.effects_nothing_style_subtitle
        ),
        EffectDef(
            id          = "geometric",
            labelRes    = com.example.waller.R.string.effect_geometric,
            subtitleRes = 0
        ),
        EffectDef(
            id          = "blur",
            labelRes    = 0,  // inline string below — add a string res if desired
            subtitleRes = 0
        )
    )

    /** Convenience: look up a def by id (never null for known ids). */
    fun find(id: String): EffectDef? = ALL.firstOrNull { it.id == id }

    /** Returns a default-off map for all effects. */
    fun defaultMap(): Map<String, EffectState> =
        ALL.associate { it.id to EffectState(enabled = false, alpha = it.defaultAlpha) }
}

// ─────────────────────────────────────────────────────────────────────────────
// EffectState — per-effect runtime/saved state
// ─────────────────────────────────────────────────────────────────────────────

data class EffectState(
    val enabled: Boolean = false,
    val alpha: Float = 1f
)

// Convenience extensions on the map type used everywhere
typealias EffectMap = Map<String, EffectState>

fun EffectMap.isEnabled(id: String): Boolean = this[id]?.enabled ?: false
fun EffectMap.alpha(id: String): Float        = this[id]?.alpha  ?: 1f

/** Returns a copy of the map with one field changed. */
fun EffectMap.withEnabled(id: String, enabled: Boolean): EffectMap =
    toMutableMap().also { it[id] = (it[id] ?: EffectState()).copy(enabled = enabled) }

fun EffectMap.withAlpha(id: String, alpha: Float): EffectMap =
    toMutableMap().also { it[id] = (it[id] ?: EffectState()).copy(alpha = alpha) }

// ─────────────────────────────────────────────────────────────────────────────
// FavoriteWallpaper — snapshot stored in favourites list
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Snapshot of a favourite wallpaper at the time user tapped the heart.
 * effects map: effectId -> EffectState (enabled + alpha).
 */
data class FavoriteWallpaper(
    val wallpaper: Wallpaper,
    val effects: EffectMap = WallpaperEffects.defaultMap()
)