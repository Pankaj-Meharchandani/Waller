package com.example.waller.ui.wallpaper.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
            text = "Effects",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Noise, stripes, and optional Nothing-style overlay.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Noise
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = addNoise, onCheckedChange = onNoiseChange)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Snow effect")
                Text(
                    "Soft snow texture.",
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
                Text("Stripes overlay")
                Text(
                    "Vertical translucent stripes.",
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
                Text("Nothing Style")
                Text(
                    "Add Nothing like glass effect",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
