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
        EffectItem("glass",   "Glass",   addOverlay,   onOverlayToggle),
        EffectItem("stripes", "Stripes", addStripes,   onStripesToggle),
        EffectItem("snow",    "Snow",    addNoise,     onNoiseToggle),
        EffectItem("geo",     "Geo",     addGeometric, onGeometricToggle),
        EffectItem("glow",    "Glow",    addGeometric, onGeometricToggle), // temp
        EffectItem("dust",    "Dust",    addGeometric, onGeometricToggle), // temp
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

/* ── Effect chip — canvas-drawn icon, no emoji ───────────────────── */
@Composable
private fun EffectChip(
    modifier: Modifier,
    iconKey: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    iconFs: TextUnit,
    labelFs: TextUnit,
    height: Dp
) {
    var pressed by remember { mutableStateOf(false) }
    val anim by animateFloatAsState(if (pressed) 0.91f else 1f, spring(0.55f, 550f), label = "ef")
    val iconColor = chipFg(selected, isDark, true)

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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                // Canvas-drawn icon — crisp at any density, matches reference images
                val iconSizeDp = androidx.compose.ui.unit.Dp(iconFs.value * 1.1f)
                Canvas(modifier = Modifier.size(iconSizeDp)) {
                    val w = size.width; val h = size.height
                    val paint = androidx.compose.ui.graphics.Paint().apply {
                        color = iconColor
                        strokeWidth = (w * 0.08f).coerceAtLeast(1.5f)
                        style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    }
                    when (iconKey) {
                        "glass" -> {
                            // Straight horizontal lines (venetian blind / glass slats)
                            val lineCount = 4
                            val gap = h / (lineCount + 1)
                            for (i in 1..lineCount) {
                                drawLine(iconColor, Offset(w * 0.1f, gap * i), Offset(w * 0.9f, gap * i),
                                    strokeWidth = paint.strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            }
                        }
                        "stripes" -> {
                            // Diagonal lines (45°) matching reference image 1
                            val lineCount = 5
                            val step = w / lineCount
                            for (i in -1..lineCount + 1) {
                                val x = step * i
                                drawLine(iconColor, Offset(x, h), Offset(x + h, 0f),
                                    strokeWidth = paint.strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            }
                        }
                        "snow" -> {
                            // Scattered dots (snow/grain) matching reference image 4
                            val dotR = (w * 0.07f)
                            val positions = listOf(
                                Offset(w*0.2f, h*0.25f), Offset(w*0.55f, h*0.15f), Offset(w*0.8f, h*0.35f),
                                Offset(w*0.15f, h*0.6f), Offset(w*0.45f, h*0.55f), Offset(w*0.75f, h*0.65f),
                                Offset(w*0.3f,  h*0.82f), Offset(w*0.65f, h*0.85f)
                            )
                            positions.forEach { pos ->
                                drawCircle(iconColor, dotR, pos)
                            }
                        }
                        "geo" -> {
                            // Circle + grid lines (geometric) matching reference image 2
                            val cx = w / 2f; val cy = h / 2f; val r = w * 0.38f
                            drawLine(iconColor, Offset(0f, cy), Offset(w, cy), strokeWidth = paint.strokeWidth * 0.7f)
                            drawLine(iconColor, Offset(cx, 0f), Offset(cx, h), strokeWidth = paint.strokeWidth * 0.7f)
                            drawCircle(iconColor, r, Offset(cx, cy), style = Stroke(paint.strokeWidth * 0.7f))
                        }
                        "glow" -> {
                            // Starburst / 4-point star
                            val cx = w / 2f; val cy = h / 2f
                            val outer = w * 0.45f; val inner = w * 0.18f
                            val path = androidx.compose.ui.graphics.Path()
                            for (i in 0 until 8) {
                                val angle = Math.toRadians(i * 45.0 - 90)
                                val r2 = if (i % 2 == 0) outer else inner
                                val px = cx + (r2 * Math.cos(angle)).toFloat()
                                val py = cy + (r2 * Math.sin(angle)).toFloat()
                                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                            }
                            path.close()
                            drawPath(path, iconColor, style = Stroke(paint.strokeWidth * 0.7f))
                        }
                        "dust" -> {
                            // Small scattered dots (finer than snow)
                            val dotR = (w * 0.05f)
                            val positions = listOf(
                                Offset(w*0.15f, h*0.2f), Offset(w*0.4f, h*0.1f), Offset(w*0.7f, h*0.25f), Offset(w*0.88f, h*0.15f),
                                Offset(w*0.25f, h*0.5f), Offset(w*0.6f,  h*0.45f), Offset(w*0.82f, h*0.55f),
                                Offset(w*0.1f,  h*0.75f), Offset(w*0.35f, h*0.8f), Offset(w*0.65f, h*0.75f), Offset(w*0.9f, h*0.82f)
                            )
                            positions.forEach { pos -> drawCircle(iconColor, dotR, pos) }
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(text = label, fontSize = labelFs,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.1.sp, textAlign = TextAlign.Center, maxLines = 1,
                    color = chipFg(selected, isDark, false))
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
                    size = Size(this.size.width - sw, this.size.height - sw),
                    cornerRadius = CornerRadius(12.dp.toPx()), style = Stroke(sw)
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