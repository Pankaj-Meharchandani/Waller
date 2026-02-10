/**
 * Compact 4-row options panel for the wallpaper generator.
 *
 * Rows:
 * 1) Colors (+ Add Color) on the left, Orientation chip on the right
 * 2) Gradient type chips (Linear / Radial / Angular / Diamond)
 * 3) Effect chips (Nothing / Snow / Stripes)
 * 4) Tone slider: Dark • Neutral • Light
 */

package com.example.waller.ui.wallpaper.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waller.R
import com.example.waller.ui.wallpaper.GradientType
import com.example.waller.ui.wallpaper.ToneMode
import androidx.compose.ui.platform.LocalView
import com.example.waller.ui.wallpaper.Haptics

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
    onGeometricToggle: () -> Unit
) {
    val view = LocalView.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {

        /* ---------------- Row 1: Colors (left) + Multi-color toggle (right) ---------------- */

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Colors + Add Color (takes available space)
            if (selectedColors.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    CompactAddColorChip(
                        onClick = onAddColor,
                        isDark = isDark
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    selectedColors.forEachIndexed { index, color ->
                        ColorSquareChip(
                            color = color,
                            onClick = { onRemoveColor(index) },
                            isDark = isDark
                        )
                    }

                    if (selectedColors.size < 5) {
                        CompactAddColorChip(
                            onClick = onAddColor,
                            isDark = isDark
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            MultiColorToggleChip(
                isMultiColor = isMultiColor,
                onToggle = {
                    if (!isMultiColor) {
                        Haptics.confirm(view)
                    } else {
                        Haptics.light(view)
                    }
                    onMultiColorChange(!isMultiColor)
                },
                isDark = isDark
            )
        }

        /* ---------------- Row 2: Gradient type chips ---------------- */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GradientType.entries.forEach { type ->
                PremiumFilterChip(
                    modifier = Modifier.weight(1f),
                    selected = type in selectedGradientTypes,
                    onClick = {
                        Haptics.light(view)
                        onGradientToggle(type)
                    },
                    label = stringResource(
                        id = when (type) {
                            GradientType.Linear -> R.string.gradient_style_linear
                            GradientType.Radial -> R.string.gradient_style_radial
                            GradientType.Angular -> R.string.gradient_style_angular
                            GradientType.Diamond -> R.string.gradient_style_diamond
                        }
                    ),
                    isDark = isDark
                )
            }
        }

        /* ---------------- Row 3: Effects chips ---------------- */

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PremiumFilterChip(
                modifier = Modifier.weight(1f),
                selected = addOverlay,
                onClick = onOverlayToggle,
                label = stringResource(R.string.effects_nothing_style),
                isDark = isDark
            )
            PremiumFilterChip(
                modifier = Modifier.weight(1f),
                selected = addNoise,
                onClick = onNoiseToggle,
                label = stringResource(R.string.effects_snow_effect),
                isDark = isDark
            )
            PremiumFilterChip(
                modifier = Modifier.weight(1f),
                selected = addStripes,
                onClick = onStripesToggle,
                label = stringResource(R.string.effects_stripes),
                isDark = isDark
            )
            PremiumFilterChip(
                modifier = Modifier.weight(1f),
                selected = addGeometric,
                onClick = onGeometricToggle,
                label = stringResource(R.string.effect_geometric),
                isDark = isDark
            )
        }

        /* ---------------- Row 4: Tone slider (Dark • Neutral • Light) ---------------- */

        ToneSliderRow(
            toneMode = toneMode,
            onToneChange = onToneChange,
            isDark = isDark
        )
    }
}

@Composable
private fun PremiumFilterChip(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    isDark: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 500f
        ),
        label = "chipScale"
    )

    Box(
        modifier = modifier.scale(scale)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (selected) {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            )
                        )
                    } else if (isDark) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.06f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.04f),
                                Color.Black.copy(alpha = 0.02f)
                            )
                        )
                    }
                )
                .premiumChipBorder(
                    selected = selected,
                    isDark = isDark
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                maxLines = 1,
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                letterSpacing = 0.2.sp,
                color = if (selected) {
                    if (isDark) Color.Black else Color.White
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                }
            )
        }
    }
}

/* ----------------------------- Tone slider row ----------------------------- */

@Composable
private fun ToneSliderRow(
    toneMode: ToneMode,
    onToneChange: (ToneMode) -> Unit,
    isDark: Boolean
) {
    var position by remember(toneMode) {
        mutableIntStateOf(
            when (toneMode) {
                ToneMode.DARK -> 0
                ToneMode.NEUTRAL -> 1
                ToneMode.LIGHT -> 2
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.wallpaper_theme_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp
        )
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isDark) {
                        Color.White.copy(alpha = 0.06f)
                    } else {
                        Color.Black.copy(alpha = 0.04f)
                    }
                )
                .premiumSliderBorder(isDark = isDark)
                .padding(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                val labels = listOf(
                    stringResource(id = R.string.wallpaper_theme_dark_tones),
                    stringResource(id = R.string.wallpaper_theme_neutral_tones),
                    stringResource(id = R.string.wallpaper_theme_light_tones)
                )

                labels.forEachIndexed { index, label ->
                    val selected = position == index

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(11.dp))
                            .clickable {
                                position = index
                                val newMode = when (index) {
                                    0 -> ToneMode.DARK
                                    1 -> ToneMode.NEUTRAL
                                    else -> ToneMode.LIGHT
                                }
                                onToneChange(newMode)
                            }
                            .background(
                                if (selected) {
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                        )
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Transparent)
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 0.3.sp,
                            color = if (selected) {
                                if (isDark) Color.Black else Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            }
                        )
                    }
                }
            }
        }
    }
}

