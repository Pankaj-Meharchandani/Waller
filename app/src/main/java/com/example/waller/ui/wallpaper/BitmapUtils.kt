/**
 * Core bitmap generation engine for Waller.
 *
 * Responsibilities:
 * - Detect device screen size (portrait/landscape)
 * - Convert Compose gradients to Android shaders (Linear/Radial/Sweep)
 * - Draw final gradient + effects into a single bitmap
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
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.example.waller.R
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

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
    effects: EffectMap = WallpaperEffects.defaultMap()
): Bitmap {
    val (width, height) = getScreenSizeForBitmap(context, isPortrait)
    val bmp = createBitmap(width, height)
    val canvas = android.graphics.Canvas(bmp)

    drawWallpaperOnCanvas(
        context = context,
        canvas = canvas,
        width = width,
        height = height,
        wallpaper = wallpaper,
        effects = effects
    )

    // Apply blur if enabled (it's the only effect that needs the full bitmap as input)
    effects["blur"]?.let { state ->
        if (state.enabled && state.alpha > 0f) {
            try {
                val radius  = (25f * state.alpha.coerceIn(0f, 1f)).coerceIn(1f, 25f).toInt()
                val blurred = stackBlur(bmp, radius)
                val pixels  = IntArray(bmp.width * bmp.height)
                blurred.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
                bmp.setPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    return bmp
}

fun drawWallpaperOnCanvas(
    context: Context,
    canvas: android.graphics.Canvas,
    width: Int,
    height: Int,
    wallpaper: Wallpaper,
    effects: EffectMap,
    cachedOverlay: Bitmap? = null,
    cachedGeometric: Bitmap? = null
) {
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

    val cx = width / 2f
    val cy = height / 2f

    // ── Gradient base layer ────────────────────────────────────────────────
    when (wallpaper.type) {
        GradientType.Linear -> {
            val dx = kotlin.math.cos(a)
            val dy = kotlin.math.sin(a)
            val halfW = width / 2f
            val halfH = height / 2f
            val shader = LinearGradient(
                cx - dx * halfW, cy - dy * halfH,
                cx + dx * halfW, cy + dy * halfH,
                colors, null, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply {
                isAntiAlias = true; this.shader = shader
            })
        }

        GradientType.Diamond -> {
            val halfW = width / 2f
            val halfH = height / 2f
            val shader = LinearGradient(
                cx, cy - halfH, cx, cy + halfH,
                colors, null, Shader.TileMode.CLAMP
            )
            val matrix = android.graphics.Matrix()
            matrix.setRotate(-45f + angleDeg, cx, cy)
            shader.setLocalMatrix(matrix)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply {
                isAntiAlias = true; this.shader = shader
            })
        }

        GradientType.Radial -> {
            val radius = max(width, height) * 0.6f
            val ox = cx + kotlin.math.cos(a) * radius * 0.22f
            val oy = cy + kotlin.math.sin(a) * radius * 0.22f
            val shader = RadialGradient(ox, oy, radius, colors, null, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply {
                isAntiAlias = true; this.shader = shader
            })
        }

        GradientType.Angular -> {
            val sweep = SweepGradient(cx, cy, colors, null)
            val matrix = android.graphics.Matrix()
            matrix.setRotate(angleDeg, cx, cy)
            try { sweep.setLocalMatrix(matrix) } catch (_: Exception) {}
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply {
                isAntiAlias = true; shader = sweep
            })
        }
    }

    // ── Effect layers ──────────────────────────────────────────────────────
    for (def in WallpaperEffects.ALL) {
        val state = effects[def.id] ?: continue
        if (!state.enabled || state.alpha <= 0f || def.id == "blur") continue
        val alpha = state.alpha

        when (def.id) {
            "noise" -> {
                val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
                val density = context.resources.displayMetrics.density.coerceAtLeast(1f)
                val noiseSizePx = (1f * density).coerceAtLeast(1f)
                val numPoints = ((width.toLong() * height.toLong()) /
                        (noiseSizePx.toLong() * noiseSizePx.toLong()) * 0.02f)
                    .toInt().coerceAtLeast(200)

                // Use angleDeg to seed the random number generator.
                // This ensures snow speed scales with live animation speed.
                val seed = (angleDeg * 2f).toLong()
                val rnd = Random(seed)

                repeat(numPoints) {
                    val x = rnd.nextFloat() * width
                    val y = rnd.nextFloat() * height
                    val a2 = ((rnd.nextFloat() * 0.15f) * alpha).coerceIn(0f, 1f)
                    paint.color = android.graphics.Color.argb((a2 * 255).roundToInt(), 255, 255, 255)
                    val r = noiseSizePx * (0.6f + rnd.nextFloat() * 1.2f)
                    canvas.drawCircle(x, y, r, paint)
                }
            }

            "stripes" -> {
                val stripeSpacing = width / 12f
                val stripeWidth   = stripeSpacing / 2f
                val stripeColor   = android.graphics.Color.argb(
                    (0.18f * alpha * 255).roundToInt().coerceIn(0, 255), 255, 255, 255
                )
                val paintStripe = Paint().apply { isAntiAlias = true }
                canvas.save()
                canvas.rotate(-45f, cx, cy)
                var x = -height.toFloat()
                while (x < width * 2f) {
                    paintStripe.shader = LinearGradient(
                        x, 0f, x + stripeWidth, 0f,
                        stripeColor, android.graphics.Color.TRANSPARENT, Shader.TileMode.CLAMP
                    )
                    canvas.drawRect(x, -height.toFloat(), x + stripeWidth, height * 2f, paintStripe)
                    x += stripeSpacing
                }
                canvas.restore()
            }

            "overlay" -> {
                try {
                    val paint = Paint().apply {
                        isAntiAlias = true
                        this.alpha = (alpha.coerceIn(0f, 1f) * 255f).toInt()
                    }
                    if (cachedOverlay != null) {
                        canvas.drawBitmap(cachedOverlay, 0f, 0f, paint)
                    } else {
                        val overlay = android.graphics.BitmapFactory.decodeResource(
                            context.resources, R.drawable.overlay_stripes
                        )
                        canvas.drawBitmap(overlay.scale(width, height), 0f, 0f, paint)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            "geometric" -> {
                try {
                    val paint = Paint().apply {
                        isAntiAlias = true
                        this.alpha = (alpha.coerceIn(0f, 1f) * 255f).roundToInt()
                    }
                    if (cachedGeometric != null) {
                        val topOffset = ((height - cachedGeometric.height) / 2f).coerceAtMost(0f)
                        canvas.drawBitmap(cachedGeometric, 0f, topOffset, paint)
                    } else {
                        val overlay = android.graphics.BitmapFactory.decodeResource(
                            context.resources, R.drawable.overlay_geometric
                        )
                        val scale = width.toFloat() / overlay.width.toFloat()
                        val scaledW = width
                        val scaledH = (overlay.height * scale).roundToInt()
                        val scaled  = overlay.scale(scaledW, scaledH)
                        val topOffset = ((height - scaledH) / 2f).coerceAtMost(0f)
                        canvas.drawBitmap(scaled, 0f, topOffset, paint)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }
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
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Waller")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri: Uri = resolver.insert(collection, contentValues) ?: return false
        resolver.openOutputStream(uri)?.use { out ->
            val compressed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            if (!compressed) { resolver.delete(uri, null, null); return false }
        } ?: run { resolver.delete(uri, null, null); return false }
        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
        true
    } catch (e: Exception) { e.printStackTrace(); false }
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
        WallpaperManager.getInstance(context).setBitmap(bitmap, null, true, flags)
        true
    } catch (e: Exception) { e.printStackTrace(); false }
}

/** Returns lock screen flag (or 0 if not supported). */
fun getLockFlag(): Int = FLAG_LOCK

