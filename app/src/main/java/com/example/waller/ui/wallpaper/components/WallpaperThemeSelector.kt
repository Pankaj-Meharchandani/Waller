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
import androidx.compose.ui.unit.dp

@Composable
fun WallpaperThemeSelector(isLightTones: Boolean, onThemeChange: (Boolean) -> Unit) {
    Column {
        Text(
            text = "Wallpaper Theme",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(text = "Dark Tones")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isLightTones,
                onCheckedChange = { onThemeChange(it) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Light Tones")
        }
    }
}
