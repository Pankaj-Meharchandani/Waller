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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import com.example.waller.R
import com.example.waller.ui.wallpaper.Haptics
import kotlinx.coroutines.coroutineScope

@Composable
fun Actions(onRefreshClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedButton(
            onClick = onRefreshClick,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(44.dp)
        ) {
            Text(stringResource(id = R.string.actions_refresh_all))
        }
    }
}
