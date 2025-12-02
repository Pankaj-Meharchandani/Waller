/**
 * Checkbox selector for choosing which gradient types to generate:
 * Linear, Radial, Angular, Diamond.
 *
 * Includes "Select All" and "Clear" quick actions.
 * The selected types control variation in the preview grid and output wallpapers.
 */

package com.example.waller.ui.wallpaper.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.waller.R
import com.example.waller.ui.wallpaper.GradientType

@Composable
fun GradientStylesSelector(
    selectedGradientTypes: List<GradientType>,
    onStyleChange: (GradientType) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit
) {
    Column {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                text = stringResource(id = R.string.gradient_styles_title, selectedGradientTypes.size),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onSelectAll) { Text(stringResource(id = R.string.select_all)) }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onClear) { Text(stringResource(id = R.string.clear)) }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            stringResource(id = R.string.gradient_styles_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(10.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked = GradientType.Linear in selectedGradientTypes,
                onCheckedChange = { onStyleChange(GradientType.Linear) }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(id = R.string.gradient_style_linear))
            Spacer(modifier = Modifier.width(16.dp))
            Checkbox(
                checked = GradientType.Radial in selectedGradientTypes,
                onCheckedChange = { onStyleChange(GradientType.Radial) }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(id = R.string.gradient_style_radial))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked = GradientType.Angular in selectedGradientTypes,
                onCheckedChange = { onStyleChange(GradientType.Angular) }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(id = R.string.gradient_style_angular))
            Spacer(modifier = Modifier.width(16.dp))
            Checkbox(
                checked = GradientType.Diamond in selectedGradientTypes,
                onCheckedChange = { onStyleChange(GradientType.Diamond) }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(id = R.string.gradient_style_diamond))
        }
    }
}
