/**
 * SvgExporter.kt
 *
 * Generates .svg and .css files from a wallpaper configuration.
 * Rendering matches BitmapUtils.createGradientBitmap as closely as possible.
 *
 * Gradient math mirrors BitmapUtils exactly:
 *   Linear  → start/end coords from cos/sin of angleDeg, anchored at canvas center
 *   Diamond → linear at (angleDeg - 45°), same center anchor
 *   Radial  → radialGradient with 0.22 center shift in angle direction
 *   Angular → conic-gradient (CSS) / foreignObject (SVG)
 *
 * Stripes: BitmapUtils rotates canvas -45°, draws vertical gradient rects
 *   spacing = width/12, stripeWidth = spacing/2
 *   SVG: <pattern> with patternTransform="rotate(-45, cx, cy)" + inner linearGradient
 *
 * Noise: BitmapUtils draws random white dots — feTurbulence is closest SVG equivalent.
 *
 * Glass  (overlay_stripes.png)   → base64 <image>
 * Geo    (overlay_geometric.png) → base64 <image>
 */

package com.example.waller.ui.wallfile

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.waller.ui.wallpaper.FavoriteWallpaper
import com.example.waller.ui.wallpaper.GradientType
import java.io.File
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

object SvgExporter {

    private const val W = 1080
    private const val H = 1920

    // ─────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────

    fun exportSvg(context: Context, fav: FavoriteWallpaper): File {
        val file = File(context.cacheDir, "waller_${System.currentTimeMillis()}.svg")
        file.writeText(buildSvg(context, fav))
        return file
    }

    fun exportCss(context: Context, fav: FavoriteWallpaper): File {
        val file = File(context.cacheDir, "waller_${System.currentTimeMillis()}.css")
        file.writeText(buildCss(context, fav))
        return file
    }

    // ─────────────────────────────────────────────
    // SVG Builder
    // ─────────────────────────────────────────────

