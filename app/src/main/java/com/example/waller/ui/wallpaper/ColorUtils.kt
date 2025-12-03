/**
 * Utility functions for color handling:
 * - Convert Color <-> HSV
 * - Generate random colors (dark / neutral / light tone ranges)
 * - Generate shaded variations of existing colors
 * - Convert Compose Color ↔ ARGB integer
 * - Format hex strings for UI display
 *
 * Shared by UI components and bitmap generation logic.
 */

package com.example.waller.ui.wallpaper

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import kotlin.random.Random

fun colorToHsv(color: Color): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
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

/** Convert Compose Color to ARGB Int (for SimpleColorDialog) */
fun Color.toArgbInt(): Int =
    android.graphics.Color.argb(
        (alpha * 255).roundToInt(),
        (red * 255).roundToInt(),
        (green * 255).roundToInt(),
        (blue * 255).roundToInt()
    )

/** Convert ARGB Int from SimpleColorDialog back to Compose Color */
fun Int.toComposeColor(): Color =
    Color(
        red = android.graphics.Color.red(this) / 255f,
        green = android.graphics.Color.green(this) / 255f,
        blue = android.graphics.Color.blue(this) / 255f,
        alpha = android.graphics.Color.alpha(this) / 255f
    )

/**
 * Generates a pleasing random color biased by tone:
 * - DARK    → lower value range
 * - NEUTRAL → mid–high value range (slightly on the lighter side)
 * - LIGHT   → high value range
 */
fun generateRandomColor(toneMode: ToneMode): Color {
    val (minLuminance, maxLuminance) = when (toneMode) {
        ToneMode.DARK -> 0.0f to 0.45f
        // Neutral now biased a bit lighter
        ToneMode.NEUTRAL -> 0.35f to 0.85f
        ToneMode.LIGHT -> 0.55f to 1.0f
    }

    val base = Color(
        red = Random.nextFloat(),
        green = Random.nextFloat(),
        blue = Random.nextFloat(),
        alpha = 1f
    )

    val hsv = colorToHsv(base)
    val h = hsv[0]
    val s = hsv[1]
    val newV =
        (minLuminance + Random.nextFloat() * (maxLuminance - minLuminance)).coerceIn(0f, 1f)

    return Color.hsv(h, s, newV)
}

/**
 * createShade: small variation close to base color, biased by tone.
 *
 * @param subtle  When true, variations stay closer to the original color:
 *                - smaller hue/sat changes
 *                - smaller brightness change
 *
 * Used with subtle = true for user-selected colors so their variations
 * look like realistic shades instead of totally new colors.
 */
fun createShade(
    color: Color,
    toneMode: ToneMode,
    subtle: Boolean = false
): Color {
    val hsv = colorToHsv(color)
    var h = hsv[0]
    var s = hsv[1]
    var v = hsv[2]

    // Hue & saturation deltas – smaller in subtle mode
    val hueDeltaRange = if (subtle) 12f else 40f      // +/- 6° vs +/- 20°
    val satDeltaRange = if (subtle) 0.20f else 0.60f  // +/- 0.10 vs +/- 0.30

    val hueDelta = (Random.nextFloat() - 0.5f) * hueDeltaRange
    val satDelta = (Random.nextFloat() - 0.5f) * satDeltaRange

    // How strong the brightness adjustment can be
    val shadeBase = if (subtle) {
        // Smaller 0.20 .. 0.50
        Random.nextFloat() * 0.30f + 0.20f
    } else {
        // Original 0.40 .. 0.90
        Random.nextFloat() * 0.50f + 0.40f
    }

    h = (h + hueDelta).let {
        var hh = it
        while (hh < 0f) hh += 360f
        while (hh >= 360f) hh -= 360f
        hh
    }
    s = (s + satDelta).coerceIn(0f, 1f)

    v = when (toneMode) {
        ToneMode.DARK -> (v - shadeBase).coerceIn(0f, 1f)
        ToneMode.LIGHT -> (v + shadeBase).coerceIn(0f, 1f)
        ToneMode.NEUTRAL -> {
            // Neutral: wobble slightly around original, smaller wobble in subtle mode
            val factor = if (subtle) 0.25f else 0.4f
            val direction = if (Random.nextBoolean()) 1f else -1f
            (v + direction * shadeBase * factor).coerceIn(0f, 1f)
        }
    }

    return Color.hsv(h, s, v)
}