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
import androidx.compose.ui.unit.dp
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
            text = "Colors (${selectedColors.size}/5)",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (selectedColors.isEmpty())
                "No colors selected - random palettes will be used."
            else
                "Tap a swatch or palette icon to tweak a color.",
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
                Text("+ Add Color")
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (selectedColors.isNotEmpty()) {
                OutlinedButton(
                    onClick = { onRemoveColor(selectedColors.lastIndex) },
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("- Remove Last")
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (selectedColors.isEmpty()) {
            Text(
                "Tip: Lock 1–5 base colors and let Waller generate shades around them.",
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
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { onRemoveColor(idx) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = Color.Red.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}
