package com.example.waller.ui.wallpaper.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OrientationSelector(isPortrait: Boolean, onOrientationChange: (Boolean) -> Unit) {
    Column {
        Text(
            text = "Orientation",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            if (isPortrait) {
                Button(
                    onClick = {},
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Icon(Icons.Filled.StayCurrentPortrait, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Portrait")
                }
            } else {
                OutlinedButton(
                    onClick = { onOrientationChange(true) },
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Icon(Icons.Filled.StayCurrentPortrait, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Portrait")
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (!isPortrait) {
                Button(
                    onClick = {},
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Icon(Icons.Filled.DesktopWindows, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Landscape")
                }
            } else {
                OutlinedButton(
                    onClick = { onOrientationChange(false) },
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Icon(Icons.Filled.DesktopWindows, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Landscape")
                }
            }
        }
    }
}
