/**
 * WallpaperItemCard.kt
 * Individual wallpaper preview card used inside the lazy grid.
 * Renders:
 * - Gradient background (Compose Brush)
 * - Optional noise and stripe effects
 * - Optional Nothing-style PNG overlay
 * - Bottom-left tag: gradient type + color swatches
 * - Top-right heart icon visually inside the card to mark/unmark as favourite
 * - Shows saved angle badge when angle != 0
 *
 * Opens the Apply/Download dialog when tapped.
 */

@file:Suppress("DEPRECATION")
package com.example.waller.ui.wallpaper.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.waller.R
import com.example.waller.ui.wallpaper.GradientType
import com.example.waller.ui.wallpaper.Wallpaper
import kotlin.random.Random
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun WallpaperItemCard(
    wallpaper: Wallpaper,
    isPortrait: Boolean,
    addNoise: Boolean,
    addStripes: Boolean,
    addOverlay: Boolean,
    // per-effect alpha values to control opacities on cards (defaults for safety)
    noiseAlpha: Float = 1f,
    stripesAlpha: Float = 1f,
    overlayAlpha: Float = 1f,
    isFavorite: Boolean,
    // updated callback includes alphas
    onFavoriteToggle: (Wallpaper, Boolean, Boolean, Boolean, Float, Float, Float) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPreview: Boolean = false
) {
    val cardModifier = if (isPreview) {
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

            // Wallpaper renderer uses alpha values
            WallpaperItem(
                wallpaper = wallpaper,
                addNoise = addNoise,
                addNothingStripes = addStripes,
                addOverlay = addOverlay,
                noiseAlpha = noiseAlpha,
                stripesAlpha = stripesAlpha,
                overlayAlpha = overlayAlpha
            )

            // Floating fav button placed inside the card bounds near the top-right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Black.copy(alpha = 0.30f),
                    tonalElevation = 2.dp,
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(
                        onClick = { onFavoriteToggle(wallpaper, addNoise, addStripes, addOverlay, noiseAlpha, stripesAlpha, overlayAlpha) },
                        modifier = Modifier.size(40.dp)
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

@Composable
fun WallpaperItem(
    wallpaper: Wallpaper,
    addNoise: Boolean,
    addNothingStripes: Boolean,
    addOverlay: Boolean,
    noiseAlpha: Float = 1f,
    stripesAlpha: Float = 1f,
    overlayAlpha: Float = 1f
) {
    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthDp = maxWidth
            val heightDp = maxHeight
            val density = LocalDensity.current
            val widthPx = with(density) { widthDp.toPx() }
            val heightPx = with(density) { heightDp.toPx() }

            val androidColors = wallpaper.colors.map { it.toArgb() }.toIntArray()

            if (wallpaper.type == GradientType.Angular) {
                // use native sweep shader so angleDeg rotation works
                Canvas(modifier = Modifier.matchParentSize()) {
                    val sweep = createRotatedSweepShader(widthPx, heightPx, androidColors, wallpaper.angleDeg)
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        shader = sweep
                    }
                    drawContext.canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)

                    if (addNoise && noiseAlpha > 0f) {
                        val noiseSize = 1.dp.toPx().coerceAtLeast(1f)
                        val numNoisePoints = (size.width * size.height / (noiseSize * noiseSize) * 0.02f).toInt()
                        repeat(numNoisePoints) {
                            val x = Random.nextFloat() * size.width
                            val y = Random.nextFloat() * size.height
                            val alpha = (Random.nextFloat() * 0.15f).coerceIn(0f, 1f) * noiseAlpha
                            drawCircle(Color.White.copy(alpha = alpha), radius = noiseSize, center = Offset(x, y))
                        }
                    }

                    if (addNothingStripes && stripesAlpha > 0f) {
                        val stripeCount = 18
                        val stripeWidth = size.width / (stripeCount * 2f)
                        for (i in 0 until stripeCount) {
                            val left = i * stripeWidth * 2f
                            drawRect(Color.White.copy(alpha = 0.10f * stripesAlpha), topLeft = Offset(left, 0f), size = Size(stripeWidth, size.height))
                        }
                    }
                }

                if (addOverlay && overlayAlpha > 0f) {
                    Image(
                        painter = painterResource(id = R.drawable.overlay_stripes),
                        contentDescription = null,
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer(alpha = overlayAlpha),
                        contentScale = ContentScale.FillBounds
                    )
                }
            } else {
                // Use our shared helper so linear/diamond/radial respect angleDeg consistently.
                val brush = createBrushForPreview(wallpaper.colors, wallpaper.type, widthPx, heightPx, wallpaper.angleDeg)

                Box(modifier = Modifier.matchParentSize().background(brush)) {
                    if (addNoise && noiseAlpha > 0f) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val noiseSize = 1.dp.toPx().coerceAtLeast(1f)
                            val numNoisePoints = (size.width * size.height / (noiseSize * noiseSize) * 0.02f).toInt()
                            repeat(numNoisePoints) {
                                val x = Random.nextFloat() * size.width
                                val y = Random.nextFloat() * size.height
                                val alpha = (Random.nextFloat() * 0.15f).coerceIn(0f, 1f) * noiseAlpha
                                drawCircle(Color.White.copy(alpha = alpha), radius = noiseSize, center = Offset(x, y))
                            }
                        }
                    }

                    if (addNothingStripes && stripesAlpha > 0f) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val stripeCount = 18
                            val stripeWidth = size.width / (stripeCount * 2f)
                            for (i in 0 until stripeCount) {
                                val left = i * stripeWidth * 2f
                                drawRect(Color.White.copy(alpha = 0.10f * stripesAlpha), topLeft = Offset(left, 0f), size = Size(stripeWidth, size.height))
                            }
                        }
                    }

                    if (addOverlay && overlayAlpha > 0f) {
                        Image(
                            painter = painterResource(id = R.drawable.overlay_stripes),
                            contentDescription = null,
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer(alpha = overlayAlpha),
                            contentScale = ContentScale.FillBounds
                        )
                    }
                }
            }

            // bottom tag (type + swatches) — unchanged visual
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

                Spacer(modifier = Modifier.width(8.dp))

                wallpaper.colors.forEachIndexed { index, color ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                    if (index != wallpaper.colors.lastIndex) {
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
            }
        }
    }
}
