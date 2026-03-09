/**
 * Options panel — responsive, no scroll, no hidden content.
 *
 * Row 1 — Colors:   [●][●]  [+ Add Color]  ────  [Multi-color]
 * Row 2 — Gradient: [Linear][Radial][Angular][ Diamond]
 * Row 3 — Effects:  [Glass][Stripes][Snow][Geo][Glow][ Dust]
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    addBlur: Boolean,
    onBlurToggle: () -> Unit,
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
        EffectItem("blur", "Blur", addBlur, onBlurToggle),
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
    val anim by animateFloatAsState(if (pressed) 0.94f else 1f, spring(0.65f, 480f), label = "ef")

    // Muted preview gradient — blue→teal, matches dark theme nicely, stays subtle
    val base = MaterialTheme.colorScheme.primary

    val previewColors = if (isDark) {
        listOf(
            base.copy(alpha = 0.85f),
            base.copy(alpha = 0.45f)
        )
    } else {
        listOf(
            base.copy(alpha = 0.65f),
            base.copy(alpha = 0.35f)
        )
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
                .background(
                    if (selected) SolidColor(Color.Transparent)
                    else chipBg(false, isDark)
                )
                .premiumChipBorder(selected, isDark)
                .clickable(onClick = onClick)
        ) {
            // Layer 1: gradient base
            // preview only when selected
            if (selected) {

                Canvas(modifier = Modifier.matchParentSize()) {
                    val brush = Brush.linearGradient(
                        colors = previewColors,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                    drawRect(brush = brush)
                }

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

                Canvas(modifier = Modifier.matchParentSize()) {
                    // pattern rendering
                }

                Canvas(modifier = Modifier.matchParentSize()) {
                    // selected ring
                }
            }

// ALWAYS draw label
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = labelFs,
                    fontWeight = FontWeight.SemiBold,
                    color = chipFg(selected, isDark, true)
                )
            }

            // Layer 2: effect pattern — mirrors BitmapUtils.kt rendering exactly
            if (selected) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width; val h = size.height
                    when (iconKey) {

                        "glass" -> {

                            val w = size.width
                            val h = size.height

                            val lineCount = 10
                            val spacing = w / lineCount

                            for (i in 0..lineCount) {

                                val x = i * spacing

                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.22f),
                                            Color.White.copy(alpha = 0.08f),
                                            Color.Transparent
                                        ),
                                        startX = x - spacing * 0.5f,
                                        endX = x + spacing * 0.5f
                                    ),
                                    topLeft = Offset(x - spacing * 0.25f, 0f),
                                    size = Size(spacing * 0.5f, h)
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

                        "blur" -> {

                            val w = size.width
                            val h = size.height
                            val cx = w / 2f
                            val cy = h / 2f

                            // outer blur halo
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.22f),
                                        Color.White.copy(alpha = 0.10f),
                                        Color.Transparent
                                    ),
                                    center = Offset(cx, cy),
                                    radius = w * 0.45f
                                ),
                                radius = w * 0.45f,
                                center = Offset(cx, cy)
                            )

                            // mid blur layer
                            drawCircle(
                                color = Color.White.copy(alpha = 0.18f),
                                radius = w * 0.25f,
                                center = Offset(cx, cy)
                            )

                            // small sharp center
                            drawCircle(
                                color = Color.White.copy(alpha = 0.28f),
                                radius = w * 0.08f,
                                center = Offset(cx, cy)
                            )
                        }

                    }
                }

                // Layer 3: selected ring overlay

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
                    val sw2 = 2.2f.dp.toPx();
                    val i2 = sw2 / 2f
                    drawRoundRect(
                        color = selectedRing,
                        topLeft = Offset(i2, i2),
                        size = androidx.compose.ui.geometry.Size(
                            this.size.width - sw2,
                            this.size.height - sw2
                        ),
                        cornerRadius = CornerRadius(ChipCorner.toPx()),
                        style = Stroke(sw2)
                    )
                }

                // Layer 4: label at bottom in frosted pill
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = labelFs,
                        fontWeight = FontWeight.Bold,
                        color = chipFg(selected, isDark, true)
                    )
                }
            }
        }
    }}

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
            text = stringResource(R.string.color_selector_add_color), fontSize = fontSize,
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