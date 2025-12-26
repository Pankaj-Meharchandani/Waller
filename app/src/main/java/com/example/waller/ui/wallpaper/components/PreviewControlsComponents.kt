/**
 * PreviewControlsComponents.kt
 *
 * Stateless UI components used by the wallpaper preview overlay.
 * - Gradient type selectors (full + compact)
 * - Effect chips with progress fill
 * - Effect opacity slider
 * - Device frame wrapper for preview content
 *
 * These composables contain NO state and NO business logic.
 * They are safe to reuse across preview variants and future features.
 */

package com.example.waller.ui.wallpaper.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.waller.R
import androidx.compose.ui.res.stringResource

/* ───────────────── Gradient type selectors ───────────────── */

@Composable
fun GradientTypeItemFull(
    label: String,
    selected: Boolean,
    textColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        tonalElevation = if (selected) 6.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (selected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
            )
            Spacer(Modifier.width(12.dp))
            Text(label, color = textColor)
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
    val bg by animateColorAsState(
        targetValue =
            if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                Color.Transparent,
        animationSpec = tween(220),
        label = "gradient_chip_bg"
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        tonalElevation = if (selected) 6.dp else 0.dp,
        border = if (!selected) ButtonDefaults.outlinedButtonBorder else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }
    }
}

/* ───────────────── Effect chip ───────────────── */

@Composable
fun EffectChip(
    label: String,
    selected: Boolean,
    fillProgress: Float,      // 0f..1f
    isActive: Boolean,
    textColor: Color,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .clickable { onToggle() }
            .border(
                1.dp,
                if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape
            )
    ) {
        // Active background (stronger)
        if (isActive) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
                    )
            )
        }

        // 🔹 Variable fill (ONLY when selected & inactive)
        if (selected && !isActive && fillProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fillProgress.coerceIn(0f, 1f))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                    )
            )
        }

        // 🔹 Centered label (always above fill)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val chipTextColor =
                if (isActive)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    Color.White
            Text(
                text = label,
                color = chipTextColor,
                maxLines = 1,
                style = MaterialTheme.typography.labelMedium
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
    Text(
        text = label,
        fontWeight = FontWeight.Medium,
        color = labelColor
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(id = R.string.preview_off),
            modifier = Modifier.width(40.dp),
            color = labelColor
        )

        Slider(
            value = value,
            onValueChange = { onSliderChange(it.coerceIn(0f, 1f)) },
            modifier = Modifier.weight(1f),
            valueRange = 0f..1f
        )

        Text(
            text = stringResource(id = R.string.preview_high),
            modifier = Modifier.width(40.dp),
            color = labelColor
        )
    }
}

/* ───────────────── Device frame ───────────────── */

@Composable
fun DeviceFrame(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Box(modifier = Modifier.padding(6.dp)) {
            content()
        }

        // top notch / speaker hint
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
                .width(48.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.06f)
                )
        )
    }
}
