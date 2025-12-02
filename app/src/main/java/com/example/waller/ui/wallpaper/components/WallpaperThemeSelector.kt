/**
 * Switch selector for Light vs Dark wallpaper tone modes.
 * Influences random color generation and shade creation.
 * This affects both preview thumbnails and final exported bitmaps.
 */

package com.example.waller.ui.wallpaper.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.waller.R

@Composable
fun WallpaperThemeSelector(isLightTones: Boolean, onThemeChange: (Boolean) -> Unit) {
    Column {
        Text(
            text = stringResource(id = R.string.wallpaper_theme_title),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(text = stringResource(id = R.string.wallpaper_theme_dark_tones))
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isLightTones,
                onCheckedChange = { onThemeChange(it) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.wallpaper_theme_light_tones))
        }
    }
}
