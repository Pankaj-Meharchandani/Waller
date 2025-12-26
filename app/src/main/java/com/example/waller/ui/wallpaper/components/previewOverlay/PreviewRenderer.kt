/**
 * PreviewRenderer.kt
 *
 * Pure rendering helpers for wallpaper preview.
 *
 * Responsibilities:
 * - Creates Compose Brushes for linear, radial, and diamond gradients
 * - Creates rotated SweepGradient shaders for angular gradients
 *
 * This file contains no composables and no UI state.
 */

@file:Suppress("unused")

package com.example.waller.ui.wallpaper.components.previewOverlay

import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

// Use the app's GradientType (defined in WallpaperModels.kt: com.example.waller.ui.wallpaper.GradientType)
import com.example.waller.ui.wallpaper.GradientType

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
        GradientType.Linear -> {
            val halfDiag = hypot(widthPx / 2f, heightPx / 2f)

            val dx = cos(a).toFloat()
            val dy = sin(a).toFloat()
            val start = Offset(cx - dx * halfDiag, cy - dy * halfDiag)
            val end = Offset(cx + dx * halfDiag, cy + dy * halfDiag)
            Brush.linearGradient(colors = colors, start = start, end = end)
        }

        GradientType.Diamond -> {
            val diamondAngleRad = Math.toRadians((angleDeg + 45.0) % 360.0)
            val halfDiag = hypot(widthPx / 2f, heightPx / 2f)

            val dx = cos(diamondAngleRad).toFloat()
            val dy = sin(diamondAngleRad).toFloat()
            val start = Offset(cx - dx * halfDiag, cy - dy * halfDiag)
            val end = Offset(cx + dx * halfDiag, cy + dy * halfDiag)
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
            Brush.sweepGradient(colors = colors)
        }
    }
}

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
