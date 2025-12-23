/**
 * Core bitmap generation engine for Waller.
 *
 * Responsibilities:
 * - Detect device screen size (portrait/landscape)
 * - Convert Compose gradients to Android shaders (Linear/Radial/Sweep)
 * - Draw final gradient, noise, stripes, and overlay PNG into a single bitmap
 * - Save PNGs through MediaStore
 * - Apply wallpapers using WallpaperManager
 *
 * Completely UI-independent; safe to use from background threads.
 */

@file:Suppress("DEPRECATION")

package com.example.waller.ui.wallpaper

import android.app.WallpaperManager
import android.app.WallpaperManager.FLAG_LOCK
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.ui.text.TextPainter.paint
import com.example.waller.R
import androidx.core.graphics.scale
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random
import androidx.core.graphics.createBitmap

// Get a practical bitmap size based on the device/window (portrait or landscape)
fun getScreenSizeForBitmap(context: Context, isPortrait: Boolean): Pair<Int, Int> {
    return try {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = wm.currentWindowMetrics
        val insets =
            metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
        val w = metrics.bounds.width() - insets.left - insets.right
        val h = metrics.bounds.height() - insets.top - insets.bottom
        if (isPortrait) Pair(minOf(w, h), maxOf(w, h)) else Pair(maxOf(w, h), minOf(w, h))
    } catch (e: Exception) {
        e.printStackTrace()
        if (isPortrait) Pair(1080, 1920) else Pair(1920, 1080)
    }
}

