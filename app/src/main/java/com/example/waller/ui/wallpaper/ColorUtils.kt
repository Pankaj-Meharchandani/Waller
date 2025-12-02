/**
 * Utility functions for color handling:
 * - Convert Color <-> HSV
 * - Generate random colors (light/dark biased)
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

/* Generates a pleasing random color biased by light/dark tone */
fun generateRandomColor(isLight: Boolean): Color {
    val minLuminance = if (isLight) 0.5f else 0.0f
    val maxLuminance = if (isLight) 1.0f else 0.5f
    return Color(
        red = Random.nextFloat(),
        green = Random.nextFloat(),
        blue = Random.nextFloat(),
        alpha = 1f
    ).let { color ->
        val (h, s, _) = colorToHsv(color)
        val newV =
            (minLuminance + Random.nextFloat() * (maxLuminance - minLuminance)).coerceIn(0f, 1f)
        Color.hsv(h, s, newV)
    }
}

/* createShade: small variation close to base color */
fun createShade(color: Color, isLight: Boolean): Color {
    val hsv = colorToHsv(color)
    var h = hsv[0]
    var s = hsv[1]
    var v = hsv[2]

    val hueDelta = (Random.nextFloat() - 0.5f) * 40f      // +/- ~20°
    val satDelta = (Random.nextFloat() - 0.5f) * 0.6f     // +/- ~0.30
    val shadeFactor = Random.nextFloat() * 0.5f + 0.40f   // 0.40 .. 0.90

    h = (h + hueDelta).let {
        var hh = it
        while (hh < 0f) hh += 360f
        while (hh >= 360f) hh -= 360f
        hh
    }
    s = (s + satDelta).coerceIn(0f, 1f)
    v =
        if (isLight) (v + shadeFactor).coerceIn(0f, 1f) else (v - shadeFactor).coerceIn(0f, 1f)
    return Color.hsv(h, s, v)
}
