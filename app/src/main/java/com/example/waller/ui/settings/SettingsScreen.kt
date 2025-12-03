/**
 * Settings screen for Waller.
 * Contains:
 * - Theme settings (app theme, gradient background)
 * - Wallpaper defaults (orientation, gradient count, default effects, default tone)
 * - A final "About" section that opens the About screen.
 */

package com.example.waller.ui.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.waller.R
import com.example.waller.ui.wallpaper.SectionCard
import com.example.waller.ui.wallpaper.ToneMode

// App-wide theme modes used by WallerApp and Settings.
enum class AppThemeMode { LIGHT, DARK, SYSTEM }

// Default wallpaper orientation options.
enum class DefaultOrientation { AUTO, PORTRAIT, LANDSCAPE }

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    appThemeMode: AppThemeMode,
    onAppThemeModeChange: (AppThemeMode) -> Unit,
    useGradientBackground: Boolean,
    onUseGradientBackgroundChange: (Boolean) -> Unit,
    defaultOrientation: DefaultOrientation,
    onDefaultOrientationChange: (DefaultOrientation) -> Unit,
    defaultGradientCount: Int,
    onDefaultGradientCountChange: (Int) -> Unit,
    enableNothingByDefault: Boolean,
    onEnableNothingByDefaultChange: (Boolean) -> Unit,
    enableSnowByDefault: Boolean,
    onEnableSnowByDefaultChange: (Boolean) -> Unit,
    enableStripesByDefault: Boolean,
    onEnableStripesByDefaultChange: (Boolean) -> Unit,
    defaultToneMode: ToneMode,
    onDefaultToneModeChange: (ToneMode) -> Unit,
    onAboutClick: () -> Unit
) {
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Theme Settings
        SectionCard {
            Text(
                text = stringResource(id = R.string.settings_section_theme),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.settings_theme_app_theme),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(6.dp))

            ThemeOptionRow(
                label = stringResource(id = R.string.settings_theme_system),
                selected = appThemeMode == AppThemeMode.SYSTEM,
                onClick = { onAppThemeModeChange(AppThemeMode.SYSTEM) }
            )
            ThemeOptionRow(
                label = stringResource(id = R.string.settings_theme_light),
                selected = appThemeMode == AppThemeMode.LIGHT,
                onClick = { onAppThemeModeChange(AppThemeMode.LIGHT) }
            )
            ThemeOptionRow(
                label = stringResource(id = R.string.settings_theme_dark),
                selected = appThemeMode == AppThemeMode.DARK,
                onClick = { onAppThemeModeChange(AppThemeMode.DARK) }
            )

            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.settings_theme_use_gradient_bg),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = useGradientBackground,
                    onCheckedChange = onUseGradientBackgroundChange
                )
            }
        }

        // Wallpaper settings
        SectionCard {
            Text(
                text = stringResource(id = R.string.settings_section_wallpaper),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.settings_wallpaper_orientation),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(6.dp))

            OrientationOptionRow(
                label = stringResource(id = R.string.settings_orientation_auto),
                selected = defaultOrientation == DefaultOrientation.AUTO,
                onClick = { onDefaultOrientationChange(DefaultOrientation.AUTO) }
            )
            OrientationOptionRow(
                label = stringResource(id = R.string.settings_orientation_portrait),
                selected = defaultOrientation == DefaultOrientation.PORTRAIT,
                onClick = { onDefaultOrientationChange(DefaultOrientation.PORTRAIT) }
            )
            OrientationOptionRow(
                label = stringResource(id = R.string.settings_orientation_landscape),
                selected = defaultOrientation == DefaultOrientation.LANDSCAPE,
                onClick = { onDefaultOrientationChange(DefaultOrientation.LANDSCAPE) }
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(id = R.string.settings_wallpaper_count),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(6.dp))
            GradientCountRow(
                current = defaultGradientCount,
                onChange = onDefaultGradientCountChange
            )

            Spacer(Modifier.height(12.dp))

            // Default wallpaper tone (3 radio options: Dark / Neutral / Light)
            Text(
                text = stringResource(id = R.string.settings_default_wallpaper_tone),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(6.dp))

            ThemeOptionRow(
                label = stringResource(id = R.string.wallpaper_theme_dark_tones),
                selected = defaultToneMode == ToneMode.DARK,
                onClick = { onDefaultToneModeChange(ToneMode.DARK) }
            )
            ThemeOptionRow(
                label = stringResource(id = R.string.wallpaper_theme_neutral_tones),
                selected = defaultToneMode == ToneMode.NEUTRAL,
                onClick = { onDefaultToneModeChange(ToneMode.NEUTRAL) }
            )
            ThemeOptionRow(
                label = stringResource(id = R.string.wallpaper_theme_light_tones),
                selected = defaultToneMode == ToneMode.LIGHT,
                onClick = { onDefaultToneModeChange(ToneMode.LIGHT) }
            )

            Spacer(Modifier.height(12.dp))

            ToggleRow(
                label = stringResource(id = R.string.settings_enable_nothing),
                checked = enableNothingByDefault,
                onCheckedChange = onEnableNothingByDefaultChange
            )
            ToggleRow(
                label = stringResource(id = R.string.settings_enable_snow),
                checked = enableSnowByDefault,
                onCheckedChange = onEnableSnowByDefaultChange
            )
            ToggleRow(
                label = stringResource(id = R.string.settings_enable_stripes),
                checked = enableStripesByDefault,
                onCheckedChange = onEnableStripesByDefaultChange
            )
        }

        // About section entry – now using SectionCard + full-card clickable
        SectionCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAboutClick)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.settings_section_about),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.settings_about_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(text = label)
    }
}

@Composable
private fun OrientationOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(text = label)
    }
}

@Composable
private fun GradientCountRow(
    current: Int,
    onChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        listOf(12, 16, 20).forEach { value ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = current == value,
                    onClick = { onChange(value) }
                )
                Text(text = value.toString())
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
