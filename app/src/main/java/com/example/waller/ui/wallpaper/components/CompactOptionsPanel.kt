package com.example.waller.ui.wallpaper.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.waller.R
import com.example.waller.ui.wallpaper.GradientType

/**
 * Compact 4-row options panel for the wallpaper generator.
 *
 * Rows:
 * 1) Dark / Light tones + Portrait / Landscape chip
 * 2) + Add Color button + up to 5 color dots (tap to remove)
 * 3) 4 gradient type chips (Linear / Radial / Angular / Diamond)
 * 4) Effect chips (Nothing / Snow / Stripes)
 */

@Composable
fun CompactOptionsPanel(
    isPortrait: Boolean,
    onOrientationChange: (Boolean) -> Unit,
    isLightTones: Boolean,
    onToneChange: (Boolean) -> Unit,
    selectedColors: List<Color>,
    onAddColor: () -> Unit,
    onRemoveColor: (Int) -> Unit,
    selectedGradientTypes: List<GradientType>,
    onGradientToggle: (GradientType) -> Unit,
    addNoise: Boolean,
    onNoiseToggle: () -> Unit,
    addStripes: Boolean,
    onStripesToggle: () -> Unit,
    addOverlay: Boolean,
    onOverlayToggle: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Row 1: tone + orientation
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = !isLightTones,
                    onClick = { onToneChange(false) },
                    label = { Text(stringResource(id = R.string.wallpaper_theme_dark_tones)) },
                    shape = RoundedCornerShape(999.dp)
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = isLightTones,
                    onClick = { onToneChange(true) },
                    label = { Text(stringResource(id = R.string.wallpaper_theme_light_tones)) },
                    shape = RoundedCornerShape(999.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            // Orientation as a chip so it matches the rest
            FilterChip(
                selected = true,
                onClick = { onOrientationChange(!isPortrait) },
                leadingIcon = {
                    Icon(
                        imageVector = if (isPortrait)
                            Icons.Filled.StayCurrentPortrait
                        else
                            Icons.Filled.DesktopWindows,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                label = {
                    Text(
                        text = stringResource(
                            id = if (isPortrait)
                                R.string.orientation_portrait
                            else
                                R.string.orientation_landscape
                        )
                    )
                },
                shape = RoundedCornerShape(999.dp)
            )
        }

        // Row 2: colors
        if (selectedColors.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AssistChip(
                    onClick = onAddColor,
                    label = { Text(stringResource(id = R.string.color_selector_add_color)) }
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                selectedColors.forEachIndexed { index, color ->
                    ColorDot(
                        color = color,
                        onClick = { onRemoveColor(index) }
                    )
                }

                if (selectedColors.size < 5) {
                    AssistChip(
                        onClick = onAddColor,
                        label = { Text(stringResource(id = R.string.color_selector_add_color)) }
                    )
                }
            }
        }

        // Row 3: gradient types
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GradientType.values().forEach { type ->
                val label = when (type) {
                    GradientType.Linear -> R.string.gradient_style_linear
                    GradientType.Radial -> R.string.gradient_style_radial
                    GradientType.Angular -> R.string.gradient_style_angular
                    GradientType.Diamond -> R.string.gradient_style_diamond
                }
                FilterChip(
                    selected = type in selectedGradientTypes,
                    onClick = { onGradientToggle(type) },
                    label = { Text(stringResource(id = label)) },
                    shape = RoundedCornerShape(999.dp)
                )
            }
        }

        // Row 4: effects
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = addOverlay,
                onClick = onOverlayToggle,
                label = { Text(stringResource(id = R.string.effects_nothing_style)) },
                shape = RoundedCornerShape(999.dp)
            )
            FilterChip(
                selected = addNoise,
                onClick = onNoiseToggle,
                label = { Text(stringResource(id = R.string.effects_snow_effect)) },
                shape = RoundedCornerShape(999.dp)
            )
            FilterChip(
                selected = addStripes,
                onClick = onStripesToggle,
                label = { Text(stringResource(id = R.string.effects_stripes)) },
                shape = RoundedCornerShape(999.dp)
            )
        }
    }
}

/* ------------------------------ Color dot UI ------------------------------ */

@Composable
private fun ColorDot(
    color: Color,
    onClick: () -> Unit
) {
    // Small color circle with an “x” on top
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "×",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}
