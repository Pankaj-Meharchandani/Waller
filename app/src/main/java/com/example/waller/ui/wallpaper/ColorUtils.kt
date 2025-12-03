/**
 * Utility functions for color handling:
 * - Convert Color <-> HSV
 * - Generate random colors based on ToneMode (Light / Neutral / Dark)
 * - Generate shaded variations of existing colors (subtle / stronger)
 * - Convert Compose Color ↔ ARGB integer
 * - Format hex strings for UI display
 * - Parse hex strings back to Color (for favourites persistence)
 *
 * Shared by UI components and bitmap generation logic.
 */

package com.example.waller.ui.wallpaper

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import kotlin.random.Random

fun colorToHsv(color: Color): FloatArray {
    val hsv = FloatArray(3)
    AndroidColor.RGBToHSV(
        (color.red * 255).roundToInt(),
        (color.green * 255).roundToInt(),
        (color.blue * 255).roundToInt(),
        hsv
    )
    return hsv
}

fun Color.toHexString(): String {
    return String.format(
        "#%02X%02X%02X",
        (this.red * 255).roundToInt(),
        (this.green * 255).roundToInt(),
        (this.blue * 255).roundToInt()
    )
}

/**
 * Parse a hex string like "#RRGGBB" or "RRGGBB" into a Color.
 * Returns null if parsing fails.
 */
fun colorFromHexOrNull(hex: String): Color? {
    return try {
        val cleaned = hex.trim().let {
            if (it.startsWith("#")) it else "#$it"
        }
        val intColor = AndroidColor.parseColor(cleaned)
        Color(
            red = AndroidColor.red(intColor) / 255f,
            green = AndroidColor.green(intColor) / 255f,
            blue = AndroidColor.blue(intColor) / 255f,
            alpha = AndroidColor.alpha(intColor) / 255f
        )
    } catch (_: IllegalArgumentException) {
        null
    }
}

/** Convert Compose Color to ARGB Int (for SimpleColorDialog) */
fun Color.toArgbInt(): Int =
    AndroidColor.argb(
        (alpha * 255).roundToInt(),
        (red * 255).roundToInt(),
        (green * 255).roundToInt(),
        (blue * 255).roundToInt()
    )

/** Convert ARGB Int from SimpleColorDialog back to Compose Color */
fun Int.toComposeColor(): Color =
    Color(
        red = AndroidColor.red(this) / 255f,
        green = AndroidColor.green(this) / 255f,
        blue = AndroidColor.blue(this) / 255f,
        alpha = AndroidColor.alpha(this) / 255f
    )

/**
 * Tone mode used for random color generation and shading.
 *
 * LIGHT   -> higher value (V), bright colors
 * NEUTRAL -> mid value, not too dark or bright
 * DARK    -> lower value, deeper colors
 */
fun generateRandomColor(toneMode: ToneMode): Color {
    val (minV, maxV) = when (toneMode) {
        ToneMode.LIGHT -> 0.6f to 1.0f
        ToneMode.NEUTRAL -> 0.35f to 0.75f
        ToneMode.DARK -> 0.05f to 0.55f
    }

    // Start with a fully random RGB color
    val base = Color(
        red = Random.nextFloat(),
        green = Random.nextFloat(),
        blue = Random.nextFloat(),
        alpha = 1f
    )

    val hsv = colorToHsv(base)
    val h = hsv[0]
    val s = hsv[1].coerceIn(0.35f, 1f) // avoid too desaturated
    val v = (minV + Random.nextFloat() * (maxV - minV)).coerceIn(0f, 1f)

    return Color.hsv(h, s, v)
}

/**
 * createShade: small variation close to base color.
 *
 * - subtle = true  -> small hue & saturation shifts, gentle value adjust
 * - subtle = false -> slightly stronger variation, used for secondary color
 */
fun createShade(color: Color, toneMode: ToneMode, subtle: Boolean): Color {
    val hsv = colorToHsv(color)
    var h = hsv[0]
    var s = hsv[1]
    var v = hsv[2]

    val hueDeltaRange = if (subtle) 18f else 35f   // degrees
    val satDeltaRange = if (subtle) 0.15f else 0.35f
    val valueDeltaRange = if (subtle) 0.12f else 0.25f

    val hueDelta = (Random.nextFloat() - 0.5f) * 2f * hueDeltaRange
    val satDelta = (Random.nextFloat() - 0.5f) * 2f * satDeltaRange
    val valueDeltaRaw = (Random.nextFloat() - 0.5f) * 2f * valueDeltaRange

    // Hue: wrap around 0..360
    h = (h + hueDelta).let {
        var hh = it
        while (hh < 0f) hh += 360f
        while (hh >= 360f) hh -= 360f
        hh
    }

    // Saturation: keep within 0..1
    s = (s + satDelta).coerceIn(0f, 1f)

    // Value: bias based on tone mode
    v = when (toneMode) {
        ToneMode.LIGHT -> (v + valueDeltaRaw.absoluteValue()).coerceIn(0.55f, 1f)
        ToneMode.NEUTRAL -> (v + valueDeltaRaw * 0.5f).coerceIn(0.3f, 0.8f)
        ToneMode.DARK -> (v - valueDeltaRaw.absoluteValue()).coerceIn(0.05f, 0.6f)
    }

    return Color.hsv(h, s, v)
}

private fun Float.absoluteValue(): Float =
    if (this < 0f) -this else this
