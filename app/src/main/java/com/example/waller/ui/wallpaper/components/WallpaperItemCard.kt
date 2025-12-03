/**
 * Individual wallpaper preview card used inside the lazy grid.
 * Renders:
 * - Gradient background (Compose Brush)
 * - Optional noise and stripe effects
 * - Optional Nothing-style PNG overlay
 * - Bottom-left tag: gradient type + 2 color swatches
 * - Top-right heart icon to mark/unmark as favourite
 *
 * Opens the Apply/Download dialog when tapped.
 */

package com.example.waller.ui.wallpaper.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.waller.R
import com.example.waller.ui.wallpaper.GradientType
import com.example.waller.ui.wallpaper.Wallpaper
import kotlin.random.Random

@Composable
fun WallpaperItemCard(
    wallpaper: Wallpaper,
    isPortrait: Boolean,
    addNoise: Boolean,
    addStripes: Boolean,
    addOverlay: Boolean,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(if (isPortrait) 9f / 16f else 16f / 9f)
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.04f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Actual wallpaper rendering
            WallpaperItem(
                wallpaper = wallpaper,
                addNoise = addNoise,
                addNothingStripes = addStripes,
                addOverlay = addOverlay
            )

            // Favourite heart in top-right with "squarcle" background
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                IconButton(
                    onClick = onFavoriteToggle,
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

@Composable
fun WallpaperItem(
    wallpaper: Wallpaper,
    addNoise: Boolean,
    addNothingStripes: Boolean,
    addOverlay: Boolean
) {
    val brush = when (wallpaper.type) {
        GradientType.Linear -> Brush.linearGradient(wallpaper.colors)
        GradientType.Radial -> Brush.radialGradient(wallpaper.colors)
        GradientType.Angular -> Brush.sweepGradient(wallpaper.colors)
        GradientType.Diamond -> Brush.linearGradient(wallpaper.colors)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 1. Draw gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
        )

        // 2. Noise
        if (addNoise) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val noiseSize = 1.dp.toPx()
                val numNoisePoints =
                    (size.width * size.height / (noiseSize * noiseSize) * 0.02f).toInt()
                repeat(numNoisePoints) {
                    val x = Random.nextFloat() * size.width
                    val y = Random.nextFloat() * size.height
                    val alpha = Random.nextFloat() * 0.15f
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = noiseSize,
                        center = Offset(x, y)
                    )
                }
            }
        }

        // 3. Generated stripes
        if (addNothingStripes) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stripeCount = 18
                val stripeWidth = size.width / (stripeCount * 2f)
                for (i in 0 until stripeCount) {
                    val left = i * stripeWidth * 2f
                    drawRect(
                        color = Color.White.copy(alpha = 0.10f),
                        topLeft = Offset(left, 0f),
                        size = Size(stripeWidth, size.height)
                    )
                }
            }
        }

        // 4. PNG Overlay — ALWAYS LAST
        if (addOverlay) {
            Image(
                painter = painterResource(id = R.drawable.overlay_stripes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }

        // 5. Tag at the bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .background(
                    Color.Black.copy(alpha = 0.36f),
                    shape = RoundedCornerShape(999.dp)
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

            Spacer(Modifier.size(8.dp))

            wallpaper.colors.take(2).forEach {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(it)
                )
                Spacer(Modifier.size(6.dp))
            }
        }
    }
}
