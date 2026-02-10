/**
 * FloatingNavBar.kt
 *
 * Superior Android luxury navigation.
 * Material You excellence with bold design language,
 * powerful animations, and premium depth effects.
 * This is what true luxury looks like.
 */

package com.example.waller.ui.wallpaper.components

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Material You dynamic colors
    val primary = MaterialTheme.colorScheme.primary
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainerHighest

    // Powerful entrance animation
    var isVisible by remember { mutableStateOf(false) }
    val slideUp by animateFloatAsState(
        targetValue = if (isVisible) 0f else 100f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        ),
        label = "slideUp"
    )

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(
        modifier = modifier
            .offset(y = slideUp.dp)
            .graphicsLayer {
                alpha = if (isVisible) 1f else 0f
            }
            .pointerInput(Unit) {}
    ) {
        // Epic shadow system
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .dynamicShadowLayer(
                    color = primary,
                    isDark = isDark,
                    blur = 32.dp
                )
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .dynamicShadowLayer(
                    color = if (isDark) Color.Black else Color.Black.copy(alpha = 0.5f),
                    isDark = isDark,
                    blur = 24.dp
                )
        )

        // Main container
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.88f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.98f),
                                Color.White.copy(alpha = 0.92f)
                            )
                        }
                    )
                )
                .frostedDepth(
                    primary = primary,
                    isDark = isDark
                )
                .premiumBorderEffect(
                    primary = primary,
                    isDark = isDark,
                    cornerRadius = 20.dp
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                label = "Home",
                icon = Icons.Default.Home,
                selected = selectedItem == FloatingNavItem.HOME,
                onClick = {
                    onItemSelected(FloatingNavItem.HOME)
                    Haptics.confirm(view)
                },
                primary = primary,
                isDark = isDark
            )
            NavItem(
                label = "Favourites",
                icon = Icons.Default.Favorite,
                selected = selectedItem == FloatingNavItem.FAVOURITES,
                onClick = {
                    onItemSelected(FloatingNavItem.FAVOURITES)
                    Haptics.confirm(view)
                },
                primary = primary,
                isDark = isDark
            )
            NavItem(
                label = "Settings",
                icon = Icons.Default.Settings,
                selected = selectedItem == FloatingNavItem.SETTINGS,
                onClick = {
                    onItemSelected(FloatingNavItem.SETTINGS)
                    Haptics.confirm(view)
                },
                primary = primary,
                isDark = isDark
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    primary: Color,
    isDark: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }

    // Powerful spring physics
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 500f
        ),
        label = "itemScale"
    )

    // Morphing shape animation
    val shapeProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 200f
        ),
        label = "shapeProgress"
    )

    // Dynamic color animation
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            primary
        } else {
            Color.Transparent
        },
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 300f
        ),
        label = "bgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            if (isDark) Color.Black else Color.White
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        },
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 300f
        ),
        label = "contentColor"
    )

    // Ripple effect for selection
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rippleScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
    ) {
        // Epic selection indicator with morphing
        if (selected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .scale(rippleScale * 0.3f)
                    .dynamicGlow(
                        color = primary,
                        intensity = 0.4f,
                        radius = 60.dp
                    )
            )
        }

        Row(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = 250f
                    )
                )
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .then(
                    if (selected) {
                        Modifier.premiumSelectedBorder(
                            primary = primary,
                            isDark = isDark
                        )
                    } else Modifier
                )
                .padding(
                    horizontal = if (selected) 20.dp else 16.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Icon with dynamic effects
            Box {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    Icon(
                        icon,
                        contentDescription = label,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                scaleX = 1f + (shapeProgress * 0.1f)
                                scaleY = 1f + (shapeProgress * 0.1f)
                            }
                    )
                }
            }

            // Label with smooth transition
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = 300f
                    )
                ) + expandHorizontally(
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = 250f
                    )
                ),
                exit = fadeOut(
                    animationSpec = tween(150)
                ) + shrinkHorizontally(
                    animationSpec = tween(150)
                )
            ) {
                Row {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// Dynamic shadow with color bleeding
private fun Modifier.dynamicShadowLayer(
    color: Color,
    isDark: Boolean,
    blur: Dp
) = this.drawBehind {
    val adjustedColor = if (isDark) {
        color.copy(alpha = 0.3f)
    } else {
        color.copy(alpha = 0.15f)
    }

    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(
                adjustedColor,
                adjustedColor.copy(alpha = 0f)
            ),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.width * 0.8f
        ),
        cornerRadius = CornerRadius(20.dp.toPx()),
        size = size
    )
}

