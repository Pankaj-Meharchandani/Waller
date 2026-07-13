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
 *
 * Uses EffectMap — no per-effect named fields. Adding a new effect only requires
 * a new `when (id)` branch in buildSvg and buildCss below.
 */

package com.example.waller.ui.wallfile

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.waller.ui.wallpaper.FavoriteWallpaper
import com.example.waller.ui.wallpaper.GradientType
import com.example.waller.ui.wallpaper.alpha
import com.example.waller.ui.wallpaper.isEnabled
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

        // Convenience accessors via EffectMap
        val addNoise    = fav.effects.isEnabled("noise")
        val noiseAlpha  = fav.effects.alpha("noise")
        val addStripes  = fav.effects.isEnabled("stripes")
        val stripesAlpha = fav.effects.alpha("stripes")
        val addOverlay  = fav.effects.isEnabled("overlay")
        val overlayAlpha = fav.effects.alpha("overlay")
        val addGeo      = fav.effects.isEnabled("geometric")
        val geoAlpha    = fav.effects.alpha("geometric")
        val addBlur     = fav.effects.isEnabled("blur")
        val blurAlpha   = fav.effects.alpha("blur")

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
            GradientType.Pastels -> { /* rendered via circles below */ }
        }

        // ── Stripes pattern ───────────────────────────────────────────────────
        if (addStripes && stripesAlpha > 0f) {
            val spacing = W / 12f
            val stripeW = spacing / 2f
            val opacity = (0.18f * stripesAlpha).fmtF()
            sb.appendLine("""    <linearGradient id="sg" x1="0" y1="0" x2="${stripeW.fmt()}" y2="0" gradientUnits="userSpaceOnUse">""")
            sb.appendLine("""      <stop offset="0%" stop-color="white" stop-opacity="$opacity"/>""")
            sb.appendLine("""      <stop offset="100%" stop-color="white" stop-opacity="0"/>""")
            sb.appendLine("""    </linearGradient>""")
            sb.appendLine("""    <pattern id="stripes" x="0" y="0" width="${spacing.fmt()}" height="${(H * 3).toFloat().fmt()}" patternUnits="userSpaceOnUse" patternTransform="rotate(-45 ${cx.fmt()} ${cy.fmt()})">""")
            sb.appendLine("""      <rect x="0" y="0" width="${stripeW.fmt()}" height="${(H * 3).toFloat().fmt()}" fill="url(#sg)"/>""")
            sb.appendLine("""    </pattern>""")
        }

        // ── Noise filter ──────────────────────────────────────────────────────
        if (addNoise && noiseAlpha > 0f) {
            sb.appendLine("""    <filter id="noise" x="0%" y="0%" width="100%" height="100%">""")
            sb.appendLine("""      <feTurbulence type="fractalNoise" baseFrequency="0.65" numOctaves="3" stitchTiles="stitch"/>""")
            sb.appendLine("""    </filter>""")
        }

        // ── Blur filter ───────────────────────────────────────────────────────
        if (addBlur && blurAlpha > 0f) {
            val stdDev = (20f * blurAlpha).roundToInt().coerceAtLeast(1)
            sb.appendLine("""    <filter id="blur" x="-20%" y="-20%" width="140%" height="140%">""")
            sb.appendLine("""      <feGaussianBlur stdDeviation="$stdDev"/>""")
            sb.appendLine("""    </filter>""")
        }

        sb.appendLine("""  </defs>""")

        // ── Blur group wraps all layers ───────────────────────────────────────
        if (addBlur && blurAlpha > 0f) {
            sb.appendLine("""  <g filter="url(#blur)">""")
        }

        // ── Layer 1: gradient ─────────────────────────────────────────────────
        if (w.type == GradientType.Angular) {
            val rotDeg = angleDeg.roundToInt()
            val stops  = hex.mapIndexed { i, h ->
                "$h ${(i.toFloat() / (hex.size - 1) * 100).roundToInt()}%"
            }.joinToString(", ")
            sb.appendLine("""  <foreignObject x="0" y="0" width="$W" height="$H">""")
            sb.appendLine("""    <div xmlns="http://www.w3.org/1999/xhtml" style="width:${W}px;height:${H}px;background:conic-gradient(from ${rotDeg}deg at 50% 50%, $stops)"/>""")
            sb.appendLine("""  </foreignObject>""")
        } else if (w.type == GradientType.Pastels) {
            sb.appendLine("""  <rect width="$W" height="$H" fill="${hex.first()}"/>""")
            appendPastelsSvg(sb, hex, angleDeg)
        } else {
            sb.appendLine("""  <rect width="$W" height="$H" fill="url(#grad)"/>""")
        }

        // ── Layer 2: Noise ────────────────────────────────────────────────────
        if (addNoise && noiseAlpha > 0f) {
            val op = (noiseAlpha * 0.18f).fmtF()
            sb.appendLine("""  <rect width="$W" height="$H" filter="url(#noise)" opacity="$op" style="mix-blend-mode:screen"/>""")
        }

        // ── Layer 3: Stripes ──────────────────────────────────────────────────
        if (addStripes && stripesAlpha > 0f) {
            sb.appendLine("""  <rect width="$W" height="$H" fill="url(#stripes)"/>""")
        }

        // ── Layer 4: Glass overlay ────────────────────────────────────────────
        if (addOverlay && overlayAlpha > 0f) {
            val b64 = drawablePngAsBase64(context, "overlay_stripes")
            if (b64 != null) {
                sb.appendLine("""  <defs>""")
                sb.appendLine("""    <filter id="glassBlend" x="0" y="0" width="100%" height="100%" color-interpolation-filters="sRGB">""")
                sb.appendLine("""      <feImage href="data:image/png;base64,$b64" result="overlay" preserveAspectRatio="xMidYMid slice"/>""")
                sb.appendLine("""      <feBlend in="BackgroundImage" in2="overlay" mode="screen" result="blended"/>""")
                sb.appendLine("""      <feComposite in="blended" in2="SourceGraphic" operator="over"/>""")
                sb.appendLine("""    </filter>""")
                sb.appendLine("""  </defs>""")
                sb.appendLine("""  <rect width="$W" height="$H" fill="transparent" filter="url(#glassBlend)" opacity="${overlayAlpha.fmtF()}" enable-background="new"/>""")
            }
        }

        // ── Layer 5: Geometry overlay ─────────────────────────────────────────
        if (addGeo && geoAlpha > 0f) {
            val b64 = drawablePngAsBase64(context, "overlay_geometric")
            if (b64 != null) {
                sb.appendLine("""  <image href="data:image/png;base64,$b64" x="0" y="0" width="$W" height="$H" preserveAspectRatio="xMidYMid slice" opacity="${geoAlpha.fmtF()}"/>""")
            }
        }

        // ── Close blur group ──────────────────────────────────────────────────
        if (addBlur && blurAlpha > 0f) {
            sb.appendLine("""  </g>""")
        }

        // ── Add new effect SVG layers here ────────────────────────────────────

        sb.appendLine("""</svg>""")
        return sb.toString()
    }

    // ─────────────────────────────────────────────
    // CSS Builder
    // ─────────────────────────────────────────────

    private fun buildCss(context: Context, fav: FavoriteWallpaper): String {
        val w        = fav.wallpaper
        val hex      = w.colors.map { it.toHex() }
        val angleDeg = w.angleDeg

        // Convenience accessors
        val addNoise    = fav.effects.isEnabled("noise")
        val noiseAlpha  = fav.effects.alpha("noise")
        val addStripes  = fav.effects.isEnabled("stripes")
        val stripesAlpha = fav.effects.alpha("stripes")
        val addOverlay  = fav.effects.isEnabled("overlay")
        val overlayAlpha = fav.effects.alpha("overlay")
        val addGeo      = fav.effects.isEnabled("geometric")
        val geoAlpha    = fav.effects.alpha("geometric")
        val addBlur     = fav.effects.isEnabled("blur")
        val blurAlpha   = fav.effects.alpha("blur")

        fun normAngle(a: Float): Int {
            val v = (a % 360f + 360f) % 360f
            return v.roundToInt()
        }

        val colorStops = hex.mapIndexed { i, h ->
            "$h ${(i.toFloat() / (hex.size - 1) * 100).roundToInt()}%"
        }.joinToString(", ")

        val cssAngle = normAngle(90f - angleDeg)

        val gradientCss = when (w.type) {
            GradientType.Linear  -> "linear-gradient(${cssAngle}deg, $colorStops)"
            GradientType.Diamond -> "linear-gradient(${normAngle(90f - (angleDeg - 45f))}deg, $colorStops)"
            GradientType.Radial  -> "radial-gradient(ellipse at center, $colorStops)"
            GradientType.Angular -> "conic-gradient(from ${angleDeg.roundToInt()}deg at 50% 50%, $colorStops)"
            GradientType.Pastels -> hex.first()
        }

        val spacing = (W / 12f).roundToInt()
        val stripeW = (spacing / 2f)

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
        sb.appendLine("}")
        sb.appendLine()

        if (addBlur && blurAlpha > 0f) {
            val blurPx = (18f * blurAlpha).roundToInt()
            sb.appendLine("/* Gradient + blur layer */")
            sb.appendLine(".waller-gradient {")
            sb.appendLine("  position: absolute;")
            sb.appendLine("  inset: -${blurPx * 2}px;")
            sb.appendLine("  background: $gradientCss;")
            sb.appendLine("  filter: blur(${blurPx}px);")
            sb.appendLine("  pointer-events: none;")
            sb.appendLine("}")
        } else {
            sb.appendLine("/* Gradient layer */")
            sb.appendLine(".waller-gradient {")
            sb.appendLine("  position: absolute;")
            sb.appendLine("  inset: 0;")
            sb.appendLine("  background: $gradientCss;")
            sb.appendLine("  pointer-events: none;")
            sb.appendLine("}")
        }
        sb.appendLine()

        if (addNoise && noiseAlpha > 0f) {
            val opacity = (noiseAlpha * 0.12f).fmtF()
            sb.appendLine("/* Snow/noise overlay */")
            sb.appendLine(".waller-noise {")
            sb.appendLine("  position: absolute; inset: 0;")
            sb.appendLine("  background-image: url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='200' height='200'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.75' numOctaves='4' stitchTiles='stitch'/%3E%3CfeColorMatrix type='matrix' values='0 0 0 0 1 0 0 0 0 1 0 0 0 0 1 0 0 0 $opacity 0'/%3E%3C/filter%3E%3Crect width='200' height='200' filter='url(%23n)'/%3E%3C/svg%3E\");")
            sb.appendLine("  background-size: 200px 200px;")
            sb.appendLine("  mix-blend-mode: screen;")
            sb.appendLine("  pointer-events: none;")
            sb.appendLine("}")
            sb.appendLine()
        }

        if (addStripes && stripesAlpha > 0f) {
            val stripeOpacity = (0.18f * stripesAlpha).fmtF()
            sb.appendLine("/* Refraction/stripes overlay */")
            sb.appendLine(".waller-stripes {")
            sb.appendLine("  position: absolute; inset: 0;")
            sb.appendLine("  background-image: repeating-linear-gradient(")
            sb.appendLine("    -45deg,")
            sb.appendLine("    rgba(255,255,255,0) 0px,")
            sb.appendLine("    rgba(255,255,255,$stripeOpacity) ${stripeW}px,")
            sb.appendLine("    rgba(255,255,255,0) ${spacing}px")
            sb.appendLine("  );")
            sb.appendLine("  pointer-events: none;")
            sb.appendLine("}")
            sb.appendLine()
        }

        if (addOverlay && overlayAlpha > 0f) {
            val b64 = drawablePngAsBase64(context, "overlay_stripes")
            if (!b64.isNullOrEmpty()) {
                sb.appendLine("/* Glass overlay */")
                sb.appendLine(".waller-glass {")
                sb.appendLine("  position: absolute; inset: 0;")
                sb.appendLine("  background: url('data:image/png;base64,$b64') center/cover;")
                sb.appendLine("  opacity: ${overlayAlpha.fmtF()};")
                sb.appendLine("  mix-blend-mode: screen;")
                sb.appendLine("  pointer-events: none;")
                sb.appendLine("}")
                sb.appendLine()
            }
        }

        if (addGeo && geoAlpha > 0f) {
            val b64 = drawablePngAsBase64(context, "overlay_geometric")
            if (!b64.isNullOrEmpty()) {
                sb.appendLine("/* Geometry overlay */")
                sb.appendLine(".waller-geo {")
                sb.appendLine("  position: absolute; inset: 0;")
                sb.appendLine("  background: url('data:image/png;base64,$b64') center/cover;")
                sb.appendLine("  opacity: ${geoAlpha.fmtF()};")
                sb.appendLine("  pointer-events: none;")
                sb.appendLine("}")
                sb.appendLine()
            }
        }

        // ── Add new CSS effect layers here ────────────────────────────────────

        sb.appendLine("/*")
        sb.appendLine(" * Usage:")
        sb.appendLine(" * <div class=\"waller-wallpaper\">")
        sb.appendLine(" *   <div class=\"waller-gradient\"></div>")
        if (addNoise)   sb.appendLine(" *   <div class=\"waller-noise\"></div>")
        if (addStripes) sb.appendLine(" *   <div class=\"waller-stripes\"></div>")
        if (addOverlay) sb.appendLine(" *   <div class=\"waller-glass\"></div>")
        if (addGeo)     sb.appendLine(" *   <div class=\"waller-geo\"></div>")
        sb.appendLine(" * </div>")
        sb.appendLine(" */")

        return sb.toString()
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private fun appendPastelsSvg(sb: StringBuilder, hex: List<String>, angleDeg: Float) {
        val focusX = W * 0.95f
        val focusY = H * 0.45f
        val outer = kotlin.math.hypot(W.toFloat(), H.toFloat()) * 1.2f
        val count = 20
        val step = (Math.PI.toFloat() * 2f) / count
        val phase = Math.toRadians((angleDeg * 0.1f).toDouble()).toFloat()

        repeat(count) { index ->
            val angle = phase + index * step
            val cx = focusX + kotlin.math.cos(angle) * outer * 0.45f
            val cy = focusY + kotlin.math.sin(angle) * outer * 0.45f
            val r = outer * 0.65f
            val color = hex[index % hex.size]
            sb.appendLine("""  <circle cx="${cx.fmt()}" cy="${cy.fmt()}" r="${r.fmt()}" fill="$color" opacity="0.12"/>""")
        }
    }

    private fun appendStops(sb: StringBuilder, hexColors: List<String>) {
        hexColors.forEachIndexed { i, hex ->
            val offset = if (hexColors.size == 1) 0
            else (i.toFloat() / (hexColors.size - 1) * 100).roundToInt()
            sb.appendLine("""      <stop offset="$offset%" stop-color="$hex"/>""")
        }
    }

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

    private fun Color.toHex(): String  = "#%06X".format(this.toArgb() and 0xFFFFFF)
    private fun Double.fmt(): String   = "%.2f".format(this)
    private fun Float.fmt(): String    = "%.2f".format(this)
    private fun Float.fmtF(): String   = "%.3f".format(this)
}