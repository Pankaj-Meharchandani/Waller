/**
 * Top header section of the screen.
 * Displays:
 * - App icon area
 * - App title + tagline
 * - Optional orientation toggle chip (Portrait / Landscape) on the right
 *
 * Note: onThemeChange / isAppDarkMode are kept for compatibility but not used
 * here anymore. App theme is controlled from the Settings screen.
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
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.waller.R
import androidx.compose.ui.platform.LocalView
import com.example.waller.ui.wallpaper.Haptics

@Composable
fun Header(
    onThemeChange: () -> Unit,          // kept for backwards compatibility (unused)
    isAppDarkMode: Boolean,             // kept for backwards compatibility (unused)
    showOrientationToggle: Boolean = false,
    isPortrait: Boolean = true,
    onOrientationChange: (Boolean) -> Unit = {}
) {
    val chipSize = 42.dp
    val chipShape = RoundedCornerShape(12.dp)
    val view = LocalView.current

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

        Column (modifier = Modifier.weight(4f) ) {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(id = R.string.header_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (showOrientationToggle) {
            val pillShape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .clip(pillShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        pillShape
                    )
                    .clickable {
                        Haptics.confirm(view)
                        onOrientationChange(!isPortrait)
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPortrait)
                            Icons.Filled.StayCurrentPortrait
                        else
                            Icons.Filled.DesktopWindows,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPortrait)
                            stringResource(id = R.string.orientation_portrait)
                        else
                            stringResource(id = R.string.orientation_landscape),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
