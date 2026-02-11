/**
 * PreviewControlsComponents.kt
 *
 * Reusable UI components used inside the wallpaper preview.
 *
 * Responsibilities:
 * - Effect chips and opacity sliders
 * - Gradient selection items
 * - Shared preview UI elements such as the device frame
 *
 * These components are UI-only and contain no screen-level state.
 */

package com.example.waller.ui.wallpaper.components.previewOverlay

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waller.R
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CircleShape

/* ───────────────── Gradient type selectors ───────────────── */

@Composable
fun GradientTypeItemFull(
    label: String,
    selected: Boolean,
    textColor: Color,
    onClick: () -> Unit
) {
    val isDark = true // always on dark scrim overlay
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 500f
        ),
        label = "gradientItemScale"
    )

    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }

    Box(modifier = Modifier.scale(scale)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (selected) {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                            )
                        )
                    } else if (isDark) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.04f),
                                Color.White.copy(alpha = 0.02f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.02f),
                                Color.Transparent
                            )
                        )
                    }
                )
                .drawBehind {
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
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            if (selected)
                                MaterialTheme.colorScheme.primary
                            else if (isDark)
                                Color.White.copy(alpha = 0.2f)
                            else
                                Color.Black.copy(alpha = 0.15f)
                        )
                        .drawBehind {
                            val strokeWidth = 0.8.dp.toPx()
                            val inset = strokeWidth / 2f
                            val rect = Rect(
                                left = inset,
                                top = inset,
                                right = size.width - inset,
                                bottom = size.height - inset
                            )
                            drawRoundRect(
                                color = if (selected) {
                                    Color.White.copy(alpha = 0.2f)
                                } else {
                                    Color.Transparent
                                },
                                topLeft = rect.topLeft,
                                size = rect.size,
                                cornerRadius = CornerRadius(7.dp.toPx()),
                                style = Stroke(strokeWidth)
                            )
                        }
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = label,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        textColor
                    },
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}

@Composable
fun GradientTypeItemRect(
    label: String,
    selected: Boolean,
    textColor: Color,
    onClick: () -> Unit
) {
    val isDark = true // always on dark scrim overlay
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 500f
        ),
        label = "gradientRectScale"
    )

    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }

    Box(modifier = Modifier.scale(scale)) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (selected) {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                            )
                        )
                    } else if (isDark) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.05f),
                                Color.White.copy(alpha = 0.03f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.03f),
                                Color.Black.copy(alpha = 0.01f)
                            )
                        )
                    }
                )
                .drawBehind {
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
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        textColor
                    },
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    letterSpacing = 0.2.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/* ───────────────── Effect chip ───────────────── */

@Composable
fun EffectChip(
    label: String,
    selected: Boolean,
    fillProgress: Float,
    isActive: Boolean,
    textColor: Color,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    val isDark = true // always on dark scrim overlay
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 500f
        ),
        label = "effectChipScale"
    )

    val shape = RoundedCornerShape(14.dp)

    val borderColor = when {
        isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        isDark -> Color.White.copy(alpha = 0.1f)
        else -> Color.Black.copy(alpha = 0.08f)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .height(48.dp)
            .clip(shape)
    ) {
        // Background layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    if (isActive) {
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
                .drawBehind {
                    val strokeWidth = 1.5f.dp.toPx()
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
                .clickable { onToggle() }
        )

        // Fill progress indicator (ONLY when selected & inactive)
        if (selected && !isActive && fillProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fillProgress.coerceIn(0f, 1f))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        )
                    )
            )
        }

        // Centered label
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isActive) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    textColor.copy(alpha = 0.8f)
                },
                fontSize = 13.sp,
                fontWeight = if (isActive || selected) FontWeight.SemiBold else FontWeight.Medium,
                letterSpacing = 0.3.sp,
                maxLines = 1
            )
        }
    }
}

/* ───────────────── Effect opacity slider ───────────────── */

@Composable
fun EffectOpacitySlider(
    label: String,
    value: Float,
    onSliderChange: (Float) -> Unit,
    labelColor: Color
) {
    val isDark = true // always on dark scrim overlay
    val pct = (value * 100).toInt()
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackInactive = Color.White.copy(alpha = 0.13f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label row
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = 0.2.sp,
            color = labelColor
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$pct%",
                modifier = Modifier.width(44.dp),
                color = labelColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Slider(
                value = value,
                onValueChange = { onSliderChange(it.coerceIn(0f, 1f)) },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = primaryColor,
                    activeTrackColor = primaryColor,
                    inactiveTrackColor = trackInactive
                )
            )

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
            )
        }

    }
}
/* ───────────────── Device frame ───────────────── */

@Composable
fun DeviceFrame(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
        Box(modifier = Modifier.padding(8.dp)) {
            content()
        }
    }
