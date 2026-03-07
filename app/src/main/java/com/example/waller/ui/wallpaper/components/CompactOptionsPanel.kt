/**
 * Options panel — responsive, no scroll, no hidden content.
 *
 * Row 1 — Colors:   [●][●]  [+ Add Color]  ────  [Multi-color]
 * Row 2 — Gradient: [Linear][Radial][Angular][Diamond]
 * Row 3 — Effects:  [Glass][Stripes][Snow][Geo][Glow][Dust]
 * Row 4 — Tone:     [Dark Tones][Neutral][Light Tones]
 *
 * Font sizing:
 * - Global scale from screen width (360dp baseline)
 * - Effect chips: BoxWithConstraints → font from actual chip width
 * - Nothing ever overflows — text shrinks to fit
 */
package com.example.waller.ui.wallpaper.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waller.R
import com.example.waller.ui.wallpaper.GradientType
import com.example.waller.ui.wallpaper.Haptics
import com.example.waller.ui.wallpaper.ToneMode

private val ChipCorner  = 14.dp
private val RowSpacing  = 10.dp
private val ChipSpacing = 8.dp

@Composable
fun CompactOptionsPanel(
    toneMode: ToneMode,
    onToneChange: (ToneMode) -> Unit,
    selectedColors: List<Color>,
    onAddColor: () -> Unit,
    onRemoveColor: (Int) -> Unit,
    selectedGradientTypes: List<GradientType>,
    isMultiColor: Boolean,
    onMultiColorChange: (Boolean) -> Unit,
    onGradientToggle: (GradientType) -> Unit,
    addNoise: Boolean,
    onNoiseToggle: () -> Unit,
    addStripes: Boolean,
    onStripesToggle: () -> Unit,
    addOverlay: Boolean,
    onOverlayToggle: () -> Unit,
    addGeometric: Boolean,
    onGeometricToggle: () -> Unit,
) {
    val view   = LocalView.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Scale factor: 360dp screen = 1.0 baseline
    val screenW = LocalConfiguration.current.screenWidthDp
    val scale   = (screenW / 360f).coerceIn(0.82f, 1.20f)

    // Scaled dimension helpers
    val gradientChipH = (35 * scale).dp  // gradient row
    val effectChipH   = (46 * scale).dp  // effects row
    val colorH = (30 * scale).dp  // color row
    val spacing  = (ChipSpacing.value * scale).dp
    val rowGap   = (RowSpacing.value * scale).dp

    data class EffectItem(val iconKey: String, val label: String, val selected: Boolean, val onClick: () -> Unit)
    // Effect icon keys — rendered as canvas-drawn shapes, not emoji
    val effects = listOf(
        EffectItem("glass", stringResource(R.string.effects_nothing_style), addOverlay, onOverlayToggle),
        EffectItem("stripes", stringResource(R.string.effects_stripes), addStripes, onStripesToggle),
        EffectItem("snow", stringResource(R.string.effects_snow_effect), addNoise, onNoiseToggle),
        EffectItem("geo", stringResource(R.string.effect_geometric), addGeometric, onGeometricToggle),
//        EffectItem("glow", "Glow", addGeometric, onGeometricToggle),
//        EffectItem("dust", "Dust", addGeometric, onGeometricToggle)
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(rowGap),
        modifier = Modifier.fillMaxWidth()
    ) {

        /* ── Row 1: Colors ───────────────────────────────────────────── */
        // Space-aware: measures actual available width to decide pill vs icon-only.
        // Works correctly on phones, tablets, and foldables.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val availableWidth = maxWidth
            val colorBlockWidth = (colorH * selectedColors.size) + (spacing * selectedColors.size.coerceAtLeast(1))
            val multiColorWidth = (90 * scale).dp
            val pillWidth       = (95 * scale).dp
            val hasRoomForPill  = selectedColors.size < 5 &&
                    (colorBlockWidth + pillWidth + multiColorWidth + spacing * 3) <= availableWidth

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing),
                modifier = Modifier.fillMaxWidth()
            ) {
                selectedColors.forEachIndexed { index, color ->
                    ColorSquare(
                        color   = color,
                        size    = colorH,
                        onClick = { onRemoveColor(index) },
                        isDark  = isDark,
                        xSize   = (14 * scale).sp
                    )
                }

                if (selectedColors.size < 5) {
                    if (hasRoomForPill) {
                        AddColorPill(
                            onClick  = onAddColor,
                            height   = colorH,
                            isDark   = isDark,
                            fontSize = (12 * scale).sp
                        )
                    } else {
                        AddColorIcon(
                            onClick  = onAddColor,
                            size     = colorH,
                            isDark   = isDark,
                            fontSize = (16 * scale).sp
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                MultiColorPill(
                    isMultiColor = isMultiColor,
                    onToggle = {
                        if (!isMultiColor) Haptics.confirm(view) else Haptics.light(view)
                        onMultiColorChange(!isMultiColor)
                    },
                    height   = colorH,
                    isDark   = isDark,
                    fontSize = (12 * scale).sp
                )
            }
        }

        /* ── Row 2: Gradient ─────────────────────────────────────────── */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            GradientType.entries.forEach { type ->
                // BoxWithConstraints so font shrinks if label is long
                Box(modifier = Modifier.weight(1f)) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val fs = (maxWidth.value * 0.20f).coerceIn(10f, 14f).sp
                        TextChip(
                            modifier = Modifier.fillMaxWidth(),
                            selected = type in selectedGradientTypes,
                            onClick  = { Haptics.light(view); onGradientToggle(type) },
                            label    = when (type) {
                                GradientType.Linear  -> stringResource(R.string.gradient_style_linear)
                                GradientType.Radial  -> stringResource(R.string.gradient_style_radial)
                                GradientType.Angular -> stringResource(R.string.gradient_style_angular)
                                GradientType.Diamond -> stringResource(R.string.gradient_style_diamond)
                            },
                            height   = gradientChipH,
                            isDark   = isDark,
                            fontSize = fs
                        )
                    }
                }
            }
        }

        /* ── Row 3: Effects ──────────────────────────────────────────── */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            effects.forEach { effect ->
                Box(modifier = Modifier.weight(1f)) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        // Font scales with chip width: more chips = narrower = smaller font
                        val iconFs  = (maxWidth.value * 0.28f).coerceIn(11f, 22f).sp
                        val labelFs = (maxWidth.value * 0.16f).coerceIn(7f,  13f).sp
                        EffectChip(
                            modifier = Modifier.fillMaxWidth(),
                            iconKey  = effect.iconKey,
                            label    = effect.label,
                            selected = effect.selected,
                            onClick  = { Haptics.light(view); effect.onClick() },
                            isDark   = isDark,
                            iconFs   = iconFs,
                            labelFs  = labelFs,
                            height   = effectChipH
                        )
                    }
                }
            }
        }

        /* ── Row 4: Tone slider ──────────────────────────────────────── */
        ToneSliderRow(
            toneMode     = toneMode,
            onToneChange = onToneChange,
            isDark       = isDark,
            fontSize     = (12 * scale).sp
        )
    }
}

