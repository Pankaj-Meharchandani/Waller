/**
 * WallpaperItemCard.kt
 * Individual wallpaper preview card used inside the lazy grid.
 * Renders:
 * - Gradient background (Compose Brush)
 * - Optional noise and stripe effects
 * - Optional Nothing-style PNG overlay
 * - Optional geometric overlay (PNG / VectorDrawable)
 * - Bottom-left tag: gradient type + color swatches
 * - Top-right heart icon visually inside the card to mark/unmark as favourite
 *
 * Opens the Apply/Download dialog when tapped.
 */

@file:Suppress("DEPRECATION")
package com.example.waller.ui.wallpaper.components

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.waller.R
import com.example.waller.ui.wallpaper.GradientType
import com.example.waller.ui.wallpaper.Wallpaper
import kotlin.random.Random
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import com.example.waller.ui.wallpaper.components.previewOverlay.createBrushForPreview
import com.example.waller.ui.wallpaper.components.previewOverlay.createRotatedSweepShader
import androidx.compose.ui.platform.LocalView
import com.example.waller.ui.wallpaper.Haptics

@Composable
fun WallpaperItemCard(
    wallpaper: Wallpaper,
    isPortrait: Boolean,
    addNoise: Boolean,
    addStripes: Boolean,
    addOverlay: Boolean,
    addGeometric: Boolean,
    noiseAlpha: Float = 1f,
    stripesAlpha: Float = 1f,
    overlayAlpha: Float = 1f,
    geometricAlpha: Float = 1f,
    isFavorite: Boolean,
    onFavoriteToggle: (Wallpaper, Boolean, Boolean, Boolean, Boolean, Float, Float, Float, Float) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    isPreview: Boolean = false
) {
    val view = LocalView.current
    val cardModifier = if (isPreview) {
        modifier
            .fillMaxWidth()
            .height(if (isPortrait) 600.dp else 420.dp)
            .combinedClickable(
                onClick = {
                    Haptics.light(view)
                    onClick()
                },
                onLongClick = onLongClick
            )
    } else {
        modifier
            .aspectRatio(if (isPortrait) 9f / 16f else 16f / 9f)
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    Haptics.light(view)
                    onClick()
                },
                onLongClick = onLongClick
            )
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

            WallpaperItem(
                wallpaper = wallpaper,
                addNoise = addNoise,
                addNothingStripes = addStripes,
                addOverlay = addOverlay,
                addGeometric = addGeometric,
                noiseAlpha = noiseAlpha,
                stripesAlpha = stripesAlpha,
                overlayAlpha = overlayAlpha,
                geometricAlpha=geometricAlpha
            )

            // Favourite button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.70f),
                    tonalElevation = 2.dp,
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(
                        onClick = {
                            Haptics.confirm(view)
                            onFavoriteToggle(
                                wallpaper,
                                addNoise,
                                addStripes,
                                addOverlay,
                                addGeometric,
                                noiseAlpha,
                                stripesAlpha,
                                overlayAlpha,
                                geometricAlpha
                            )
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite)
                                Icons.Filled.Favorite
                            else
                                Icons.Outlined.FavoriteBorder,
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
    addGeometric: Boolean,
    noiseAlpha: Float = 1f,
    stripesAlpha: Float = 1f,
    overlayAlpha: Float = 1f,
    geometricAlpha: Float = 1f
) {
    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }

            val androidColors = wallpaper.colors.map { it.toArgb() }.toIntArray()

            // 🔹 Compute contrast tint for geometric overlay
            val avgLuminance = wallpaper.colors
                .map { it.luminance() }
                .average()
                .toFloat()

            val geometricTint =
                if (avgLuminance > 0.5f) Color.Black else Color.White

            if (wallpaper.type == GradientType.Angular) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val sweep = createRotatedSweepShader(
                        widthPx,
                        heightPx,
                        androidColors,
                        wallpaper.angleDeg
                    )
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        shader = sweep
                    }
                    drawContext.canvas.nativeCanvas.drawRect(
                        0f, 0f, size.width, size.height, paint
                    )

                    if (addNoise && noiseAlpha > 0f) {
                        val noiseSize = 1.dp.toPx().coerceAtLeast(1f)
                        val numNoisePoints =
                            (size.width * size.height / (noiseSize * noiseSize) * 0.02f).toInt()
                        repeat(numNoisePoints) {
                            val x = Random.nextFloat() * size.width
                            val y = Random.nextFloat() * size.height
                            val alpha =
                                (Random.nextFloat() * 0.15f).coerceIn(0f, 1f) * noiseAlpha
                            drawCircle(
                                Color.White.copy(alpha = alpha),
                                radius = noiseSize,
                                center = Offset(x, y)
                            )
                        }
                    }

                    if (addNothingStripes && stripesAlpha > 0f) {
                        val stripeCount = 18
                        val stripeWidth = size.width / (stripeCount * 2f)
                        repeat(stripeCount) {
                            val left = it * stripeWidth * 2f
                            drawRect(
                                Color.White.copy(alpha = 0.10f * stripesAlpha),
                                topLeft = Offset(left, 0f),
                                size = Size(stripeWidth, size.height)
                            )
                        }
                    }
                }

                // 🔹 GEOMETRIC OVERLAY (NEW)
                if (addGeometric && geometricAlpha > 0f) {
                    Image(
                        painter = painterResource(R.drawable.overlay_geometric),
                        contentDescription = null,
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer(alpha = geometricAlpha),
                        contentScale = ContentScale.FillWidth,
                        colorFilter = ColorFilter.tint(geometricTint)
                    )
                }

                if (addOverlay && overlayAlpha > 0f) {
                    Image(
                        painter = painterResource(R.drawable.overlay_stripes),
                        contentDescription = null,
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer(alpha = overlayAlpha),
                        contentScale = ContentScale.FillBounds
                    )
                }
            } else {
                val brush = createBrushForPreview(
                    wallpaper.colors,
                    wallpaper.type,
                    widthPx,
                    heightPx,
                    wallpaper.angleDeg
                )

                Box(modifier = Modifier.matchParentSize().background(brush)) {

                    if (addNoise && noiseAlpha > 0f) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val noiseSize = 1.dp.toPx().coerceAtLeast(1f)
                            val numNoisePoints =
                                (size.width * size.height / (noiseSize * noiseSize) * 0.02f).toInt()
                            repeat(numNoisePoints) {
                                val x = Random.nextFloat() * size.width
                                val y = Random.nextFloat() * size.height
                                val alpha =
                                    (Random.nextFloat() * 0.15f).coerceIn(0f, 1f) * noiseAlpha
                                drawCircle(
                                    Color.White.copy(alpha = alpha),
                                    radius = noiseSize,
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }

                    if (addNothingStripes && stripesAlpha > 0f) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val stripeCount = 18
                            val stripeWidth = size.width / (stripeCount * 2f)
                            repeat(stripeCount) {
                                val left = it * stripeWidth * 2f
                                drawRect(
                                    Color.White.copy(alpha = 0.10f * stripesAlpha),
                                    topLeft = Offset(left, 0f),
                                    size = Size(stripeWidth, size.height)
                                )
                            }
                        }
                    }

                    if (addOverlay && overlayAlpha > 0f) {
                        Image(
                            painter = painterResource(R.drawable.overlay_stripes),
                            contentDescription = null,
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer(alpha = overlayAlpha),
                            contentScale = ContentScale.FillBounds
                        )
                    }
                    if (addGeometric && geometricAlpha > 0f) {
                        Image(
                            painter = painterResource(R.drawable.overlay_geometric),
                            contentDescription = null,
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer(alpha = geometricAlpha),
                            contentScale = ContentScale.FillWidth,
                            colorFilter = ColorFilter.tint(geometricTint)
                        )
                    }
                }
            }

            // bottom tag (type + swatches)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .background(brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                        , shape = RoundedCornerShape(12.dp)
                    )
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
                    Box(
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
