/**
 * Options panel — everything visible at once, no scrolling, no hidden content.
 *
 * Layout (4 tight rows):
 *
 * Row 1 — Colors:  [●][●][●]  +Add   ·····  [Multi-color]
 * Row 2 — Gradient: [Linear][Radial][Angular][Diamond]  (equal chips, full width)
 * Row 3 — Effects:  [🪟Glass][▤Stripes][❄Snow][◈Geo][✦Glow][◌Dust]  (icon+label, equal)
 * Row 4 — Tone:    [Dark Tones] [Neutral] [Light Tones]
 *
 * Key decisions:
 * - Colors are small circles inline with +Add and Multi-color on the same row
 * - Gradient = text chips, 4 equal columns
 * - Effects = icon+label chips, 6 equal columns — naturally smaller than gradient
 *   because 6 chips share the same width as 4 gradient chips → no extra height needed
 * - Tone slider unchanged
 * - Total panel height ≈ 180dp (4 rows × ~40dp + 3 gaps × 8dp)
 */
package com.example.waller.ui.wallpaper.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waller.R
import com.example.waller.ui.wallpaper.GradientType
import com.example.waller.ui.wallpaper.Haptics
import com.example.waller.ui.wallpaper.ToneMode

private val ChipCorner  = 14.dp
private val RowSpacing  = 8.dp
private val ChipSpacing = 6.dp

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

    data class EffectItem(val icon: String, val label: String, val selected: Boolean, val onClick: () -> Unit)

    val effects = listOf(
        EffectItem("🪟", "Glass",   addOverlay,   onOverlayToggle),
        EffectItem("▤",  "Stripes", addStripes,   onStripesToggle),
        EffectItem("❄",  "Snow",    addNoise,     onNoiseToggle),
        EffectItem("◈",  "Geo",     addGeometric, onGeometricToggle),
        EffectItem("✦",  "Glow",    addGeometric, onGeometricToggle), // temp
        EffectItem("◌",  "Dust",    addGeometric, onGeometricToggle), // temp
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(RowSpacing),
        modifier = Modifier.fillMaxWidth()
    ) {

        /* ══ Row 1: Colors ══════════════════════════════════════════════
         *  [●][●][●]  +Add  ─────────────────────  [Multi-color]
         * ══════════════════════════════════════════════════════════════ */
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Color circles
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                selectedColors.forEachIndexed { index, color ->
                    ColorDot(color = color, onClick = { onRemoveColor(index) }, isDark = isDark)
                }
                if (selectedColors.size < 5) {
                    AddColorDot(onClick = onAddColor, isDark = isDark)
                }
            }

            Spacer(Modifier.weight(1f))

            // Multi-color toggle
            MultiColorChip(
                isMultiColor = isMultiColor,
                onToggle = {
                    if (!isMultiColor) Haptics.confirm(view) else Haptics.light(view)
                    onMultiColorChange(!isMultiColor)
                },
                isDark = isDark
            )
        }

        /* ══ Row 2: Gradient type ═══════════════════════════════════════
         *  [  Linear  ][  Radial  ][ Angular  ][ Diamond  ]
         * ══════════════════════════════════════════════════════════════ */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ChipSpacing)
        ) {
            GradientType.entries.forEach { type ->
                TextChip(
                    modifier = Modifier.weight(1f),
                    selected = type in selectedGradientTypes,
                    onClick  = { Haptics.light(view); onGradientToggle(type) },
                    label    = when (type) {
                        GradientType.Linear  -> stringResource(R.string.gradient_style_linear)
                        GradientType.Radial  -> stringResource(R.string.gradient_style_radial)
                        GradientType.Angular -> stringResource(R.string.gradient_style_angular)
                        GradientType.Diamond -> stringResource(R.string.gradient_style_diamond)
                    },
                    height   = 38.dp,
                    isDark   = isDark
                )
            }
        }

        /* ══ Row 3: Effects ═════════════════════════════════════════════
         *  [🪟][▤][❄][◈][✦][◌]   — 6 equal icon+label chips
         *  Each chip is (screenWidth / 6) wide — naturally compact
         * ══════════════════════════════════════════════════════════════ */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ChipSpacing)
        ) {
            effects.forEach { effect ->
                EffectChip(
                    modifier = Modifier.weight(1f),
                    icon     = effect.icon,
                    label    = effect.label,
                    selected = effect.selected,
                    onClick  = { Haptics.light(view); effect.onClick() },
                    isDark   = isDark
                )
            }
        }

        /* ══ Row 4: Tone slider ═════════════════════════════════════════ */
        ToneSliderRow(toneMode = toneMode, onToneChange = onToneChange, isDark = isDark)
    }
}

