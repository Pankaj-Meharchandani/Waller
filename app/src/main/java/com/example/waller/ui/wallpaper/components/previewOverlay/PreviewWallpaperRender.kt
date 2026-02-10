/**
 * PreviewWallpaperRender.kt
 *
 * Visual renderer for wallpaper previews.
 *
 * Responsibilities:
 * - Draws gradients, noise, stripes, overlays, and geometric layers
 * - Uses Canvas and images to compose the final preview output
 * - Displays the bottom tag showing gradient type and colors
 *
 * This composable is stateless and contains no screen or interaction logic.
 */

package com.example.waller.ui.wallpaper.components.previewOverlay

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waller.R
import com.example.waller.ui.wallpaper.GradientType
import com.example.waller.ui.wallpaper.Wallpaper
import kotlin.random.Random

@Composable
fun PreviewWallpaperRender(
    wallpaper: Wallpaper,
    previewType: GradientType,
    angleDeg: Float,
    addNoise: Boolean,
    addStripes: Boolean,
    addOverlay: Boolean,
    addGeometric: Boolean,
    noiseAlpha: Float = 1f,
    stripesAlpha: Float = 1f,
    overlayAlpha: Float = 1f,
    geometricAlpha: Float = 1f,
    modifier: Modifier = Modifier,
    showTypeLabel: Boolean = true
) {
    val cornerRadius = 18.dp

    Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius))) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthDp = maxWidth
            val heightDp = maxHeight
            val density = LocalDensity.current
            val widthPx = with(density) { widthDp.toPx() }
            val heightPx = with(density) { heightDp.toPx() }

            val androidColors = wallpaper.colors.map { it.toArgb() }.toIntArray()

            val brush = remember(wallpaper.colors, previewType, angleDeg, widthPx, heightPx) {
                createBrushForPreview(wallpaper.colors, previewType, widthPx, heightPx, angleDeg)
            }

            if (previewType == GradientType.Angular) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sweep =
                        createRotatedSweepShader(size.width, size.height, androidColors, angleDeg)
                    val paint = Paint().apply {
                        isAntiAlias = true
                        shader = sweep
                    }
                    drawContext.canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)

                    if (addNoise && noiseAlpha > 0f) {
                        val noiseSize = 1.dp.toPx().coerceAtLeast(1f)
                        val numNoisePoints =
                            (size.width * size.height / (noiseSize * noiseSize) * 0.02f).toInt()
                        repeat(numNoisePoints) {
                            val x = Random.nextFloat() * size.width
                            val y = Random.nextFloat() * size.height
                            val alpha = (Random.nextFloat() * 0.15f) * noiseAlpha
                            drawCircle(
                                Color.White.copy(alpha = alpha),
                                radius = noiseSize,
                                center = Offset(x, y)
                            )
                        }
                    }

                    if (addStripes && stripesAlpha > 0f) {
                        val stripeCount = 18
                        val stripeWidth = size.width / (stripeCount * 2f)
                        for (i in 0 until stripeCount) {
                            val left = i * stripeWidth * 2f
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
                        painter = painterResource(id = R.drawable.overlay_stripes),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(alpha = overlayAlpha),
                        contentScale = ContentScale.FillBounds
                    )
                }

                if (addGeometric && geometricAlpha > 0f) {
                    Image(
                        painter = painterResource(id = R.drawable.overlay_geometric),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(alpha = geometricAlpha),
                        contentScale = ContentScale.FillWidth
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().background(brush)) {
                    if (addNoise && noiseAlpha > 0f) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val noiseSize = 1.dp.toPx().coerceAtLeast(1f)
                            val numNoisePoints =
                                (size.width * size.height / (noiseSize * noiseSize) * 0.02f).toInt()
                            repeat(numNoisePoints) {
                                val x = Random.nextFloat() * size.width
                                val y = Random.nextFloat() * size.height
                                val alpha = (Random.nextFloat() * 0.15f) * noiseAlpha
                                drawCircle(
                                    Color.White.copy(alpha = alpha),
                                    radius = noiseSize,
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }

                    if (addStripes && stripesAlpha > 0f) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stripeCount = 18
                            val stripeWidth = size.width / (stripeCount * 2f)
                            for (i in 0 until stripeCount) {
                                val left = i * stripeWidth * 2f
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
                            painter = painterResource(id = R.drawable.overlay_stripes),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(alpha = overlayAlpha),
                            contentScale = ContentScale.FillBounds
                        )
                    }

                    if (addGeometric && geometricAlpha > 0f) {
                        Image(
                            painter = painterResource(id = R.drawable.overlay_geometric),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(alpha = geometricAlpha),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }

            // Bottom tag — matches card style (gradient bg, plain swatches)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.70f),
                                Color.Black.copy(alpha = 0.80f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showTypeLabel) {
                    Text(
                        text = previewType.name
                            .lowercase()
                            .replaceFirstChar { it.uppercase() },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp
                    )
                    Spacer(Modifier.width(8.dp))
                }

                wallpaper.colors.forEachIndexed { index, color ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                    if (index != wallpaper.colors.lastIndex) {
                        Spacer(Modifier.width(5.dp))
                    }
                }
            }
        }
    }
}