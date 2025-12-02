/**
 * Top header section of the screen.
 * Displays:
 * - App icon area
 * - App title + tagline
 * - Light/Dark theme toggle
 *
 * Pure presentation component with no business logic.
 */

package com.example.waller.ui.wallpaper.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.waller.R

@Composable
fun Header(onThemeChange: () -> Unit, isAppDarkMode: Boolean) {
    val chipSize = 42.dp
    val chipShape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(chipSize)
                .clip(chipShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    chipShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Palette,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(id = R.string.header_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(chipSize)
                .clip(chipShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    chipShape
                )
                .clickable { onThemeChange() },
            contentAlignment = Alignment.Center
        ) {
            if (isAppDarkMode) {
                Icon(
                    Icons.Filled.DarkMode,
                    contentDescription = stringResource(id = R.string.theme_dark),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    Icons.Filled.LightMode,
                    contentDescription = stringResource(id = R.string.theme_light),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