/* ─────────────────────────────────────────────────────────────────
   Effect chip: square-ish, icon on top, label below
   Height is driven by aspectRatio so it scales with available width.
   6 chips on a ~360dp screen → each ~54dp wide → ~49dp tall (ratio 1.1)
───────────────────────────────────────────────────────────────────*/
@Composable
private fun EffectChip(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.91f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 550f),
        label         = "effectScale"
    )
    Box(modifier = modifier.scale(scale)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.05f)
                .clip(RoundedCornerShape(ChipCorner))
                .background(chipBg(selected, isDark))
                .premiumChipBorder(selected, isDark)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text     = icon,
                    fontSize = 17.sp,
                    color    = chipContentColor(selected, isDark, strong = true)
                )
                Text(
                    text          = label,
                    fontSize      = 9.sp,
                    fontWeight    = if (selected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.1.sp,
                    textAlign     = TextAlign.Center,
                    maxLines      = 1,
                    color         = chipContentColor(selected, isDark, strong = false)
                )
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────
   Text chip: gradient row
───────────────────────────────────────────────────────────────────*/
@Composable
private fun TextChip(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    height: androidx.compose.ui.unit.Dp,
    isDark: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label         = "textChipScale"
    )
    Box(modifier = modifier.scale(scale)) {
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
                text          = label,
                maxLines      = 1,
                textAlign     = TextAlign.Center,
                fontSize      = 12.sp,
                fontWeight    = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                letterSpacing = 0.2.sp,
                color         = chipContentColor(selected, isDark, strong = true)
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────────
   Color dot: rounded square (12dp radius matching original file),
   shows a small "×" so user knows it's tappable to remove
───────────────────────────────────────────────────────────────────*/
@Composable
private fun ColorDot(color: Color, onClick: () -> Unit, isDark: Boolean) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (isPressed) 0.87f else 1f,
        spring(0.55f, 600f), label = "dotScale"
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .size(32.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .drawBehind {
                val sw = 1.5f.dp.toPx(); val i = sw / 2f
                drawRoundRect(
                    color        = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.4f),
                    topLeft      = androidx.compose.ui.geometry.Offset(i, i),
                    size         = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style        = Stroke(sw)
                )
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = "×",
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold,
            color      = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.55f)
            else Color.White.copy(alpha = 0.75f)
        )
    }
}

/* ─────────────────────────────────────────────────────────────────
   Add Color pill: rounded rect, matches chip style from original
───────────────────────────────────────────────────────────────────*/
@Composable
private fun AddColorDot(onClick: () -> Unit, isDark: Boolean) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (isPressed) 0.94f else 1f,
        spring(0.6f, 500f), label = "addDotScale"
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .height(32.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
            .premiumAddColorBorder(isDark)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text          = "+ Add Color",
            fontSize      = 11.sp,
            fontWeight    = FontWeight.Medium,
            letterSpacing = 0.1.sp,
            color         = MaterialTheme.colorScheme.primary
        )
    }
}

/* ─────────────────────────────────────────────────────────────────
   Multi-color chip: pill on the right of color row
───────────────────────────────────────────────────────────────────*/
@Composable
private fun MultiColorChip(isMultiColor: Boolean, onToggle: () -> Unit, isDark: Boolean) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (isPressed) 0.94f else 1f,
        spring(0.6f, 500f), label = "multiScale"
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .height(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isMultiColor) Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    )
                ) else if (isDark) Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.05f))
                ) else Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.03f))
                )
            )
            .premiumMultiColorBorder(isMultiColor, isDark)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text      = stringResource(id = R.string.multicolor_label),
            fontSize  = 11.sp,
            fontWeight = if (isMultiColor) FontWeight.SemiBold else FontWeight.Medium,
            letterSpacing = 0.1.sp,
            color     = if (isMultiColor) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
    }
}