fun createGradientBitmap(
    context: Context,
    wallpaper: Wallpaper,
    isPortrait: Boolean,
    addNoise: Boolean = false,
    addStripes: Boolean = false,
    addOverlay: Boolean = false,
    addGeometric: Boolean = false,
    noiseAlpha: Float = 1f,
    stripesAlpha: Float = 1f,
    overlayAlpha: Float = 1f,
    geometricAlpha: Float = 1f
): Bitmap {
    val (width, height) = getScreenSizeForBitmap(context, isPortrait)
    val bmp = createBitmap(width, height)
    val canvas = android.graphics.Canvas(bmp)

    // Use angle from model (degrees -> radians)
    val angleDeg = wallpaper.angleDeg
    val a = Math.toRadians(angleDeg.toDouble()).toFloat()

    val colors = wallpaper.colors.map {
        android.graphics.Color.argb(
            (it.alpha * 255).roundToInt(),
            (it.red * 255).roundToInt(),
            (it.green * 255).roundToInt(),
            (it.blue * 255).roundToInt()
        )
    }.toIntArray()

    // Helper center
    val cx = width / 2f
    val cy = height / 2f

    when (wallpaper.type) {
        // Linear & Diamond: use linear shader along rotated angle
        GradientType.Linear, GradientType.Diamond -> {
            // compute direction vector from angle
            val dx = kotlin.math.cos(a)
            val dy = kotlin.math.sin(a)
            val halfW = width / 2f
            val halfH = height / 2f
            val startX = cx - dx * halfW
            val startY = cy - dy * halfH
            val endX = cx + dx * halfW
            val endY = cy + dy * halfH

            val shader = LinearGradient(
                startX, startY, endX, endY,
                colors, null, Shader.TileMode.CLAMP
            )
            val paint = Paint().apply {
                isAntiAlias = true
                this.shader = shader
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }

        GradientType.Radial -> {
            val radius = max(width, height) * 0.6f
            val shiftFactor = 0.22f
            val ox = cx + kotlin.math.cos(a) * radius * shiftFactor
            val oy = cy + kotlin.math.sin(a) * radius * shiftFactor

            val shader = RadialGradient(
                ox, oy, radius,
                colors, null, Shader.TileMode.CLAMP
            )
            val paint = Paint().apply {
                isAntiAlias = true
                this.shader = shader
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }

        // Angular (sweep): create SweepGradient and rotate it using a Matrix
        GradientType.Angular -> {
            val sweep = SweepGradient(cx, cy, colors, null)
            val matrix = android.graphics.Matrix()
            matrix.setRotate(angleDeg, cx, cy)
            try {
                sweep.setLocalMatrix(matrix)
            } catch (e: Exception) {
                // Some Android/VM combos may not support setLocalMatrix — ignore
            }
            val paint = Paint().apply {
                isAntiAlias = true
                shader = sweep
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }

    if (addNoise) {
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val baseDp = 1f
        val density = (context.resources.displayMetrics.density).coerceAtLeast(1f)
        val noiseSizePx = (baseDp * density).coerceAtLeast(1f)

        val numNoisePoints =
            ((width.toLong() * height.toLong()) / (noiseSizePx.toLong() * noiseSizePx.toLong()) * 0.02f).toInt()
                .coerceAtLeast(200)

        val rnd = Random(System.currentTimeMillis())

        repeat(numNoisePoints) {
            val x = rnd.nextFloat() * width
            val y = rnd.nextFloat() * height

            val alpha = ((rnd.nextFloat() * 0.15f) * noiseAlpha).coerceIn(0f, 1f)
            val alphaInt = (alpha * 255).roundToInt().coerceIn(0, 255)
            paint.color = android.graphics.Color.argb(alphaInt, 255, 255, 255)

            val radius = noiseSizePx * (0.6f + rnd.nextFloat() * 1.2f)
            canvas.drawCircle(x, y, radius, paint)
        }
    }

    if (addStripes) {
        val paintStripe = Paint().apply { isAntiAlias = true }
        val stripeCount = 18
        val stripeWidth = width.toFloat() / (stripeCount * 2f)

        repeat(stripeCount) { i ->
            val left = i * stripeWidth * 2f
            val right = left + stripeWidth
            // FIX: The base alpha was hardcoded. Now it's correctly scaled by the stripesAlpha parameter.
            val alphaStripe = 0.09f * stripesAlpha
            val alphaInt = (alphaStripe * 255).roundToInt().coerceIn(0, 255)
            paintStripe.color = android.graphics.Color.argb(alphaInt, 255, 255, 255)
            canvas.drawRect(left, 0f, right, height.toFloat(), paintStripe)
        }
    }

    if (addOverlay) {
        try {
            val overlay = android.graphics.BitmapFactory.decodeResource(
                context.resources,
                R.drawable.overlay_stripes
            )

            // FIX: A separate Paint object is now used to apply the overlayAlpha.
            // The previous implementation drew the bitmap with a null paint, ignoring the alpha.
            val paint = Paint().apply {
                isAntiAlias = true
                alpha = (overlayAlpha.coerceIn(0f, 1f) * 255f).toInt()
            }

            val scaled = overlay.scale(width, height)
            canvas.drawBitmap(scaled, 0f, 0f, paint)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    if (addGeometric) {
        try {
            val overlay = android.graphics.BitmapFactory.decodeResource(
                context.resources,
                R.drawable.overlay_geometric
            )
            val scale = width.toFloat() / overlay.width.toFloat()
            val scaledWidth = width
            val scaledHeight = (overlay.height * scale).roundToInt()
            val scaled = overlay.scale(scaledWidth, scaledHeight)
            val topOffset = ((height - scaledHeight) / 2f).coerceAtMost(0f)

            val paint = Paint().apply {
                isAntiAlias = true
                alpha = (geometricAlpha.coerceIn(0f, 1f) * 255f).roundToInt()
            }

            canvas.drawBitmap(scaled, 0f, topOffset, paint)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    return bmp
}

/**
 * Save a bitmap to MediaStore (Pictures/Waller).
 */
fun saveBitmapToMediaStore(context: Context, bitmap: Bitmap, displayName: String): Boolean {
    return try {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/Waller"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri: Uri = resolver.insert(collection, contentValues) ?: return false

        resolver.openOutputStream(uri)?.use { out ->
            val compressed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            if (!compressed) {
                resolver.delete(uri, null, null)
                return false
            }
        } ?: run {
            resolver.delete(uri, null, null)
            return false
        }

        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

/**
 * Apply bitmap as wallpaper.
 */
fun tryApplyWallpaper(
    context: Context,
    bitmap: Bitmap,
    flags: Int = WallpaperManager.FLAG_SYSTEM
): Boolean {
    return try {
        val manager = WallpaperManager.getInstance(context)
        manager.setBitmap(bitmap, null, true, flags)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

/** helper to return lock flag (or 0 if not supported) */
fun getLockFlag(): Int {
    return FLAG_LOCK
}