/* ── Effect chip — full-chip mini wallpaper thumbnail ────────────── */
// The entire chip background IS the effect preview — gradient + effect pattern on top.
// Label sits at the bottom in a frosted pill so it's always readable.
// This matches exactly what the user sees in the wallpaper grid below.
@Composable
private fun EffectChip(
    modifier: Modifier,
    iconKey: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    iconFs: TextUnit,  // unused now but kept for API compat
    labelFs: TextUnit,
    height: Dp
) {
    var pressed by remember { mutableStateOf(false) }
    val anim by animateFloatAsState(if (pressed) 0.94f else 1f, spring(0.55f, 550f), label = "ef")

    // Muted preview gradient — blue→teal, matches dark theme nicely, stays subtle
    val previewColors = if (isDark) {
        listOf(Color(0xFF1a237e), Color(0xFF37474f))
    } else {
        listOf(Color(0xFF6A7BFF), Color(0xFF8EC5FF))
    }
    val overlayAlpha  = if (selected) 1f else 0.75f
    val patternColor = Color.White.copy(alpha = if (isDark) 0.16f else 0.22f)
    val selectedRing  = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.scale(anim)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(ChipCorner))
                .clickable(onClick = onClick)
        ) {
            // Layer 1: gradient base
            Canvas(modifier = Modifier.matchParentSize()) {
                val brush = Brush.linearGradient(
                    colors = previewColors,
                    start  = Offset(0f, 0f),
                    end    = Offset(size.width, size.height)
                )
                drawRect(brush = brush)
            }

            // subtle vignette to give depth
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = if (isDark) 0.22f else 0.08f)
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.maxDimension
                    )
                )
            }

            // Layer 2: effect pattern — mirrors BitmapUtils.kt rendering exactly
            Canvas(modifier = Modifier.matchParentSize().graphicsLayer(alpha = overlayAlpha)) {
                val w = size.width; val h = size.height
                when (iconKey) {

                    "glass" -> {
                        val bandCount = 8
                        val bandH = h / bandCount

                        for (i in 0 until bandCount) {
                            val y = bandH * i

                            val alpha = if (i % 2 == 0) 0.12f else 0.05f

                            drawRect(
                                color = Color.White.copy(alpha = alpha),
                                topLeft = Offset(0f, y),
                                size = Size(w, bandH)
                            )
                        }

                        val lineSw = (h * 0.006f).coerceAtLeast(0.7f)

                        for (i in 1 until bandCount) {
                            val y = bandH * i

                            drawLine(
                                Color.White.copy(alpha = 0.22f),
                                Offset(0f, y),
                                Offset(w, y),
                                strokeWidth = lineSw
                            )
                        }
                    }

                    "stripes" -> {
                        // BitmapUtils: canvas.rotate(-45°) + vertical soft-fade rects
                        // Replicated as diagonal lines at -45° with soft alpha, spacing = w/10
                        val spacing = w / 10f
                        val diag = kotlin.math.sqrt((w * w + h * h).toDouble()).toFloat()
                        val sw2 = (spacing * 0.45f).coerceAtLeast(1f)
                        var i = -diag
                        while (i < diag * 2f) {
                            // Each stripe: two lines side-by-side for soft-edge effect
                            val x0 = i;  val y0 = 0f
                            val x1 = i + h; val y1 = h   // -45° line
                            drawLine(Color.White.copy(alpha = 0.20f), Offset(x0, y0), Offset(x1, y1),
                                strokeWidth = sw2, cap = StrokeCap.Butt)
                            drawLine(Color.White.copy(alpha = 0.06f), Offset(x0 + sw2 * 0.5f, y0),
                                Offset(x1 + sw2 * 0.5f, y1),
                                strokeWidth = sw2 * 0.7f, cap = StrokeCap.Butt)
                            i += spacing
                        }
                    }

                    "snow" -> {
                        // BitmapUtils: ~2% of pixels as random white circles, radius 0.6–1.8×basePx
                        // Fixed seed positions for consistent thumbnail appearance
                        val dotR = (w * 0.028f).coerceAtLeast(1f)
                        listOf(
                            0.07f to 0.06f, 0.21f to 0.13f, 0.44f to 0.04f, 0.63f to 0.11f, 0.82f to 0.08f, 0.95f to 0.17f,
                            0.03f to 0.27f, 0.16f to 0.33f, 0.31f to 0.24f, 0.52f to 0.31f, 0.70f to 0.26f, 0.88f to 0.35f,
                            0.11f to 0.48f, 0.27f to 0.54f, 0.43f to 0.44f, 0.60f to 0.51f, 0.76f to 0.46f, 0.91f to 0.55f,
                            0.05f to 0.67f, 0.20f to 0.72f, 0.37f to 0.63f, 0.55f to 0.69f, 0.73f to 0.74f, 0.87f to 0.65f,
                            0.13f to 0.85f, 0.29f to 0.90f, 0.48f to 0.82f, 0.66f to 0.88f, 0.83f to 0.83f, 0.97f to 0.91f
                        ).forEach { (fx, fy) ->
                            val r = dotR * (0.7f + ((fx * 7 + fy * 13) % 10) * 0.13f)
                            val a = 0.08f + ((fx * 11 + fy * 7) % 10) * 0.012f
                            drawCircle(Color.White.copy(alpha = a), r, Offset(w * fx, h * fy))
                        }
                    }

                    "geo" -> {
                        // overlay_geometric PNG = grid lines + circles (matches reference image 2 exactly)
                        val sw = (w * 0.025f).coerceAtLeast(0.8f)
                        val lineColor = Color.White.copy(alpha = 0.18f)
                        // 3-column grid
                        for (i in 1..2) {
                            drawLine(lineColor, Offset(w * i / 3f, 0f), Offset(w * i / 3f, h), strokeWidth = sw)
                        }
                        // 4-row grid
                        for (i in 1..3) {
                            drawLine(lineColor, Offset(0f, h * i / 4f), Offset(w, h * i / 4f), strokeWidth = sw)
                        }
                        // Large circle centered upper portion
                        drawCircle(lineColor, w * 0.40f, Offset(w * 0.5f, h * 0.32f), style = Stroke(sw))
                        // Smaller circle centered lower
                        drawCircle(lineColor, w * 0.30f, Offset(w * 0.5f, h * 0.62f), style = Stroke(sw))
                    }

                    "glow" -> {
                        // Radial burst — temp effect, visualise as bright center radial glow
                        val cx = w / 2f; val cy = h * 0.45f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
                                center = Offset(cx, cy),
                                radius = w * 0.48f
                            ),
                            radius = w * 0.48f,
                            center = Offset(cx, cy)
                        )
                        // Inner bright core
                        drawCircle(Color.White.copy(alpha = 0.30f), w * 0.12f, Offset(cx, cy))
                    }

                    "dust" -> {
                        // Fine grain — same as snow but higher density, smaller radius
                        val dotR = (w * 0.018f).coerceAtLeast(0.8f)
                        listOf(
                            0.04f to 0.04f, 0.14f to 0.09f, 0.26f to 0.03f, 0.38f to 0.12f, 0.50f to 0.06f,
                            0.62f to 0.11f, 0.74f to 0.05f, 0.86f to 0.13f, 0.95f to 0.07f,
                            0.08f to 0.22f, 0.19f to 0.28f, 0.31f to 0.19f, 0.44f to 0.25f, 0.56f to 0.20f,
                            0.68f to 0.27f, 0.79f to 0.21f, 0.91f to 0.29f,
                            0.03f to 0.40f, 0.13f to 0.45f, 0.24f to 0.37f, 0.36f to 0.43f, 0.48f to 0.38f,
                            0.59f to 0.44f, 0.71f to 0.39f, 0.83f to 0.46f, 0.94f to 0.41f,
                            0.07f to 0.58f, 0.18f to 0.63f, 0.30f to 0.55f, 0.42f to 0.61f, 0.54f to 0.57f,
                            0.65f to 0.64f, 0.77f to 0.58f, 0.89f to 0.65f,
                            0.02f to 0.76f, 0.12f to 0.81f, 0.23f to 0.73f, 0.35f to 0.79f, 0.47f to 0.75f,
                            0.58f to 0.82f, 0.70f to 0.76f, 0.81f to 0.83f, 0.93f to 0.77f,
                            0.06f to 0.92f, 0.17f to 0.88f, 0.28f to 0.95f, 0.40f to 0.90f, 0.52f to 0.93f,
                            0.63f to 0.87f, 0.75f to 0.94f, 0.87f to 0.89f, 0.97f to 0.96f
                        ).forEach { (fx, fy) ->
                            val a = 0.06f + ((fx * 13 + fy * 9) % 10) * 0.010f
                            drawCircle(Color.White.copy(alpha = a), dotR, Offset(w * fx, h * fy))
                        }
                    }
                }
            }

            // Layer 3: selected ring overlay
            if (selected) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                selectedRing.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            radius = size.maxDimension
                        ),
                        cornerRadius = CornerRadius(ChipCorner.toPx())
                    )
                    val sw2 = 2.2f.dp.toPx(); val i2 = sw2 / 2f
                    drawRoundRect(
                        color        = selectedRing,
                        topLeft      = Offset(i2, i2),
                        size = androidx.compose.ui.geometry.Size(this.size.width - sw2, this.size.height - sw2),
                        cornerRadius = CornerRadius(ChipCorner.toPx()),
                        style        = Stroke(sw2)
                    )
                }
            } else {
                // Unselected border
                Canvas(modifier = Modifier.matchParentSize()) {
                    val sw2 = 1.2f.dp.toPx(); val i2 = sw2 / 2f
                    drawRoundRect(
                        color        = Color.White.copy(alpha = 0.12f),
                        topLeft      = Offset(i2, i2),
                        size = androidx.compose.ui.geometry.Size(this.size.width - sw2, this.size.height - sw2),
                        cornerRadius = CornerRadius(ChipCorner.toPx()),
                        style        = Stroke(sw2)
                    )
                }
            }

            // Layer 4: label at bottom in frosted pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isDark)
                            Color.Black.copy(alpha = if (selected) 0.55f else 0.35f)
                        else
                            Color.White.copy(alpha = if (selected) 0.85f else 0.75f)
                    )
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text          = label,
                    fontSize      = labelFs,
                    fontWeight    = if (selected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.1.sp,
                    textAlign     = TextAlign.Center,
                    maxLines      = 1,
                    color         = if (isDark) Color.White else Color.Black
                )
            }
        }
    }
}