/**
 * Pure-Kotlin Stack Blur — no RenderScript, no external libraries.
 * Produces a true Gaussian-like blur. Radius 1–25 px.
 */
fun stackBlur(src: Bitmap, radius: Int): Bitmap {
    val r = radius.coerceIn(1, 25)
    val w = src.width; val h = src.height
    val pix = IntArray(w * h)
    src.getPixels(pix, 0, w, 0, 0, w, h)
    val wm = w - 1; val hm = h - 1
    val div = r + r + 1
    val vmin = IntArray(maxOf(w, h))
    var divSum = (div + 1) shr 1; divSum *= divSum
    val dv = IntArray(256 * divSum)
    for (i in dv.indices) dv[i] = i / divSum
    var yw = 0; var yi = 0
    val stack = Array(div) { IntArray(3) }
    for (y in 0 until h) {
        var rSum = 0; var gSum = 0; var bSum = 0
        var rInSum = 0; var gInSum = 0; var bInSum = 0
        var rOutSum = 0; var gOutSum = 0; var bOutSum = 0
        for (i in -r..r) {
            val p = pix[yi + minOf(wm, maxOf(0, i))]
            val sir = stack[i + r]
            sir[0] = (p and 0xff0000) shr 16; sir[1] = (p and 0x00ff00) shr 8; sir[2] = p and 0x0000ff
            val rbs = r + 1 - kotlin.math.abs(i)
            rSum += sir[0] * rbs; gSum += sir[1] * rbs; bSum += sir[2] * rbs
            if (i > 0) { rInSum += sir[0]; gInSum += sir[1]; bInSum += sir[2] }
            else { rOutSum += sir[0]; gOutSum += sir[1]; bOutSum += sir[2] }
        }
        var stackPointer = r
        for (x in 0 until w) {
            pix[yi] = (pix[yi] and -0x1000000) or (dv[rSum] shl 16) or (dv[gSum] shl 8) or dv[bSum]
            rSum -= rOutSum; gSum -= gOutSum; bSum -= bOutSum
            val stackStart = (stackPointer - r + div) % div
            val sir2 = stack[stackStart]
            rOutSum -= sir2[0]; gOutSum -= sir2[1]; bOutSum -= sir2[2]
            val px = yw + minOf(x + r + 1, wm)
            sir2[0] = (pix[px] and 0xff0000) shr 16; sir2[1] = (pix[px] and 0x00ff00) shr 8; sir2[2] = pix[px] and 0x0000ff
            rInSum += sir2[0]; gInSum += sir2[1]; bInSum += sir2[2]
            rSum += rInSum; gSum += gInSum; bSum += bInSum
            stackPointer = (stackPointer + 1) % div
            val sir3 = stack[stackPointer]
            rOutSum += sir3[0]; gOutSum += sir3[1]; bOutSum += sir3[2]
            rInSum -= sir3[0]; gInSum -= sir3[1]; bInSum -= sir3[2]
            yi++
        }
        yw += w
    }
    for (x in 0 until w) {
        var rSum = 0; var gSum = 0; var bSum = 0
        var rInSum = 0; var gInSum = 0; var bInSum = 0
        var rOutSum = 0; var gOutSum = 0; var bOutSum = 0
        val yp = -r * w
        for (i in -r..r) {
            yi = maxOf(0, yp + i * w) + x
            val sir = stack[i + r]
            sir[0] = (pix[yi] and 0xff0000) shr 16; sir[1] = (pix[yi] and 0x00ff00) shr 8; sir[2] = pix[yi] and 0x0000ff
            val rbs = r + 1 - kotlin.math.abs(i)
            rSum += sir[0] * rbs; gSum += sir[1] * rbs; bSum += sir[2] * rbs
            if (i > 0) { rInSum += sir[0]; gInSum += sir[1]; bInSum += sir[2] }
            else { rOutSum += sir[0]; gOutSum += sir[1]; bOutSum += sir[2] }
        }
        yi = x
        var stackPointer = r
        for (y in 0 until h) {
            pix[yi] = (pix[yi] and -0x1000000) or (dv[rSum] shl 16) or (dv[gSum] shl 8) or dv[bSum]
            rSum -= rOutSum; gSum -= gOutSum; bSum -= bOutSum
            val stackStart = (stackPointer - r + div) % div
            val sir2 = stack[stackStart]
            rOutSum -= sir2[0]; gOutSum -= sir2[1]; bOutSum -= sir2[2]
            val py = minOf(y + r + 1, hm) * w
            sir2[0] = (pix[py + x] and 0xff0000) shr 16; sir2[1] = (pix[py + x] and 0x00ff00) shr 8; sir2[2] = pix[py + x] and 0x0000ff
            rInSum += sir2[0]; gInSum += sir2[1]; bInSum += sir2[2]
            rSum += rInSum; gSum += gInSum; bSum += bInSum
            stackPointer = (stackPointer + 1) % div
            val sir3 = stack[stackPointer]
            rOutSum += sir3[0]; gOutSum += sir3[1]; bOutSum += sir3[2]
            rInSum -= sir3[0]; gInSum -= sir3[1]; bInSum -= sir3[2]
            yi += w
        }
    }
    val out = src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
    out.setPixels(pix, 0, w, 0, 0, w, h)
    return out
}
