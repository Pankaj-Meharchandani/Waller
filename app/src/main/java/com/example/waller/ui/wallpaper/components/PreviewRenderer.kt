/**
 * PreviewRenderer.kt
 * Pure rendering helpers for wallpaper preview:
 * - createBrushForPreview(...) returns Compose Brush for linear/radial/diamond
 * - createRotatedSweepShader(...) returns a configured SweepGradient for ANGULAR
 *
 * This file contains no composables — safe to test independently.
 */

@file:Suppress("unused")
package com.example.waller.ui.wallpaper.components

import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// Use the app's GradientType (defined in WallpaperModels.kt: com.example.waller.ui.wallpaper.GradientType)
import com.example.waller.ui.wallpaper.GradientType

/**
 * Create a Compose Brush for the preview (angle-aware for linear/radial/diamond).
 * - colors: Compose Color list
 * - type: GradientType (enum from WallpaperModels)
 * - widthPx/heightPx: measured size in px
 * - angleDeg: 0..360
 */
fun createBrushForPreview(
    colors: List<Color>,
    type: GradientType,
    widthPx: Float,
    heightPx: Float,
    angleDeg: Float
): Brush {
    val a = Math.toRadians(angleDeg.toDouble()).toFloat()
    val cx = widthPx / 2f
    val cy = heightPx / 2f

    return when (type) {
        GradientType.Linear, GradientType.Diamond -> {
            val dx = cos(a)
            val dy = sin(a)
            val halfW = widthPx / 2f
            val halfH = heightPx / 2f
            val start = Offset(cx - dx * halfW, cy - dy * halfH)
            val end = Offset(cx + dx * halfW, cy + dy * halfH)
            Brush.linearGradient(colors = colors, start = start, end = end)
        }
        GradientType.Radial -> {
            val radius = max(widthPx, heightPx) * 0.6f
            val shiftFactor = 0.22f
            val ox = cx + cos(a) * radius * shiftFactor
            val oy = cy + sin(a) * radius * shiftFactor
            Brush.radialGradient(colors = colors, center = Offset(ox, oy), radius = radius)
        }
        GradientType.Angular -> {
            // Fallback sweep brush for Compose (actual rotation applied on native Shader in UI)
            Brush.sweepGradient(colors = colors)
        }
    }
}

/**
 * Create a rotated SweepGradient for native Canvas drawing (android.graphics.SweepGradient).
 * The returned SweepGradient will have its local matrix set (best-effort).
 */
fun createRotatedSweepShader(widthPx: Float, heightPx: Float, androidColors: IntArray, angleDeg: Float): SweepGradient {
    val cx = widthPx / 2f
    val cy = heightPx / 2f
    val sweep = SweepGradient(cx, cy, androidColors, null)
    val matrix = Matrix()
    matrix.setRotate(angleDeg, cx, cy)
    try {
        sweep.setLocalMatrix(matrix)
    } catch (_: Exception) {
        // Some devices/VMs may not support setLocalMatrix; fail gracefully and return sweep un-rotated.
    }
    return sweep
}