/* ── Text chip (gradient row) ────────────────────────────────────── */
@Composable
private fun TextChip(
    modifier: Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    height: Dp,
    isDark: Boolean,
    fontSize: TextUnit
) {
    var pressed by remember { mutableStateOf(false) }
    val anim by animateFloatAsState(if (pressed) 0.94f else 1f, spring(0.6f, 500f), label = "tc")
    Box(modifier = modifier.scale(anim)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(ChipCorner))
                .background(chipBg(selected, isDark))
                .premiumChipBorder(selected, isDark)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label, maxLines = 1, textAlign = TextAlign.Center,
                fontSize = fontSize,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                letterSpacing = 0.2.sp,
                color = chipFg(selected, isDark, true)
            )
        }
    }
}

/* ── Color square ────────────────────────────────────────────────── */
@Composable
private fun ColorSquare(color: Color, size: Dp, onClick: () -> Unit, isDark: Boolean, xSize: TextUnit) {
    var pressed by remember { mutableStateOf(false) }
    val anim by animateFloatAsState(if (pressed) 0.87f else 1f, spring(0.55f, 600f), label = "cs")
    Box(
        modifier = Modifier
            .scale(anim)
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .drawBehind {
                val sw = 1.5f.dp.toPx(); val i = sw / 2f
                drawRoundRect(
                    color = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.4f),
                    topLeft = androidx.compose.ui.geometry.Offset(i, i),
                    size = Size(this.size.width - sw, this.size.height - sw),                    cornerRadius = CornerRadius(12.dp.toPx()), style = Stroke(sw)
                )
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "×", fontSize = xSize, fontWeight = FontWeight.Bold,
            color = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.55f)
            else Color.White.copy(alpha = 0.8f)
        )
    }
}