/* ------------------------ Color square chip with premium design ------------------------ */

@Composable
private fun ColorSquareChip(
    color: Color,
    onClick: () -> Unit,
    isDark: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 500f
        ),
        label = "colorScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .premiumColorChipBorder()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "×",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (color.luminance() > 0.5f) {
                Color.Black.copy(alpha = 0.6f)
            } else {
                Color.White.copy(alpha = 0.85f)
            }
        )
    }
}

/* ------------------------ Add Color chip ------------------------ */

@Composable
private fun CompactAddColorChip(
    onClick: () -> Unit,
    isDark: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 500f
        ),
        label = "addColorScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isDark) {
                    Color.White.copy(alpha = 0.06f)
                } else {
                    Color.Black.copy(alpha = 0.04f)
                }
            )
            .premiumAddColorBorder(isDark = isDark)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+ Add Color",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/* ------------------------ Multi-color toggle chip ------------------------ */

@Composable
private fun MultiColorToggleChip(
    isMultiColor: Boolean,
    onToggle: () -> Unit,
    isDark: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 500f
        ),
        label = "multiColorScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isMultiColor) {
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        )
                    )
                } else if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.04f),
                            Color.Black.copy(alpha = 0.02f)
                        )
                    )
                }
            )
            .premiumMultiColorBorder(
                selected = isMultiColor,
                isDark = isDark
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.multicolor_label),
            fontSize = 13.sp,
            fontWeight = if (isMultiColor) FontWeight.SemiBold else FontWeight.Medium,
            letterSpacing = 0.2.sp,
            color = if (isMultiColor) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            }
        )
    }
}

// Premium chip border
fun Modifier.premiumChipBorder(
    selected: Boolean,
    isDark: Boolean
) = composed {
    val borderColor = if (selected) {
        if (isDark) Color.Black.copy(alpha = 0.15f)
        else Color.White.copy(alpha = 0.3f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.1f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    this.drawBehind {
        val strokeWidth = 1.2f.dp.toPx()
        val inset = strokeWidth / 2f

        val rect = Rect(
            left = inset,
            top = inset,
            right = size.width - inset,
            bottom = size.height - inset
        )

        drawRoundRect(
            color = borderColor,
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = CornerRadius(14.dp.toPx()),
            style = Stroke(strokeWidth)
        )
    }
}

// Premium slider border
fun Modifier.premiumSliderBorder(isDark: Boolean) = composed {
    val color = if (isDark) {
        Color.White.copy(alpha = 0.1f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    this.drawBehind {
        val strokeWidth = 1.2f.dp.toPx()
        val inset = strokeWidth / 2f

        val rect = Rect(
            left = inset,
            top = inset,
            right = size.width - inset,
            bottom = size.height - inset
        )

        drawRoundRect(
            color = color,
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = CornerRadius(14.dp.toPx()),
            style = Stroke(strokeWidth)
        )
    }
}

// Premium color chip border
fun Modifier.premiumColorChipBorder() = this.drawBehind {
    val strokeWidth = 1.5f.dp.toPx()
    val inset = strokeWidth / 2f

    val rect = Rect(
        left = inset,
        top = inset,
        right = size.width - inset,
        bottom = size.height - inset
    )

    drawRoundRect(
        color = Color.White.copy(alpha = 0.25f),
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = CornerRadius(12.dp.toPx()),
        style = Stroke(strokeWidth)
    )
}

// Add color border
fun Modifier.premiumAddColorBorder(isDark: Boolean) = composed {
    val brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
    )

    this.drawBehind {
        val strokeWidth = 1.2f.dp.toPx()
        val inset = strokeWidth / 2f

        val rect = Rect(
            left = inset,
            top = inset,
            right = size.width - inset,
            bottom = size.height - inset
        )

        drawRoundRect(
            brush = brush,
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = CornerRadius(12.dp.toPx()),
            style = Stroke(strokeWidth)
        )
    }
}

// Multi-color border
fun Modifier.premiumMultiColorBorder(
    selected: Boolean,
    isDark: Boolean
) = composed {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.1f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    this.drawBehind {
        val strokeWidth = 1.2f.dp.toPx()
        val inset = strokeWidth / 2f

        val rect = Rect(
            left = inset,
            top = inset,
            right = size.width - inset,
            bottom = size.height - inset
        )

        drawRoundRect(
            color = borderColor,
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = CornerRadius(12.dp.toPx()),
            style = Stroke(strokeWidth)
        )
    }
}
