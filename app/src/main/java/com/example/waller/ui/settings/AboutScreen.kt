/**
 * About screen for Waller.
 * Shows:
 * - App icon, name, version, description
 * - Developer info and community links (GitHub, Telegram channel & group)
 * - Legal / external actions (rate app, privacy policy, terms).
 * Links are clickable and open the browser / app via intents.
 * Opened from the Settings screen; the actual back behavior
 * (device back → Settings) is controlled in WallerApp.
 */

package com.example.waller.ui.settings

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.waller.R
import androidx.core.net.toUri

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    onBackToSettings: () -> Unit
) {
    val context = LocalContext.current

    // 🔹 Get versionName from PackageManager
    val versionName = try {
        val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pkgInfo.versionName ?: "-"
    } catch (_: Exception) {
        "-"
    }

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        // best-effort: let Android handle it, ignore if no activity
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackToSettings) {
                Text(text = stringResource(id = R.string.about_back_to_settings))
            }
        }

        // Top app icon + name area
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Use a drawable (vector or PNG), not mipmap/adaptive
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_playstore),
                    // If you have a custom logo, replace with e.g. R.drawable.ic_waller_logo
                    contentDescription = stringResource(id = R.string.about_app_name),
                    modifier = Modifier
                        .height(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.about_app_name),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(id = R.string.about_app_version, versionName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.about_app_description),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Developer & clickable links
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.about_developer_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(id = R.string.about_developer_name),
                    style = MaterialTheme.typography.bodyMedium
                )

                // Developer GitHub profile
                LinkRow(
                    icon = Icons.Outlined.Person,
                    title = stringResource(id = R.string.about_developer_github_title),
                    value = "github.com/Pankaj-Meharchandani",
                    onClick = { openUrl("https://github.com/Pankaj-Meharchandani") }
                )

                // App source repo
                LinkRow(
                    icon = Icons.Outlined.Code,
                    title = stringResource(id = R.string.about_app_source_title),
                    value = "github.com/Pankaj-Meharchandani/Waller",
                    onClick = { openUrl("https://github.com/Pankaj-Meharchandani/Waller") }
                )

                // Telegram channel
                LinkRow(
                    icon = Icons.AutoMirrored.Outlined.Send,
                    title = stringResource(id = R.string.about_telegram_channel_title),
                    value = "t.me/Waller_app",
                    onClick = { openUrl("https://t.me/Waller_app") }
                )

                // Telegram support group
                LinkRow(
                    icon = Icons.Outlined.Group,
                    title = stringResource(id = R.string.about_telegram_support_title),
                    value = "t.me/walllller",
                    onClick = { openUrl("https://t.me/walllller") }
                )
            }
        }

        // Legal / actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.about_legal_title),
                    style = MaterialTheme.typography.titleMedium
                )

                Button(
                    onClick = {
                        openUrl("https://github.com/Pankaj-Meharchandani/waller")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.about_rate_app))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            // TODO: openUrl("https://your-privacy-policy-url")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(id = R.string.about_privacy_policy)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = {
                            // TODO: openUrl("https://your-terms-url")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(id = R.string.about_terms)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
