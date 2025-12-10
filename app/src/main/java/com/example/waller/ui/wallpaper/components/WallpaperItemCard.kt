/**
 * WallpaperItemCard.kt
 * Individual wallpaper preview card used inside the lazy grid.
 * Renders:
 * - Gradient background (Compose Brush)
 * - Optional noise and stripe effects
 * - Optional Nothing-style PNG overlay
 * - Bottom-left tag: gradient type + color swatches
 * - Top-right heart icon visually outside the card to mark/unmark as favourite
 * - Shows saved angle badge when angle != 0
 *
 * Opens the Apply/Download dialog when tapped.
 */

@file:Suppress("DEPRECATION", "COMPOSE_APPLIER_CALL_MISMATCH")
package com.example.waller.ui.wallpaper.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.waller.R
import com.example.waller.ui.wallpaper.GradientType
import com.example.waller.ui.wallpaper.Wallpaper

/**
 * Changed signature: onFavoriteToggle now gets the Wallpaper and current effect flags
 * so parents can create a proper FavoriteWallpaper snapshot (with angle/type).
 *
 * onFavoriteToggle: (wallpaper, addNoise, addStripes, addOverlay) -> Unit
 */
@Composable
fun WallpaperItemCard(
    wallpaper: Wallpaper,
    isPortrait: Boolean,
    addNoise: Boolean,
    addStripes: Boolean,
    addOverlay: Boolean,
    isFavorite: Boolean,
    onFavoriteToggle: (Wallpaper, Boolean, Boolean, Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPreview: Boolean = false
) {
    // choose a modifier based on preview vs grid item
    val cardModifier = if (isPreview) {
        // large preview sizing (caller may override with modifier param if desired)
        modifier
            .fillMaxWidth()
            .height(if (isPortrait) 600.dp else 420.dp)
            .clickable { onClick() }
    } else {
        modifier
            .aspectRatio(if (isPortrait) 9f / 16f else 16f / 9f)
            .fillMaxWidth()
            .clickable { onClick() }
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(if (isPreview) 14.dp else 18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPreview) 12.dp else 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = if (isPreview) 0.02f else 0.04f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Actual wallpaper rendering (same behavior)
            WallpaperItem(
                wallpaper = wallpaper,
                addNoise = addNoise,
                addNothingStripes = addStripes,
                addOverlay = addOverlay
            )

            // Floating fav button placed slightly outside the card bounds (top-right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    // push outside the card by negative offset on Y
                    .offset(x = 8.dp, y = (-8).dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = Color.Black.copy(alpha = 0.35f)
                ) {
                    IconButton(
                        onClick = { onFavoriteToggle(wallpaper, addNoise, addStripes, addOverlay) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) Color(0xFFFF4D6A) else Color.White
                        )
                    }
                }
            }
        }
    }
}

/* WallpaperItem kept as in your original file — rendering logic for gradient/effects/overlay */

@Composable
fun WallpaperItem(
    wallpaper: Wallpaper,
    addNoise: Boolean,
    addNothingStripes: Boolean,
    addOverlay: Boolean
) {
    // For linear / radial / diamond we can keep Compose Brushes.
    // For Angular we draw with a native SweepGradient and a rotation matrix
    // so the wallpaper.angleDeg is respected — same approach as PreviewRenderer.
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        BoxWithConstraints(modifier = Modifier.matchParentSize()) {
            val widthDp = maxWidth
            val heightDp = maxHeight
            val density = LocalDensity.current
            val widthPx = with(density) { widthDp.toPx() }
            val heightPx = with(density) { heightDp.toPx() }

            val androidColors = wallpaper.colors.map { it.toArgb() }.toIntArray()

            if (wallpaper.type == GradientType.Angular) {
                // Draw rotated sweep on native canvas so angle is effective
                Canvas(modifier = Modifier.matchParentSize()) {
                    val sweep = createRotatedSweepShader(widthPx, heightPx, androidColors, wallpaper.angleDeg)
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        shader = sweep
                    }
                    // draw the sweep background
                    drawContext.canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)

                    // noise (same approach as before)
                    if (addNoise) {
                        val noiseSize = 1.dp.toPx().coerceAtLeast(1f)
                        val numNoisePoints = (size.width * size.height / (noiseSize * noiseSize) * 0.02f).toInt()
                        repeat(numNoisePoints) {
                            val x = kotlin.random.Random.nextFloat() * size.width
                            val y = kotlin.random.Random.nextFloat() * size.height
                            val alpha = kotlin.random.Random.nextFloat() * 0.15f
                            drawCircle(Color.White.copy(alpha = alpha), radius = noiseSize, center = Offset(x, y))
                        }
                    }

                    // stripes
                    if (addNothingStripes) {
                        val stripeCount = 18
                        val stripeWidth = size.width / (stripeCount * 2f)
                        for (i in 0 until stripeCount) {
                            val left = i * stripeWidth * 2f
                            drawRect(Color.White.copy(alpha = 0.10f), topLeft = Offset(left, 0f), size = Size(stripeWidth, size.height))
                        }
                    }
                }

                // overlay PNG if enabled
                if (addOverlay) {
                    Image(
                        painter = painterResource(id = R.drawable.overlay_stripes),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.FillBounds
                    )
                }
            } else {
                // Non-angular: use Compose brushes (linear / radial / diamond fallback)
                val brush = when (wallpaper.type) {
                    GradientType.Linear, GradientType.Diamond -> Brush.linearGradient(wallpaper.colors)
                    GradientType.Radial -> Brush.radialGradient(wallpaper.colors)
                    else -> Brush.linearGradient(wallpaper.colors)
                }

                Box(modifier = Modifier.matchParentSize().background(brush)) {
                    if (addNoise) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val noiseSize = 1.dp.toPx().coerceAtLeast(1f)
                            val numNoisePoints = (size.width * size.height / (noiseSize * noiseSize) * 0.02f).toInt()
                            repeat(numNoisePoints) {
                                val x = kotlin.random.Random.nextFloat() * size.width
                                val y = kotlin.random.Random.nextFloat() * size.height
                                val alpha = kotlin.random.Random.nextFloat() * 0.15f
                                drawCircle(Color.White.copy(alpha = alpha), radius = noiseSize, center = Offset(x, y))
                            }
                        }
                    }

                    if (addNothingStripes) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val stripeCount = 18
                            val stripeWidth = size.width / (stripeCount * 2f)
                            for (i in 0 until stripeCount) {
                                val left = i * stripeWidth * 2f
                                drawRect(Color.White.copy(alpha = 0.10f), topLeft = Offset(left, 0f), size = Size(stripeWidth, size.height))
                            }
                        }
                    }

                    if (addOverlay) {
                        Image(
                            painter = painterResource(id = R.drawable.overlay_stripes),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.FillBounds
                        )
                    }
                }
            }

            // bottom tag (type + swatches) — unchanged from your current code
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.36f), shape = RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = wallpaper.type.name
                        .lowercase()
                        .replaceFirstChar { it.uppercase() },
                    color = Color.White
                )

                Spacer(Modifier.width(8.dp))

                wallpaper.colors.forEachIndexed { index, color ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                    if (index != wallpaper.colors.lastIndex) {
                        Spacer(Modifier.width(6.dp))
                    }
                }
            }
        }
    }
}
