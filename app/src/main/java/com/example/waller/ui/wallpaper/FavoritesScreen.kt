/**
 * Favourites screen for Waller.
 * Shows only the wallpapers the user has marked with a heart.
 * Uses the stored effect flags (snow / stripes / glass / geometric) from FavoriteWallpaper snapshot.
 * Uses shared orientation state from WallerApp and lets user toggle it via the Header chip.
 */

package com.example.waller.ui.wallpaper

import android.annotation.SuppressLint
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.waller.R
import com.example.waller.ui.wallpaper.components.Header
import com.example.waller.ui.wallpaper.components.WallpaperItemCard
import com.example.waller.ui.wallpaper.components.WallpaperPreviewOverlay
import kotlinx.coroutines.launch

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    isAppDarkMode: Boolean,
    onThemeChange: () -> Unit,
    favourites: List<FavoriteWallpaper>,
    isPortrait: Boolean,
    onOrientationChange: (Boolean) -> Unit,
    onRemoveFavourite: (FavoriteWallpaper) -> Unit,
    onAddFavourite: (FavoriteWallpaper) -> Unit,
    interactionMode: InteractionMode
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    val writePermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.storage_permission_denied),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

    var pendingClickedWallpaper by remember { mutableStateOf<FavoriteWallpaper?>(null) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }

    val spanCount = if (isPortrait) 2 else 1
    val columns = GridCells.Fixed(spanCount)

    LazyVerticalGrid(
        columns = columns,
        state = gridState,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Header
        item(span = { GridItemSpan(spanCount) }) {
            Header(
                onThemeChange = onThemeChange,
                isAppDarkMode = isAppDarkMode,
                showOrientationToggle = true,
                isPortrait = isPortrait,
                onOrientationChange = onOrientationChange
            )
        }

        // Count row
        item(span = { GridItemSpan(spanCount) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (favourites.isEmpty())
                        stringResource(R.string.favourites_empty)
                    else
                        stringResource(R.string.favourites_count, favourites.size),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Grid
        items(favourites) { fav ->
            WallpaperItemCard(
                wallpaper = fav.wallpaper,
                isPortrait = isPortrait,
                addNoise = fav.addNoise,
                addStripes = fav.addStripes,
                addOverlay = fav.addOverlay,
                addGeometric = fav.addGeometric, // ✅ FIXED
                noiseAlpha = fav.noiseAlpha,
                stripesAlpha = fav.stripesAlpha,
                overlayAlpha = fav.overlayAlpha,
                isFavorite = true,
                onFavoriteToggle = { _, _, _, _, _, _, _, _ ->
                    onRemoveFavourite(fav)
                },
                onClick = {
                    when (interactionMode) {
                        InteractionMode.SIMPLE -> {
                            pendingClickedWallpaper = fav
                            showApplyDialog = true
                        }
                        InteractionMode.ADVANCED -> {
                            pendingClickedWallpaper = fav
                            showPreview = true
                        }
                    }
                }
            )
        }

        // Scroll to top
        if (favourites.isNotEmpty()) {
            item(span = { GridItemSpan(spanCount) }) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch { gridState.animateScrollToItem(1) }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(44.dp)
                    ) {
                        Text(stringResource(R.string.scroll_to_top))
                    }
                }
            }
        }
    }

    // Preview overlay
    if (showPreview && pendingClickedWallpaper != null) {
        val fav = pendingClickedWallpaper!!

        WallpaperPreviewOverlay(
            wallpaper = fav.wallpaper,
            isPortrait = isPortrait,
            isFavorite = true,
            globalNoise = fav.addNoise,
            globalStripes = fav.addStripes,
            globalOverlay = fav.addOverlay,
            globalGeometric = fav.addGeometric,
            initialNoiseAlpha = fav.noiseAlpha,
            initialStripesAlpha = fav.stripesAlpha,
            initialOverlayAlpha = fav.overlayAlpha,
            onFavoriteToggle = { snapshot, n, s, o, g, na, sa, oa ->
                val updatedFav = FavoriteWallpaper(
                    wallpaper = snapshot,
                    addNoise = n,
                    addStripes = s,
                    addOverlay = o,
                    addGeometric = g,
                    noiseAlpha = na,
                    stripesAlpha = sa,
                    overlayAlpha = oa
                )

                onRemoveFavourite(fav)
                onAddFavourite(updatedFav)
                pendingClickedWallpaper = updatedFav
            },
                    onDismiss = {
                showPreview = false
                pendingClickedWallpaper = null
            },
            writePermissionLauncher = writePermissionLauncher,
            context = context,
            coroutineScope = coroutineScope
        )
    }

    ApplyDownloadDialog(
        show = showApplyDialog,
        wallpaper = pendingClickedWallpaper?.wallpaper,
        isPortrait = isPortrait,
        addNoise = pendingClickedWallpaper?.addNoise ?: false,
        addStripes = pendingClickedWallpaper?.addStripes ?: false,
        addOverlay = pendingClickedWallpaper?.addOverlay ?: false,
        addGeometric = pendingClickedWallpaper?.addGeometric ?: false,
        noiseAlpha = pendingClickedWallpaper?.noiseAlpha ?: 1f,
        stripesAlpha = pendingClickedWallpaper?.stripesAlpha ?: 1f,
        overlayAlpha = pendingClickedWallpaper?.overlayAlpha ?: 1f,
        isWorking = isWorking,
        onWorkingChange = { isWorking = it },
        onDismiss = {
            showApplyDialog = false
            pendingClickedWallpaper = null
        },
        writePermissionLauncher = writePermissionLauncher,
        context = context,
        coroutineScope = coroutineScope
    )
}