/* ── Add Color pill ──────────────────────────────────────────────── */
@Composable
private fun AddColorPill(onClick: () -> Unit, height: Dp, isDark: Boolean, fontSize: TextUnit) {
    var pressed by remember { mutableStateOf(false) }
    val anim by animateFloatAsState(if (pressed) 0.94f else 1f, spring(0.6f, 500f), label = "ac")
    Box(
        modifier = Modifier
            .scale(anim)
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
            .premiumAddColorBorder(isDark)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+ Add Color", fontSize = fontSize,
            fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/* ── Add Color icon-only square (3+ colors) ─────────────────────── */
@Composable
private fun AddColorIcon(onClick: () -> Unit, size: Dp, isDark: Boolean, fontSize: TextUnit) {
    var pressed by remember { mutableStateOf(false) }
    val anim by animateFloatAsState(if (pressed) 0.94f else 1f, spring(0.6f, 500f), label = "aci")
    Box(
        modifier = Modifier
            .scale(anim).size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
            .premiumAddColorBorder(isDark)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "+", fontSize = fontSize, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary)
    }
}

/* ── Multi-color pill ────────────────────────────────────────────── */
@Composable
private fun MultiColorPill(
    isMultiColor: Boolean, onToggle: () -> Unit,
    height: Dp, isDark: Boolean, fontSize: TextUnit
) {
    var pressed by remember { mutableStateOf(false) }
    val anim by animateFloatAsState(if (pressed) 0.94f else 1f, spring(0.6f, 500f), label = "mc")
    Box(
        modifier = Modifier
            .scale(anim)
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isMultiColor) Brush.verticalGradient(listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                )) else if (isDark) Brush.verticalGradient(listOf(
                    Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.05f)
                )) else Brush.verticalGradient(listOf(
                    Color.Black.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.03f)
                ))
            )
            .premiumMultiColorBorder(isMultiColor, isDark)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.multicolor_label),
            fontSize = fontSize,
            fontWeight = if (isMultiColor) FontWeight.SemiBold else FontWeight.Medium,
            letterSpacing = 0.1.sp,
            color = if (isMultiColor) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
    }
}

