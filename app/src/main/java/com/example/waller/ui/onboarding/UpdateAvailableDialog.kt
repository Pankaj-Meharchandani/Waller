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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        val isDark =
            MaterialTheme.colorScheme.background.luminance() < 0.5f

        Card(
            modifier = Modifier
                .border(
                    width = 3.dp,
                    color = if (isDark) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                    },
                    shape = RoundedCornerShape(20.dp)
                ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        {
            Column(
                modifier = Modifier.padding(
                    top = 28.dp,
                    bottom = 20.dp,
                    start = 24.dp,
                    end = 24.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = stringResource(R.string.update_available_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(
                        R.string.update_available_message,
                        latestVersion
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (releaseNotes.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.whats_new),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = formatReleaseNotes(releaseNotes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
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
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        )
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Text(
                                stringResource(R.string.later),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }


                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl))
                            )
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            stringResource(R.string.update),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun formatReleaseNotes(raw: String): String =
    raw
        .replace(Regex("^#{1,6}\\s*", RegexOption.MULTILINE), "")
        .replace(Regex("^-\\s*", RegexOption.MULTILINE), "• ")
        .trim()
