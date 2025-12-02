/**
 * UI section containing the "Refresh All" button.
 * Triggers regeneration of the entire wallpaper list.
 * A simple, reusable composable used inside the main generator screen.
 */

package com.example.waller.ui.wallpaper.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.waller.R

@Composable
fun Actions(onRefreshClick: () -> Unit) {
    Column {
        Text(
            text = stringResource(id = R.string.actions_title),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onRefreshClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(stringResource(id = R.string.actions_refresh_all))
        }
    }
}
