/**
 * Compact 4-row options panel for the wallpaper generator.
 *
 * Rows:
 * 1) Colors (+ Add Color) on the left, Orientation chip on the right
 * 2) Gradient type chips (Linear / Radial / Angular / Diamond)
 * 3) Effect chips (Nothing / Snow / Stripes)
 * 4) Tone slider: Dark • Neutral • Light (neutral is UI-only for now)
 */

package com.example.waller.ui.wallpaper.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.waller.R
import com.example.waller.ui.wallpaper.GradientType

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

        /* ---------------- Row 1: Colors (left) + Orientation (right) ---------------- */

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
                    CompactAddColorChip(onClick = onAddColor)
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
                            onClick = { onRemoveColor(index) }
                        )
                    }

                    if (selectedColors.size < 5) {
                        CompactAddColorChip(onClick = onAddColor)
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // Orientation chip pinned to the right
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

        /* ---------------- Row 2: Gradient type chips ---------------- */

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

        /* ---------------- Row 3: Effects chips ---------------- */

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

        /* ---------------- Row 4: Tone slider (Dark • Neutral • Light) ---------------- */

        ToneSliderRow(
            isLightTones = isLightTones,
            onToneChange = onToneChange
        )
    }
}

/* ----------------------------- Tone slider row ----------------------------- */

@Composable
private fun ToneSliderRow(
    isLightTones: Boolean,
    onToneChange: (Boolean) -> Unit
) {
    // 0 = Dark, 1 = Neutral, 2 = Light
    var position by remember(isLightTones) {
        mutableStateOf(if (isLightTones) 2 else 0)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.wallpaper_theme_title),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
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
                            .clickable {
                                position = index
                                when (index) {
                                    0 -> onToneChange(false) // dark
                                    2 -> onToneChange(true)  // light
                                    1 -> {
                                        // Neutral: UI only for now, no logic change
                                    }
                                }
                            }
                            .background(
                                color = if (selected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/* ------------------------ Squircle color chip (UI-only) ------------------------ */

@Composable
private fun ColorSquareChip(
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(12.dp),   // squircle
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "×",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

/* ------------------------ Compact Add Color rectangular chip ------------------------ */

@Composable
private fun CompactAddColorChip(
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),    // rectangular, not oval
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+ Add Color",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}