/* ── Tone slider ─────────────────────────────────────────────────── */
@Composable
private fun ToneSliderRow(toneMode: ToneMode, onToneChange: (ToneMode) -> Unit, isDark: Boolean, fontSize: TextUnit) {
    var pos by remember(toneMode) {
        mutableIntStateOf(when (toneMode) { ToneMode.DARK -> 0; ToneMode.NEUTRAL -> 1; ToneMode.LIGHT -> 2 })
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.wallpaper_theme_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth().height(44.dp)
                .clip(RoundedCornerShape(ChipCorner))
                .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
                .premiumSliderBorder(isDark)
                .padding(3.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                listOf(
                    stringResource(R.string.wallpaper_theme_dark_tones),
                    stringResource(R.string.wallpaper_theme_neutral_tones),
                    stringResource(R.string.wallpaper_theme_light_tones)
                ).forEachIndexed { i, label ->
                    val sel = pos == i
                    Box(
                        modifier = Modifier
                            .weight(1f).fillMaxHeight()
                            .clip(RoundedCornerShape(11.dp))
                            .clickable {
                                pos = i
                                onToneChange(when (i) { 0 -> ToneMode.DARK; 1 -> ToneMode.NEUTRAL; else -> ToneMode.LIGHT })
                            }
                            .background(
                                if (sel) Brush.verticalGradient(listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                )) else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label, fontSize = fontSize,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 0.3.sp,
                            color = if (sel) { if (isDark) Color.Black else Color.White }
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/* ── Shared helpers ──────────────────────────────────────────────── */
@Composable
private fun chipBg(selected: Boolean, isDark: Boolean): Brush = when {
    selected -> Brush.verticalGradient(listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
    ))
    isDark   -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.04f)))
    else     -> Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.02f)))
}