    private fun buildSvg(context: Context, fav: FavoriteWallpaper): String {
        val w        = fav.wallpaper
        val hex      = w.colors.map { it.toHex() }
        val angleDeg = w.angleDeg
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val cx       = W / 2f
        val cy       = H / 2f

        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<svg xmlns="http://www.w3.org/2000/svg" width="$W" height="$H" viewBox="0 0 $W $H">""")
        sb.appendLine("""  <defs>""")

        // ── Gradient definition ───────────────────────────────────────────────
        when (w.type) {
            GradientType.Linear -> {
                val dx = cos(angleRad); val dy = sin(angleRad)
                sb.appendLine("""    <linearGradient id="grad" x1="${(cx - dx * W / 2.0).fmt()}" y1="${(cy - dy * H / 2.0).fmt()}" x2="${(cx + dx * W / 2.0).fmt()}" y2="${(cy + dy * H / 2.0).fmt()}" gradientUnits="userSpaceOnUse">""")
                appendStops(sb, hex)
                sb.appendLine("""    </linearGradient>""")
            }
            GradientType.Diamond -> {
                val a = Math.toRadians(-45.0 + angleDeg)
                val dx = cos(a); val dy = sin(a)
                sb.appendLine("""    <linearGradient id="grad" x1="${(cx - dx * H / 2.0).fmt()}" y1="${(cy - dy * H / 2.0).fmt()}" x2="${(cx + dx * H / 2.0).fmt()}" y2="${(cy + dy * H / 2.0).fmt()}" gradientUnits="userSpaceOnUse">""")
                appendStops(sb, hex)
                sb.appendLine("""    </linearGradient>""")
            }
            GradientType.Radial -> {
                val radius = max(W, H) * 0.6
                val ox = cx + cos(angleRad) * radius * 0.22
                val oy = cy + sin(angleRad) * radius * 0.22
                sb.appendLine("""    <radialGradient id="grad" cx="${ox.fmt()}" cy="${oy.fmt()}" r="${radius.fmt()}" gradientUnits="userSpaceOnUse">""")
                appendStops(sb, hex)
                sb.appendLine("""    </radialGradient>""")
            }
            GradientType.Angular -> { /* rendered via foreignObject below */ }
        }

        // ── Stripes pattern ───────────────────────────────────────────────────
        if (fav.addStripes && fav.stripesAlpha > 0f) {
            val spacing = W / 12f
            val stripeW = spacing / 2f
            val opacity = (0.18f * fav.stripesAlpha).fmtF()
            sb.appendLine("""    <linearGradient id="sg" x1="0" y1="0" x2="${stripeW.fmt()}" y2="0" gradientUnits="userSpaceOnUse">""")
            sb.appendLine("""      <stop offset="0%" stop-color="white" stop-opacity="$opacity"/>""")
            sb.appendLine("""      <stop offset="100%" stop-color="white" stop-opacity="0"/>""")
            sb.appendLine("""    </linearGradient>""")
            sb.appendLine("""    <pattern id="stripes" x="0" y="0" width="${spacing.fmt()}" height="${(H * 3).toFloat().fmt()}" patternUnits="userSpaceOnUse" patternTransform="rotate(-45 ${cx.fmt()} ${cy.fmt()})">""")
            sb.appendLine("""      <rect x="0" y="0" width="${stripeW.fmt()}" height="${(H * 3).toFloat().fmt()}" fill="url(#sg)"/>""")
            sb.appendLine("""    </pattern>""")
        }

        // ── Noise filter ──────────────────────────────────────────────────────
        if (fav.addNoise && fav.noiseAlpha > 0f) {
            sb.appendLine("""    <filter id="noise" x="0%" y="0%" width="100%" height="100%">""")
            sb.appendLine("""      <feTurbulence type="fractalNoise" baseFrequency="0.65" numOctaves="3" stitchTiles="stitch"/>""")
            sb.appendLine("""    </filter>""")
        }

        sb.appendLine("""  </defs>""")

        // ── Layer 1: gradient ─────────────────────────────────────────────────
        if (w.type == GradientType.Angular) {
            val rotDeg = angleDeg.roundToInt()
            val stops  = hex.mapIndexed { i, h ->
                "$h ${(i.toFloat() / (hex.size - 1) * 100).roundToInt()}%"
            }.joinToString(", ")
            sb.appendLine("""  <foreignObject x="0" y="0" width="$W" height="$H">""")
            sb.appendLine("""    <div xmlns="http://www.w3.org/1999/xhtml" style="width:${W}px;height:${H}px;background:conic-gradient(from ${rotDeg}deg at 50% 50%, $stops)"/>""")
            sb.appendLine("""  </foreignObject>""")
        } else {
            sb.appendLine("""  <rect width="$W" height="$H" fill="url(#grad)"/>""")
        }

        // ── Layer 2: Noise ────────────────────────────────────────────────────
        if (fav.addNoise && fav.noiseAlpha > 0f) {
            val op = (fav.noiseAlpha * 0.18f).fmtF()
            sb.appendLine("""  <rect width="$W" height="$H" filter="url(#noise)" opacity="$op" style="mix-blend-mode:screen"/>""")
        }

        // ── Layer 3: Stripes ──────────────────────────────────────────────────
        if (fav.addStripes && fav.stripesAlpha > 0f) {
            sb.appendLine("""  <rect width="$W" height="$H" fill="url(#stripes)"/>""")
        }

        // ── Layer 4: Glass overlay (overlay_stripes.png from res/drawable, base64) ──
        if (fav.addOverlay && fav.overlayAlpha > 0f) {
            val b64 = drawablePngAsBase64(context, "overlay_stripes")
            if (b64 != null) {
                sb.appendLine("""  <image href="data:image/png;base64,$b64" x="0" y="0" width="$W" height="$H" preserveAspectRatio="xMidYMid slice" opacity="${fav.overlayAlpha.fmtF()}" style="mix-blend-mode:screen"/>""")
            }
        }

        // ── Layer 5: Geometry overlay (overlay_geometric.png from res/drawable, base64) ─
        if (fav.addGeometric && fav.geometricAlpha > 0f) {
            val b64 = drawablePngAsBase64(context, "overlay_geometric")
            if (b64 != null) {
                sb.appendLine("""  <image href="data:image/png;base64,$b64" x="0" y="0" width="$W" height="$H" preserveAspectRatio="xMidYMid slice" opacity="${fav.geometricAlpha.fmtF()}"/>""")
            }
        }

        sb.appendLine("""</svg>""")
        return sb.toString()
    }

    private fun buildCss(context: Context, fav: FavoriteWallpaper): String {
        val w       = fav.wallpaper
        val hex     = w.colors.map { it.toHex() }
        val angleDeg = w.angleDeg

        val colorStops = hex.mapIndexed { i, h ->
            "$h ${(i.toFloat() / (hex.size - 1) * 100).roundToInt()}%"
        }.joinToString(", ")

        // CSS angle: CSS 0°=up clockwise, BitmapUtils 0°=right (cos/sin East)
        // Conversion: cssAngle = 90 - bitmapAngle
        val cssAngle = ((90f - angleDeg).mod(360f)).roundToInt()

        val gradientCss = when (w.type) {
            GradientType.Linear  -> "linear-gradient(${cssAngle}deg, $colorStops)"
            GradientType.Diamond -> "linear-gradient(${((90f - (angleDeg - 45f)).mod(360f)).roundToInt()}deg, $colorStops)"
            GradientType.Radial  -> "radial-gradient(ellipse at center, $colorStops)"
            GradientType.Angular -> "conic-gradient(from ${angleDeg.roundToInt()}deg at 50% 50%, $colorStops)"
        }

        // Stripes: match BitmapUtils -45° canvas rotation, spacing=W/12, stripeWidth=spacing/2
        val spacing = W / 12f
        val stripeW = spacing / 2f

        val sb = StringBuilder()
        sb.appendLine("/* Generated by Waller — ${w.type.name} gradient, ${w.colors.size} colors */")
        sb.appendLine()
        sb.appendLine(":root {")
        hex.forEachIndexed { i, h -> sb.appendLine("  --waller-color-$i: $h;") }
        sb.appendLine("}")
        sb.appendLine()

        sb.appendLine(".waller-wallpaper {")
        sb.appendLine("  width: ${W}px;")
        sb.appendLine("  height: ${H}px;")
        sb.appendLine("  position: relative;")
        sb.appendLine("  overflow: hidden;")
        sb.appendLine("  background: $gradientCss;")
        if (fav.addBlur && fav.blurAlpha > 0f) {
            sb.appendLine("  filter: blur(${(18f * fav.blurAlpha).roundToInt()}px);")
        }
        sb.appendLine("}")
        sb.appendLine()

        // Snow/noise
        if (fav.addNoise && fav.noiseAlpha > 0f) {
            val opacity = (fav.noiseAlpha * 0.12f).fmtF()
            sb.appendLine("/* Snow/noise overlay */")
            sb.appendLine(".waller-wallpaper::before {")
            sb.appendLine("  content: '';")
            sb.appendLine("  position: absolute; inset: 0;")
            sb.appendLine("  background-image: url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='200' height='200'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.75' numOctaves='4' stitchTiles='stitch'/%3E%3CfeColorMatrix type='matrix' values='0 0 0 0 1 0 0 0 0 1 0 0 0 0 1 0 0 0 $opacity 0'/%3E%3C/filter%3E%3Crect width='200' height='200' filter='url(%23n)'/%3E%3C/svg%3E\");")
            sb.appendLine("  background-size: 200px 200px;")
            sb.appendLine("  mix-blend-mode: screen;")
            sb.appendLine("  pointer-events: none;")
            sb.appendLine("}")
            sb.appendLine()
        }

        // Refraction/stripes — matches -45° rotated vertical gradient rects
        if (fav.addStripes && fav.stripesAlpha > 0f) {
            val stripeOpacity = (0.18f * fav.stripesAlpha).fmtF()
            sb.appendLine("/* Refraction/stripes overlay */")
            sb.appendLine(".waller-wallpaper::after {")
            sb.appendLine("  content: '';")
            sb.appendLine("  position: absolute; inset: 0;")
            sb.appendLine("  background-image: repeating-linear-gradient(")
            sb.appendLine("    -45deg,")
            sb.appendLine("    rgba(255,255,255,0) 0px,")
            sb.appendLine("    rgba(255,255,255,$stripeOpacity) ${stripeW.roundToInt()}px,")
            sb.appendLine("    rgba(255,255,255,0) ${spacing.roundToInt()}px")
            sb.appendLine("  );")
            sb.appendLine("  pointer-events: none;")
            sb.appendLine("}")
            sb.appendLine()
        }

        // Glass overlay
        if (fav.addOverlay && fav.overlayAlpha > 0f) {
            val b64 = drawablePngAsBase64(context, "overlay_stripes")
            if (b64 != null) {
                sb.appendLine("/* Glass overlay */")
                sb.appendLine(".waller-glass {")
                sb.appendLine("  position: absolute; inset: 0;")
                sb.appendLine("  background: url('data:image/png;base64,$b64') center/cover;")
                sb.appendLine("  opacity: ${fav.overlayAlpha.fmtF()};")
                sb.appendLine("  mix-blend-mode: screen;")
                sb.appendLine("  pointer-events: none;")
                sb.appendLine("}")
                sb.appendLine()
            }
        }

        // Geometry overlay
        if (fav.addGeometric && fav.geometricAlpha > 0f) {
            val b64 = drawablePngAsBase64(context, "overlay_geometric")
            if (b64 != null) {
                sb.appendLine("/* Geometry overlay */")
                sb.appendLine(".waller-geo {")
                sb.appendLine("  position: absolute; inset: 0;")
                sb.appendLine("  background: url('data:image/png;base64,$b64') center/cover;")
                sb.appendLine("  opacity: ${fav.geometricAlpha.fmtF()};")
                sb.appendLine("  pointer-events: none;")
                sb.appendLine("}")
                sb.appendLine()
            }
        }

        sb.appendLine("/*")
        sb.appendLine(" * Usage:")
        sb.appendLine(" * <div class=\"waller-wallpaper\">")
        if (fav.addOverlay)   sb.appendLine(" *   <div class=\"waller-glass\"></div>")
        if (fav.addGeometric) sb.appendLine(" *   <div class=\"waller-geo\"></div>")
        sb.appendLine(" * </div>")
        sb.appendLine(" */")

        return sb.toString()
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private fun appendStops(sb: StringBuilder, hexColors: List<String>) {
        hexColors.forEachIndexed { i, hex ->
            val offset = if (hexColors.size == 1) 0
            else (i.toFloat() / (hexColors.size - 1) * 100).roundToInt()
            sb.appendLine("""      <stop offset="$offset%" stop-color="$hex"/>""")
        }
    }

    /**
     * Reads an SVG file from res/raw by name (no extension).
     * Returns the full SVG text, or null if not found.
     */
    /** Reads a PNG from res/drawable by name and returns it as a Base64 string for data-uri embedding. */
    private fun drawablePngAsBase64(context: Context, name: String): String? {
        return try {
            val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
            if (resId == 0) return null
            val bytes = context.resources.openRawResource(resId).use { it.readBytes() }
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun Color.toHex(): String = "#%06X".format(this.toArgb() and 0xFFFFFF)
    private fun Double.fmt(): String  = "%.2f".format(this)
    private fun Float.fmt(): String   = "%.2f".format(this)
    private fun Float.fmtF(): String  = "%.3f".format(this)
}