/* ─────────────────────────────────────────────────────────────────
   Tone slider
───────────────────────────────────────────────────────────────────*/
@Composable
private fun ToneSliderRow(toneMode: ToneMode, onToneChange: (ToneMode) -> Unit, isDark: Boolean) {
    var position by remember(toneMode) {
        mutableIntStateOf(when (toneMode) { ToneMode.DARK -> 0; ToneMode.NEUTRAL -> 1; ToneMode.LIGHT -> 2 })
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text          = stringResource(id = R.string.wallpaper_theme_title),
            style         = MaterialTheme.typography.titleMedium,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 0.2.sp
        )
        Spacer(Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
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
                ).forEachIndexed { index, label ->
                    val selected = position == index
                    Box(
                        modifier = Modifier
                            .weight(1f).fillMaxHeight()
                            .clip(RoundedCornerShape(11.dp))
                            .clickable {
                                position = index
                                onToneChange(when (index) { 0 -> ToneMode.DARK; 1 -> ToneMode.NEUTRAL; else -> ToneMode.LIGHT })
                            }
                            .background(
                                if (selected) Brush.verticalGradient(
                                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                                ) else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text          = label,
                            fontSize      = 12.sp,
                            fontWeight    = if (selected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 0.3.sp,
                            color         = if (selected) { if (isDark) Color.Black else Color.White }
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────
   Shared helpers
───────────────────────────────────────────────────────────────────*/
@Composable
private fun chipBg(selected: Boolean, isDark: Boolean): Brush =
    when {
        selected -> Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
            )
        )
        isDark -> Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.04f))
        )
        else -> Brush.verticalGradient(
            colors = listOf(Color.Black.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.02f))
        )
    }

@Composable
private fun chipContentColor(selected: Boolean, isDark: Boolean, strong: Boolean): Color =
    if (selected) {
        if (isDark) Color.Black.copy(alpha = if (strong) 1f else 0.8f)
        else Color.White.copy(alpha = if (strong) 1f else 0.9f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = if (strong) 0.8f else 0.5f)
    }

/* ─────────────────────────────────────────────────────────────────
   Border modifiers — identical to original
───────────────────────────────────────────────────────────────────*/
fun Modifier.premiumChipBorder(selected: Boolean, isDark: Boolean) = composed {
    val color = if (selected) {
        if (isDark) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.3f)
    } else if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)
    drawBehind {
        val sw = 1.2f.dp.toPx(); val i = sw / 2f
        drawRoundRect(color = color, topLeft = androidx.compose.ui.geometry.Offset(i, i),
            size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
            cornerRadius = CornerRadius(14.dp.toPx()), style = Stroke(sw))
    }
}

fun Modifier.premiumSliderBorder(isDark: Boolean) = composed {
    val color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)
    drawBehind {
        val sw = 1.2f.dp.toPx(); val i = sw / 2f
        drawRoundRect(color = color, topLeft = androidx.compose.ui.geometry.Offset(i, i),
            size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
            cornerRadius = CornerRadius(14.dp.toPx()), style = Stroke(sw))
    }
}

fun Modifier.premiumAddColorBorder(isDark: Boolean) = composed {
    val brush = Brush.linearGradient(listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    ))
    drawBehind {
        val sw = 1.2f.dp.toPx(); val i = sw / 2f
        drawRoundRect(brush = brush, topLeft = androidx.compose.ui.geometry.Offset(i, i),
            size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
            cornerRadius = CornerRadius(12.dp.toPx()), style = Stroke(sw))
    }
}

fun Modifier.premiumMultiColorBorder(selected: Boolean, isDark: Boolean) = composed {
    val color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
    else if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)
    drawBehind {
        val sw = 1.2f.dp.toPx(); val i = sw / 2f
        drawRoundRect(color = color, topLeft = androidx.compose.ui.geometry.Offset(i, i),
            size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
            cornerRadius = CornerRadius(12.dp.toPx()), style = Stroke(sw))
    }
}