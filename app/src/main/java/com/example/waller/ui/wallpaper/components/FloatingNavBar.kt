/**
 * FloatingNavBar.kt
 *
 * Theme-aware floating bottom navigation for Waller.
 * Plain surface with a single, light-refracting border
 * (glass illusion via edge only — no blur, no gradients).
 */

package com.example.waller.ui.wallpaper.components

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import com.example.waller.ui.wallpaper.Haptics

enum class FloatingNavItem {
    HOME, FAVOURITES, SETTINGS
}

@Composable
fun FloatingNavBar(
    selectedItem: FloatingNavItem,
    onItemSelected: (FloatingNavItem) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true
) {
    if (!visible) return
    val view = LocalView.current
    val shape = RoundedCornerShape(26.dp)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val surfaceColor =
        if (isDark)
            MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        else
            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)

    Box(
        modifier = modifier
            .pointerInput(Unit){}
            .shadow(
                elevation = 26.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.45f else 0.18f),
                spotColor = Color.Black.copy(alpha = if (isDark) 0.65f else 0.25f)
            )
            .background(surfaceColor, shape)
            .singleRefractiveBorder(
                cornerRadius = 26.dp,
                isDark = isDark
            )
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                label = "Home",
                icon = Icons.Default.Home,
                selected = selectedItem == FloatingNavItem.HOME,
                onClick = { onItemSelected(FloatingNavItem.HOME)
                    Haptics.confirm(view)}
            )
            NavItem(
                label = "Favourites",
                icon = Icons.Default.Favorite,
                selected = selectedItem == FloatingNavItem.FAVOURITES,
                onClick = { onItemSelected(FloatingNavItem.FAVOURITES)
                    Haptics.confirm(view)}
            )
            NavItem(
                label = "Settings",
                icon = Icons.Default.Settings,
                selected = selectedItem == FloatingNavItem.SETTINGS,
                onClick = { onItemSelected(FloatingNavItem.SETTINGS)
                    Haptics.confirm(view)}
            )
        }
    }
}

@SuppressLint("RememberInComposition")
@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tint =
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)

    Row(
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = MutableInteractionSource(),
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                )
                .background(
                    color = if (selected)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    else Color.Transparent,
                    shape = RoundedCornerShape(50)
                )
                .padding(
                    horizontal = if (selected) 14.dp else 10.dp,
                    vertical = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalContentColor provides tint) {
                Icon(icon, contentDescription = label)
            }

            if (selected) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = tint
                )
            }
        }
    }
}

private fun Modifier.singleRefractiveBorder(
    cornerRadius: Dp,
    isDark: Boolean
) = this.drawBehind {

    val strokeWidth = 2.25.dp.toPx()
    val inset = strokeWidth / 2f

    val rect = Rect(
        inset,
        inset,
        size.width - inset,
        size.height - inset
    )

    val radius = cornerRadius.toPx()

    val borderColor =
        if (isDark)
            Color.White.copy(alpha = 0.28f)
        else
            Color.Black.copy(alpha = 0.22f)

    drawRoundRect(
        color = borderColor,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(strokeWidth)
    )
}