@Composable
private fun chipFg(selected: Boolean, isDark: Boolean, strong: Boolean): Color =
    if (selected) {
        if (isDark) Color.Black.copy(alpha = if (strong) 1f else 0.8f)
        else Color.White.copy(alpha = if (strong) 1f else 0.9f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = if (strong) 0.8f else 0.55f)
    }

/* ── Border modifiers — identical to original ────────────────────── */
fun Modifier.premiumChipBorder(selected: Boolean, isDark: Boolean) = composed {
    val c = if (selected) { if (isDark) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.3f) }
    else if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)
    drawBehind {
        val sw = 1.2f.dp.toPx(); val i = sw / 2f
        drawRoundRect(color = c, topLeft = androidx.compose.ui.geometry.Offset(i, i),
            size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
            cornerRadius = CornerRadius(14.dp.toPx()), style = Stroke(sw))
    }
}

fun Modifier.premiumSliderBorder(isDark: Boolean) = composed {
    val c = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)
    drawBehind {
        val sw = 1.2f.dp.toPx(); val i = sw / 2f
        drawRoundRect(color = c, topLeft = androidx.compose.ui.geometry.Offset(i, i),
            size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
            cornerRadius = CornerRadius(14.dp.toPx()), style = Stroke(sw))
    }
}

fun Modifier.premiumAddColorBorder(isDark: Boolean) = composed {
    val b = Brush.linearGradient(listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    ))
    drawBehind {
        val sw = 1.2f.dp.toPx(); val i = sw / 2f
        drawRoundRect(brush = b, topLeft = androidx.compose.ui.geometry.Offset(i, i),
            size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
            cornerRadius = CornerRadius(12.dp.toPx()), style = Stroke(sw))
    }
}

fun Modifier.premiumMultiColorBorder(selected: Boolean, isDark: Boolean) = composed {
    val c = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
    else if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)
    drawBehind {
        val sw = 1.2f.dp.toPx(); val i = sw / 2f
        drawRoundRect(color = c, topLeft = androidx.compose.ui.geometry.Offset(i, i),
            size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
            cornerRadius = CornerRadius(12.dp.toPx()), style = Stroke(sw))
    }
}