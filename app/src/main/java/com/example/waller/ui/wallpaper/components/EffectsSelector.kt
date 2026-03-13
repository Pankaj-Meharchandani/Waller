/**
 * UI toggles for enabling or disabling visual effects.
 *
 * Loops over WallpaperEffects.ALL — no per-effect params here.
 * Adding a new effect only requires an entry in WallpaperEffects.ALL.
 */

package com.example.waller.ui.wallpaper.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.waller.R
import com.example.waller.ui.wallpaper.EffectMap
import com.example.waller.ui.wallpaper.WallpaperEffects
import com.example.waller.ui.wallpaper.isEnabled
import com.example.waller.ui.wallpaper.withEnabled

private val EFFECT_INLINE_LABELS = mapOf(
    "blur" to "Blur"
)
private val EFFECT_INLINE_SUBTITLES = mapOf(
    "blur"      to "Frosted glass blur over the gradient",
    "geometric" to "Subtle geometric grid lines"
)

@Composable
fun EffectsSelector(
    effects: EffectMap,
    onEffectChange: (EffectMap) -> Unit
) {
    Column {
        Text(
            text = stringResource(id = R.string.effects_title),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(id = R.string.effects_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        WallpaperEffects.ALL.forEachIndexed { index, def ->
            if (index > 0) Spacer(modifier = Modifier.height(12.dp))

            val label = if (def.labelRes != 0)
                stringResource(id = def.labelRes)
            else
                EFFECT_INLINE_LABELS[def.id] ?: def.id

            val subtitle = if (def.subtitleRes != 0)
                stringResource(id = def.subtitleRes)
            else
                EFFECT_INLINE_SUBTITLES[def.id]

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = effects.isEnabled(def.id),
                    onCheckedChange = { enabled ->
                        onEffectChange(effects.withEnabled(def.id, enabled))
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(label)
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}