/**
 * Favourites screen for Waller.
 * Shows only the wallpapers the user has marked with a heart.
 * Uses the stored effect flags (snow / stripes / glass) from FavoriteWallpaper snapshot.
 */

package com.example.waller.ui.wallpaper

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.waller.R
import com.example.waller.ui.settings.DefaultOrientation
import com.example.waller.ui.wallpaper.components.Header
import com.example.waller.ui.wallpaper.components.WallpaperItemCard
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    isAppDarkMode: Boolean,
    onThemeChange: () -> Unit,
    favourites: List<FavoriteWallpaper>,
    defaultOrientation: DefaultOrientation,
    onRemoveFavourite: (FavoriteWallpaper) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    val isPortrait = when (defaultOrientation) {
        DefaultOrientation.LANDSCAPE -> false
        DefaultOrientation.AUTO, DefaultOrientation.PORTRAIT -> true
    }

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
        item(span = { GridItemSpan(spanCount) }) {
            Header(onThemeChange = onThemeChange, isAppDarkMode = isAppDarkMode)
        }

        item(span = { GridItemSpan(spanCount) }) {
            Text(
                text = if (favourites.isEmpty())
                    stringResource(id = R.string.favourites_empty)
                else
                    stringResource(id = R.string.favourites_count, favourites.size),
            )
        }

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
                    showApplyDialog = true
                }
            )
        }

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

    ApplyDownloadDialog(
        show = showApplyDialog,
        wallpaper = pendingClickedWallpaper?.wallpaper,
        isPortrait = isPortrait,
        addNoise = pendingClickedWallpaper?.addNoise ?: false,
        addStripes = pendingClickedWallpaper?.addStripes ?: false,
        addOverlay = pendingClickedWallpaper?.addOverlay ?: false,
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
