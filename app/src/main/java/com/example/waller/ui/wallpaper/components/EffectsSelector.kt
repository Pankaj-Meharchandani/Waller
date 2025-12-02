package com.example.waller.ui.wallpaper.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.waller.R

@Composable
fun EffectsSelector(
    addNoise: Boolean,
    onNoiseChange: (Boolean) -> Unit,
    addStripes: Boolean,
    onStripesChange: (Boolean) -> Unit,
    addOverlay: Boolean,
    onOverlayChange: (Boolean) -> Unit
) {
    Column {
        Text(
            text = stringResource(id = R.string.effects_title),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(id = R.string.effects_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Noise
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = addNoise, onCheckedChange = onNoiseChange)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(stringResource(id = R.string.effects_snow_effect))
                Text(
                    stringResource(id = R.string.effects_snow_effect_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Generated stripes
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = addStripes, onCheckedChange = onStripesChange)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(stringResource(id = R.string.effects_stripes_overlay))
                Text(
                    stringResource(id = R.string.effects_stripes_overlay_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Nothing style
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = addOverlay, onCheckedChange = onOverlayChange)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(stringResource(id = R.string.effects_nothing_style))
                Text(
                    stringResource(id = R.string.effects_nothing_style_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
