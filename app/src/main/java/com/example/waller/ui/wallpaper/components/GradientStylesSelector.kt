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
import androidx.compose.ui.unit.dp
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
                text = "Gradient Styles (${selectedGradientTypes.size}/4)",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onSelectAll) { Text("Select All") }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onClear) { Text("Clear") }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "Mix multiple styles for a varied grid.",
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
            Text("Linear")
            Spacer(modifier = Modifier.width(16.dp))
            Checkbox(
                checked = GradientType.Radial in selectedGradientTypes,
                onCheckedChange = { onStyleChange(GradientType.Radial) }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Radial")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked = GradientType.Angular in selectedGradientTypes,
                onCheckedChange = { onStyleChange(GradientType.Angular) }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Angular")
            Spacer(modifier = Modifier.width(16.dp))
            Checkbox(
                checked = GradientType.Diamond in selectedGradientTypes,
                onCheckedChange = { onStyleChange(GradientType.Diamond) }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Diamond")
        }
    }
}
