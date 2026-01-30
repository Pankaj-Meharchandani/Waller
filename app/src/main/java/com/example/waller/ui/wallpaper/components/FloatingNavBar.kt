/**
 * FloatingNavBar.kt
 *
 * Theme-aware liquid-glass floating bottom navigation for Waller.
 *
 * Design goals:
 * - Proper glass illusion in BOTH light and dark themes
 * - No content visibility issues
 * - No clipping, no milky overlays, no hacks
 * - Calm, intentional animation
 */

package com.example.waller.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class FloatingNavItem {
    HOME, FAVOURITES, SETTINGS
}

@Composable
fun FloatingNavBar(
    selectedItem: FloatingNavItem,
    onItemSelected: (FloatingNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(26.dp)

    // 🔑 THEME-AWARE GLASS
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val glassBrush = if (isDark) {
        // Dark theme → deep, classy glass
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.06f),
                Color.Black.copy(alpha = 0.55f),
                Color.Black.copy(alpha = 0.70f)
            )
        )
    } else {
        // Light theme → airy glass
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.30f),
                Color(0xFFF1F3F6).copy(alpha = 0.82f),
                Color(0xFFE9ECF1).copy(alpha = 0.88f)
            )
        )
    }

    Box(
        modifier = modifier
            // ⛔ block touch-through
            .pointerInput(Unit) {}
            // soft ambient lift
            .shadow(
                elevation = 22.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.40f else 0.12f),
                spotColor = Color.Black.copy(alpha = if (isDark) 0.55f else 0.16f)
            )
            // glass surface
            .background(
                brush = glassBrush,
                shape = shape
            )
            // glass edge highlight
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = if (isDark) 0.08f else 0.22f),
                shape = shape
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
                isDark = isDark,
                onClick = { onItemSelected(FloatingNavItem.HOME) }
            )

            NavItem(
                label = "Favourites",
                icon = Icons.Default.Favorite,
                selected = selectedItem == FloatingNavItem.FAVOURITES,
                isDark = isDark,
                onClick = { onItemSelected(FloatingNavItem.FAVOURITES) }
            )

            NavItem(
                label = "Settings",
                icon = Icons.Default.Settings,
                selected = selectedItem == FloatingNavItem.SETTINGS,
                isDark = isDark,
                onClick = { onItemSelected(FloatingNavItem.SETTINGS) }
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val tint = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
    }

    Row(
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ✅ INNER ACTIVE PILL (only animated element)
        Row(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    )
                )
                .background(
                    color = if (selected) {
                        if (isDark)
                            Color.Black.copy(alpha = 0.60f)
                        else
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    } else {
                        Color.Transparent
                    },
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
