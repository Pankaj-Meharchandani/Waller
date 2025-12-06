/**
 * Favourites screen for Waller.
 * Shows only the wallpapers the user has marked with a heart.
 * Uses the stored effect flags (snow / stripes / glass) from FavoriteWallpaper snapshot.
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
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.example.waller.ui.wallpaper.components.WallpaperPreviewOverlay

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
    onAddFavourite: (FavoriteWallpaper) -> Unit // NEW: callback to add back a favourite
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

    // NEW: preview overlay visible state
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
        // Header with orientation chip (for symmetry with Home)
        item(span = { GridItemSpan(spanCount) }) {
            Header(
                onThemeChange = onThemeChange,
                isAppDarkMode = isAppDarkMode,
                showOrientationToggle = true,
                isPortrait = isPortrait,
                onOrientationChange = { newValue -> onOrientationChange(newValue) }
            )
        }

        // Favourites count row
        item(span = { GridItemSpan(spanCount) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val countText =
                    if (favourites.isEmpty())
                        stringResource(id = R.string.favourites_empty)
                    else
                        stringResource(id = R.string.favourites_count, favourites.size)

                Text(
                    text = countText,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Favourites grid
        items(favourites) { fav ->
            WallpaperItemCard(
                wallpaper = fav.wallpaper,
                isPortrait = isPortrait,
                addNoise = fav.addNoise,
                addStripes = fav.addStripes,
                addOverlay = fav.addOverlay,
                isFavorite = true,
                onFavoriteToggle = {
                    onRemoveFavourite(fav)
                },
                onClick = {
                    pendingClickedWallpaper = fav
                    showPreview = true
                }
            )
        }

        // Scroll-to-top button
        if (favourites.isNotEmpty()) {
            item(span = { GridItemSpan(spanCount) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
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
                        Text(stringResource(id = R.string.scroll_to_top))
                    }
                }
            }
        }
    }

    // PREVIEW OVERLAY (opened when a favourite is tapped)
    if (showPreview && pendingClickedWallpaper != null) {

        val fav = pendingClickedWallpaper!!

        // local overlay favourite UI state
        var overlayIsFavorite by remember(fav) { mutableStateOf(true) }

        WallpaperPreviewOverlay(
            wallpaper = fav.wallpaper,
            isPortrait = isPortrait,
            isFavorite = overlayIsFavorite,

            globalNoise = fav.addNoise,
            globalStripes = fav.addStripes,
            globalOverlay = fav.addOverlay,

            onFavoriteToggle = { n, s, o ->
                // toggle UI immediately
                overlayIsFavorite = !overlayIsFavorite

                if (overlayIsFavorite) {
                    // re-add with UPDATED effect flags from the overlay
                    val updatedFav = FavoriteWallpaper(
                        wallpaper = fav.wallpaper,
                        addNoise = n,
                        addStripes = s,
                        addOverlay = o
                    )
                    onAddFavourite(updatedFav)

                    // update the pending snapshot so Apply uses correct effects
                    pendingClickedWallpaper = updatedFav

                } else {
                    // remove old favourite
                    onRemoveFavourite(fav)
                }
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
        isWorking = isWorking,
        onWorkingChange = { isWorking = it }, //don't dismiss this warning
        onDismiss = {
            showApplyDialog = false //don't dismiss this warning
            pendingClickedWallpaper = null //don't dismiss this warning
        },
        writePermissionLauncher = writePermissionLauncher,
        context = context,
        coroutineScope = coroutineScope
    )
}
