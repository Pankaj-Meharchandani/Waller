/**
 * ModePickerDialog.kt
 *
 * Onboarding dialog to pick Simple / Advanced interaction mode.
 *
 * - Renders two large selectable cards with icons
 * - Full-card click selects the mode; selected card gets highlighted
 * - Cancel (secondary) on the left, Confirm (primary) on the right
 * - Shows a note that this setting can be changed anytime
 */

package com.example.waller.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.waller.ui.wallpaper.InteractionMode

@Composable
fun ModePickerDialog(
    initialSelection: InteractionMode = InteractionMode.SIMPLE,
    onChosen: (InteractionMode) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(initialSelection) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                Modifier.padding(
                    top = 24.dp,
                    bottom = 16.dp,
                    start = 24.dp,
                    end = 24.dp
                )
            ) {

                // Title
                Text(
                    text = "Choose interaction mode",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(24.dp))

                // Cards row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ModeCard(
                        label = "Simple",
                        sublabel = "Quick apply — skips preview",
                        icon = Icons.Filled.Bolt,
                        selected = selected == InteractionMode.SIMPLE,
                        onClick = { selected = InteractionMode.SIMPLE },
                        modifier = Modifier.weight(1f)
                    )

                    ModeCard(
                        label = "Advanced",
                        sublabel = "Show full preview & controls",
                        icon = Icons.Filled.Tune,
                        selected = selected == InteractionMode.ADVANCED,
                        onClick = { selected = InteractionMode.ADVANCED },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Footer note
                Text(
                    text = "You can change this anytime from Settings.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(18.dp))

                // Custom button row: Cancel left-most, Confirm right-most
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { onChosen(selected) },
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    label: String,
    sublabel: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = if (selected)
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    else
        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        border = border,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon circle
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(label, style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(6.dp))

            Text(
                text = sublabel,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}
