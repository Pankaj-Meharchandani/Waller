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
    onAddFavourite: (FavoriteWallpaper) -> Unit,
    interactionMode: com.example.waller.ui.wallpaper.InteractionMode
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
                // pass stored alphas so the card renders with the stored opacity values
                noiseAlpha = fav.noiseAlpha,
                stripesAlpha = fav.stripesAlpha,
                overlayAlpha = fav.overlayAlpha,
                isFavorite = true,
                onFavoriteToggle = { w, n, s, o, na, sa, oa ->
                    // clicking heart on a favourite should remove that favourite
                    onRemoveFavourite(fav)
                },
                onClick = {
                    when (interactionMode) {
                        com.example.waller.ui.wallpaper.InteractionMode.SIMPLE -> {
                            // in favourites we can use pendingClickedWallpaper & showApplyDialog
                            pendingClickedWallpaper = fav
                            showApplyDialog = true
                        }
                        com.example.waller.ui.wallpaper.InteractionMode.ADVANCED -> {
                            pendingClickedWallpaper = fav
                            showPreview = true
                        }
                    }
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

        // Helper: compare colors list exactly via hex (stable string form).
        fun sameColors(a: List<androidx.compose.ui.graphics.Color>, b: List<androidx.compose.ui.graphics.Color>): Boolean {
            if (a.size != b.size) return false
            for (i in a.indices) if (a[i].toHexString() != b[i].toHexString()) return false
            return true
        }

        // Find the stored favourite that corresponds to a given wallpaper snapshot.
        // 1) try exact match (including angle),
        // 2) fallback to match by type + colors (backward-compatibility; ignores angle).
        fun findStoredFavouriteFor(snapshot: Wallpaper): FavoriteWallpaper? {
            // exact match (data class equality will compare angle if your Wallpaper includes it)
            favourites.find { it.wallpaper == snapshot }?.let { return it }

            // fallback: match by type + exact color stops
            for (stored in favourites) {
                if (stored.wallpaper.type == snapshot.type && sameColors(stored.wallpaper.colors, snapshot.colors)) {
                    return stored
                }
            }
            return null
        }

        // Initialize overlayIsFavorite by checking stored favourites (exact or fallback).
        var overlayIsFavorite by remember(fav, favourites) {
            mutableStateOf(findStoredFavouriteFor(fav.wallpaper) != null)
        }

        WallpaperPreviewOverlay(
            wallpaper = fav.wallpaper,
            isPortrait = isPortrait,
            isFavorite = overlayIsFavorite,
            globalNoise = fav.addNoise,
            globalStripes = fav.addStripes,
            globalOverlay = fav.addOverlay,
            initialNoiseAlpha = fav.noiseAlpha,
            initialStripesAlpha = fav.stripesAlpha,
            initialOverlayAlpha = fav.overlayAlpha,
            onFavoriteToggle = { wallpaperSnapshot, n, s, o, na, sa, oa ->
                overlayIsFavorite = !overlayIsFavorite

                if (overlayIsFavorite) {
                    val updatedFav = FavoriteWallpaper(
                        wallpaper = wallpaperSnapshot,
                        addNoise = n,
                        addStripes = s,
                        addOverlay = o,
                        noiseAlpha = na,
                        stripesAlpha = sa,
                        overlayAlpha = oa
                    )

                    // Avoid duplicate: if there's already a compatible stored fav, remove it first so we replace.
                    val existing = findStoredFavouriteFor(wallpaperSnapshot)
                    if (existing != null) {
                        onRemoveFavourite(existing)
                    }

                    onAddFavourite(updatedFav)
                    // update pendingClickedWallpaper so apply/save uses the new snapshot
                    pendingClickedWallpaper = updatedFav

                } else {
                    // REMOVE: remove the stored favourite corresponding to this snapshot (exact or fallback)
                    val stored = findStoredFavouriteFor(wallpaperSnapshot) ?: fav
                    onRemoveFavourite(stored)

                    // keep overlay open, update pending to the removed entry so UI remains consistent
                    pendingClickedWallpaper = stored
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
