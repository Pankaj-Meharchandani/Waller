/**
 * WallpaperItemCard.kt
 * Individual wallpaper preview card used inside the lazy grid.
 * Renders:
 * - Gradient background (Compose Brush)
 * - Visual effects driven by EffectMap (loop — no per-effect params)
 * - Bottom-left tag: gradient type + color swatches
 * - Top-right heart icon to mark/unmark as favourite
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.waller.R
import com.example.waller.ui.wallpaper.EffectMap
import com.example.waller.ui.wallpaper.GradientType
import com.example.waller.ui.wallpaper.Wallpaper
import com.example.waller.ui.wallpaper.WallpaperEffects
import com.example.waller.ui.wallpaper.alpha
import com.example.waller.ui.wallpaper.isEnabled
import kotlin.random.Random
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.nativeCanvas
import com.example.waller.ui.wallpaper.components.previewOverlay.createBrushForPreview
import com.example.waller.ui.wallpaper.components.previewOverlay.createRotatedSweepShader
import androidx.compose.ui.platform.LocalView
import com.example.waller.ui.wallpaper.Haptics

@Composable
fun WallpaperItemCard(
    wallpaper: Wallpaper,
    isPortrait: Boolean,
    effects: EffectMap,
    isFavorite: Boolean,
    onFavoriteToggle: (Wallpaper, EffectMap) -> Unit,
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
                onClick = { Haptics.light(view); onClick() },
                onLongClick = onLongClick
            )
    } else {
        modifier
            .aspectRatio(if (isPortrait) 9f / 16f else 16f / 9f)
            .fillMaxWidth()
            .combinedClickable(
                onClick = { Haptics.light(view); onClick() },
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
                effects   = effects
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
                            onFavoriteToggle(wallpaper, effects)
                        },
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

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun WallpaperItem(
    wallpaper: Wallpaper,
    effects: EffectMap
) {
    val addBlur   = effects.isEnabled("blur")
    val blurAlpha = effects.alpha("blur")

    val blurEffect =
        if (addBlur && blurAlpha > 0f && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.graphics.RenderEffect
                .createBlurEffect(18f * blurAlpha, 18f * blurAlpha, android.graphics.Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
        } else null

    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient + effects layer
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { renderEffect = blurEffect }) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

                val density  = LocalDensity.current
                val widthPx  = with(density) { maxWidth.toPx() }
                val heightPx = with(density) { maxHeight.toPx() }
                val androidColors = wallpaper.colors.map { it.toArgb() }.toIntArray()

                val avgLuminance = wallpaper.colors.map { it.luminance() }.average().toFloat()
                val geometricTint = if (avgLuminance > 0.5f) Color.Black else Color.White

                val addNoise    = effects.isEnabled("noise")
                val noiseAlpha  = effects.alpha("noise")
                val addStripes  = effects.isEnabled("stripes")
                val stripesAlpha = effects.alpha("stripes")
                val addOverlay  = effects.isEnabled("overlay")
                val overlayAlpha = effects.alpha("overlay")
                val addGeo      = effects.isEnabled("geometric")
                val geoAlpha    = effects.alpha("geometric")

                if (wallpaper.type == GradientType.Angular) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val sweep = createRotatedSweepShader(widthPx, heightPx, androidColors, wallpaper.angleDeg)
                        val paint = android.graphics.Paint().apply { isAntiAlias = true; shader = sweep }
                        drawContext.canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)

                        if (addNoise && noiseAlpha > 0f) {
                            val noiseSize = 1.dp.toPx().coerceAtLeast(1f)
                            val numPoints = (size.width * size.height / (noiseSize * noiseSize) * 0.02f).toInt()
                            repeat(numPoints) {
                                val x = Random.nextFloat() * size.width
                                val y = Random.nextFloat() * size.height
                                val a = (Random.nextFloat() * 0.15f).coerceIn(0f, 1f) * noiseAlpha
                                drawCircle(Color.White.copy(alpha = a), radius = noiseSize, center = Offset(x, y))
                            }
                        }

                        if (addStripes && stripesAlpha > 0f) {
                            val stripeSpacing = size.width / 12f
                            val stripeWidth   = stripeSpacing / 2f
                            rotate(-45f, pivot = center) {
                                var x = -size.height
                                while (x < size.width * 2f) {
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.18f * stripesAlpha),
                                                Color.Transparent
                                            )
                                        ),
                                        topLeft = Offset(x, -size.height * 2f),
                                        size = Size(stripeWidth, size.height * 4f)
                                    )
                                    x += stripeSpacing
                                }
                            }
                        }
                    }

                    if (addGeo && geoAlpha > 0f) {
                        Image(
                            painter = painterResource(R.drawable.overlay_geometric),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize().graphicsLayer(alpha = geoAlpha),
                            contentScale = ContentScale.FillWidth,
                            colorFilter = ColorFilter.tint(geometricTint)
                        )
                    }

                    if (addOverlay && overlayAlpha > 0f) {
                        Image(
                            painter = painterResource(R.drawable.overlay_stripes),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize().graphicsLayer(alpha = overlayAlpha),
                            contentScale = ContentScale.FillBounds
                        )
                    }

                } else {
                    val brush = createBrushForPreview(wallpaper.colors, wallpaper.type, widthPx, heightPx, wallpaper.angleDeg)

                    Box(modifier = Modifier.matchParentSize().background(brush)) {

                        if (addNoise && noiseAlpha > 0f) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val noiseSize = 1.dp.toPx().coerceAtLeast(1f)
                                val numPoints = (size.width * size.height / (noiseSize * noiseSize) * 0.02f).toInt()
                                repeat(numPoints) {
                                    val x = Random.nextFloat() * size.width
                                    val y = Random.nextFloat() * size.height
                                    val a = (Random.nextFloat() * 0.15f).coerceIn(0f, 1f) * noiseAlpha
                                    drawCircle(Color.White.copy(alpha = a), radius = noiseSize, center = Offset(x, y))
                                }
                            }
                        }

                        if (addStripes && stripesAlpha > 0f) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val stripeSpacing = size.width / 10f
                                val stripeWidth   = stripeSpacing * 0.65f
                                rotate(-45f, pivot = center) {
                                    var x = -size.height
                                    while (x < size.width * 2f) {
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.14f * stripesAlpha),
                                                    Color.White.copy(alpha = 0.08f * stripesAlpha),
                                                    Color.Transparent
                                                ),
                                                startX = x,
                                                endX = x + stripeWidth * 1.4f
                                            ),
                                            topLeft = Offset(x, -size.height * 2f),
                                            size = Size(stripeWidth, size.height * 4f)
                                        )
                                        x += stripeSpacing
                                    }
                                }
                            }
                        }

                        if (addOverlay && overlayAlpha > 0f) {
                            Image(
                                painter = painterResource(R.drawable.overlay_stripes),
                                contentDescription = null,
                                modifier = Modifier.matchParentSize().graphicsLayer(alpha = overlayAlpha),
                                contentScale = ContentScale.FillBounds
                            )
                        }

                        if (addGeo && geoAlpha > 0f) {
                            Image(
                                painter = painterResource(R.drawable.overlay_geometric),
                                contentDescription = null,
                                modifier = Modifier.matchParentSize().graphicsLayer(alpha = geoAlpha),
                                contentScale = ContentScale.FillWidth,
                                colorFilter = ColorFilter.tint(geometricTint)
                            )
                        }
                    }
                }
            }
        }

        // Bottom tag (type + swatches) — outside blur layer so it stays sharp
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black.copy(alpha = 0.8f)
                            )
                        ), shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = wallpaper.type.name.lowercase().replaceFirstChar { it.uppercase() },
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
                    if (index != wallpaper.colors.lastIndex) Spacer(modifier = Modifier.width(6.dp))
                }
            }
        }
    }
}