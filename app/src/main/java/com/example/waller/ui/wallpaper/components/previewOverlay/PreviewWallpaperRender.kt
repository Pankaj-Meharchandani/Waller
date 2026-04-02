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
 * Effect rendering is driven by EffectMap — no per-effect params needed.
 * This file does not need to change when new effects are added.
 */

package com.example.waller.ui.wallpaper.components.previewOverlay

import android.annotation.SuppressLint
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waller.R
import com.example.waller.ui.wallpaper.EffectMap
import com.example.waller.ui.wallpaper.GradientType
import com.example.waller.ui.wallpaper.Wallpaper
import com.example.waller.ui.wallpaper.WallpaperEffects
import com.example.waller.ui.wallpaper.alpha
import com.example.waller.ui.wallpaper.isEnabled
import kotlin.random.Random

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun PreviewWallpaperRender(
    wallpaper: Wallpaper,
    previewType: GradientType,
    angleDeg: Float,
    effects: EffectMap,
    modifier: Modifier = Modifier,
    showTypeLabel: Boolean = true
) {
    val cornerRadius = 12.dp

    val addBlur   = effects.isEnabled("blur")
    val blurAlpha = effects.alpha("blur")
    val addNoise  = effects.isEnabled("noise")
    val noiseAlpha = effects.alpha("noise")
    val addStripes = effects.isEnabled("stripes")
    val stripesAlpha = effects.alpha("stripes")
    val addOverlay = effects.isEnabled("overlay")
    val overlayAlpha = effects.alpha("overlay")
    val addGeo     = effects.isEnabled("geometric")
    val geoAlpha   = effects.alpha("geometric")

    val blurEffect = if (addBlur && blurAlpha > 0f && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        android.graphics.RenderEffect
            .createBlurEffect(22f * blurAlpha, 22f * blurAlpha, android.graphics.Shader.TileMode.CLAMP)
            .asComposeRenderEffect()
    } else null

    Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius))) {
        // Gradient + effects — blurred as a unit, label excluded
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { renderEffect = blurEffect }) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val widthDp  = maxWidth
                val heightDp = maxHeight
                val density  = LocalDensity.current
                val widthPx  = with(density) { widthDp.toPx() }
                val heightPx = with(density) { heightDp.toPx() }
                val androidColors = wallpaper.colors.map { it.toArgb() }.toIntArray()

                val brush = remember(wallpaper.colors, previewType, angleDeg, widthPx, heightPx) {
                    createBrushForPreview(wallpaper.colors, previewType, widthPx, heightPx, angleDeg)
                }

                if (previewType == GradientType.Angular) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val sweep = createRotatedSweepShader(size.width, size.height, androidColors, angleDeg)
                        val paint = Paint().apply { isAntiAlias = true; shader = sweep }
                        drawContext.canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)

                        if (addNoise && noiseAlpha > 0f) {
                            val noiseSize = 1.dp.toPx().coerceAtLeast(1f)
                            val numPoints = (size.width * size.height / (noiseSize * noiseSize) * 0.02f).toInt()
                            
                            // Seed Random with angleDeg to synchronize snow speed with live speed
                            val seed = (angleDeg * 2f).toLong()
                            val rnd = Random(seed)
                            
                            repeat(numPoints) {
                                val x = rnd.nextFloat() * size.width
                                val y = rnd.nextFloat() * size.height
                                val a = (rnd.nextFloat() * 0.15f) * noiseAlpha
                                drawCircle(Color.White.copy(alpha = a), radius = noiseSize, center = Offset(x, y))
                            }
                        }

                        if (addStripes && stripesAlpha > 0f) {
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
                                            startX = x, endX = x + stripeWidth * 1.4f
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
                            painter = painterResource(id = R.drawable.overlay_stripes),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().graphicsLayer(alpha = overlayAlpha),
                            contentScale = ContentScale.FillBounds
                        )
                    }

                    if (addGeo && geoAlpha > 0f) {
                        Image(
                            painter = painterResource(id = R.drawable.overlay_geometric),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().graphicsLayer(alpha = geoAlpha),
                            contentScale = ContentScale.FillWidth
                        )
                    }

                } else {
                    Box(modifier = Modifier.fillMaxSize().background(brush)) {

                        if (addNoise && noiseAlpha > 0f) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val noiseSize = 1.dp.toPx().coerceAtLeast(1f)
                                val numPoints = (size.width * size.height / (noiseSize * noiseSize) * 0.02f).toInt()
                                
                                // Seed Random with angleDeg to synchronize snow speed with live speed
                                val seed = (angleDeg * 2f).toLong()
                                val rnd = Random(seed)
                                
                                repeat(numPoints) {
                                    val x = rnd.nextFloat() * size.width
                                    val y = rnd.nextFloat() * size.height
                                    val a = (rnd.nextFloat() * 0.15f) * noiseAlpha
                                    drawCircle(Color.White.copy(alpha = a), radius = noiseSize, center = Offset(x, y))
                                }
                            }
                        }

                        if (addStripes && stripesAlpha > 0f) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
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
                                                startX = x, endX = x + stripeWidth * 1.4f
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
                                painter = painterResource(id = R.drawable.overlay_stripes),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().graphicsLayer(alpha = overlayAlpha),
                                contentScale = ContentScale.FillBounds
                            )
                        }

                        if (addGeo && geoAlpha > 0f) {
                            Image(
                                painter = painterResource(id = R.drawable.overlay_geometric),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().graphicsLayer(alpha = geoAlpha),
                                contentScale = ContentScale.FillWidth
                            )
                        }
                    }
                }
            }
        }
    }
}
