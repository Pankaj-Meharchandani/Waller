/**
 * UpdateAvailableDialog
 *
 * - Waller-style modal dialog
 * - Matches ModePickerDialog look & feel
 * - Shows version + release notes
 * - Allows user to update or dismiss
 */

package com.example.waller.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.waller.R

@Composable
fun UpdateAvailableDialog(
    latestVersion: String,
    releaseNotes: String,
    releaseUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(
                    top = 24.dp,
                    bottom = 16.dp,
                    start = 24.dp,
                    end = 24.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = stringResource(R.string.update_available_title),
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(
                        R.string.update_available_message,
                        latestVersion
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                if (releaseNotes.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.whats_new),
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .verticalScroll(scrollState)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text(stringResource(R.string.later))
                    }

                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(releaseUrl)
                                )
                            )
                            onDismiss()
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text(stringResource(R.string.update))
                    }
                }
            }
        }
    }
}