// Premium multi-tone border
private fun Modifier.premiumBorderEffect(
    primary: Color,
    isDark: Boolean,
    cornerRadius: Dp
) = this.drawBehind {
    val strokeWidth = 2.5f.dp.toPx()
    val inset = strokeWidth / 2f

    val rect = Rect(
        left = inset,
        top = inset,
        right = size.width - inset,
        bottom = size.height - inset
    )

    val radius = cornerRadius.toPx()

    // Outer accent border
    drawRoundRect(
        brush = Brush.sweepGradient(
            colors = if (isDark) {
                listOf(
                    primary.copy(alpha = 0.6f),
                    Color.White.copy(alpha = 0.3f),
                    primary.copy(alpha = 0.8f),
                    Color.White.copy(alpha = 0.2f),
                    primary.copy(alpha = 0.6f)
                )
            } else {
                listOf(
                    primary.copy(alpha = 0.4f),
                    Color.Black.copy(alpha = 0.15f),
                    primary.copy(alpha = 0.5f),
                    Color.Black.copy(alpha = 0.1f),
                    primary.copy(alpha = 0.4f)
                )
            },
            center = Offset(size.width / 2f, size.height / 2f)
        ),
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = CornerRadius(radius),
        style = Stroke(strokeWidth)
    )

    // Inner highlight
    val innerInset = strokeWidth * 2
    val innerRect = Rect(
        left = innerInset,
        top = innerInset,
        right = size.width - innerInset,
        bottom = size.height - innerInset
    )

    drawRoundRect(
        brush = Brush.linearGradient(
            colors = if (isDark) {
                listOf(
                    Color.White.copy(alpha = 0.15f),
                    Color.Transparent,
                    Color.Transparent
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.4f),
                    Color.Transparent,
                    Color.Transparent
                )
            },
            start = Offset(0f, 0f),
            end = Offset(0f, size.height * 0.5f)
        ),
        topLeft = innerRect.topLeft,
        size = innerRect.size,
        cornerRadius = CornerRadius(radius - strokeWidth),
        style = Stroke(1.dp.toPx())
    )
}

// Selected item border glow
private fun Modifier.premiumSelectedBorder(
    primary: Color,
    isDark: Boolean
) = this.drawBehind {
    val strokeWidth = 1.5f.dp.toPx()
    val inset = strokeWidth / 2f

    val rect = Rect(
        left = inset,
        top = inset,
        right = size.width - inset,
        bottom = size.height - inset
    )

    val radius = 16.dp.toPx()

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.2f)
    } else {
        Color.White.copy(alpha = 0.5f)
    }

    drawRoundRect(
        color = borderColor,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = CornerRadius(radius),
        style = Stroke(strokeWidth)
    )
}

// Frosted depth - Android material elevation effect
private fun Modifier.frostedDepth(
    primary: Color,
    isDark: Boolean
) = this.drawBehind {
    // Ambient occlusion at edges
    val aoGradient = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(
                Color.Black.copy(alpha = 0.25f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.15f)
            )
        } else {
            listOf(
                Color.Black.copy(alpha = 0.06f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.03f)
            )
        },
        startY = 0f,
        endY = size.height
    )

    drawRoundRect(
        brush = aoGradient,
        cornerRadius = CornerRadius(20.dp.toPx()),
        size = size
    )

    // Specular highlight - material elevation catch light
    val highlightHeight = size.height * 0.35f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = if (isDark) {
                listOf(
                    Color.White.copy(alpha = 0.04f),
                    Color.Transparent
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.6f),
                    Color.Transparent
                )
            },
            startY = 0f,
            endY = highlightHeight
        ),
        topLeft = Offset(0f, 0f),
        size = size.copy(height = highlightHeight),
        cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
    )

    // Subtle color tint from primary theme
    if (isDark) {
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = Offset(size.width / 2f, size.height * 0.3f),
                radius = size.width * 0.6f
            ),
            cornerRadius = CornerRadius(20.dp.toPx()),
            size = size
        )
    }
}

// Dynamic glow effect
private fun Modifier.dynamicGlow(
    color: Color,
    intensity: Float,
    radius: Dp
) = this.drawBehind {
    val glowRadius = radius.toPx()

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = intensity),
                color.copy(alpha = intensity * 0.5f),
                Color.Transparent
            ),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = glowRadius
        ),
        radius = glowRadius,
        center = Offset(size.width / 2f, size.height / 2f)
    )
}