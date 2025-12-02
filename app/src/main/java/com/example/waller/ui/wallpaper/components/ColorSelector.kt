/**
 * Handles selection, editing, and removal of up to 5 custom colors.
 * Displays color swatches, hex values, and palette/delete icons.
 * Integrates with the SimpleColorDialog through callbacks in the main screen.
 */

package com.example.waller.ui.wallpaper.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.waller.R
import com.example.waller.ui.wallpaper.toHexString

@Composable
fun ColorSelector(
    selectedColors: List<Color>,
    onAddColor: () -> Unit,
    onEditColor: (Int) -> Unit,
    onRemoveColor: (Int) -> Unit
) {
    Column {
        Text(
            text = stringResource(id = R.string.colors_title, selectedColors.size),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (selectedColors.isEmpty())
                stringResource(id = R.string.color_selector_no_colors)
            else
                stringResource(id = R.string.color_selector_tweak_color_tip),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onAddColor,
                enabled = selectedColors.size < 5,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text(stringResource(id = R.string.color_selector_add_color))
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (selectedColors.isNotEmpty()) {
                OutlinedButton(
                    onClick = { onRemoveColor(selectedColors.lastIndex) },
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(stringResource(id = R.string.color_selector_remove_last))
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (selectedColors.isEmpty()) {
            Text(
                stringResource(id = R.string.color_selector_tip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column {
                selectedColors.forEachIndexed { idx, color ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(color)
                                .border(
                                    1.dp,
                                    Color.Black.copy(alpha = 0.08f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onEditColor(idx) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = color.toHexString(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onEditColor(idx) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.Palette,
                                contentDescription = stringResource(id = R.string.edit),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { onRemoveColor(idx) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(id = R.string.delete),
                                tint = Color.Red.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}
