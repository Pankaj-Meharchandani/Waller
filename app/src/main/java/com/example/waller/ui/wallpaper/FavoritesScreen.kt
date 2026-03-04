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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waller.R
import com.example.waller.ui.wallfile.WallFileManager
import com.example.waller.ui.wallpaper.components.Header
import com.example.waller.ui.wallpaper.components.WallpaperItemCard
import com.example.waller.ui.wallpaper.components.premiumAddColorBorder
import com.example.waller.ui.wallpaper.components.previewOverlay.WallpaperPreviewOverlay
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
    val importWallLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->

            var importedCount = 0
            val importedKeys = mutableSetOf<String>()

            uris.forEach { uri ->

                val imported = WallFileManager.importWallFile(context, uri)

                imported?.forEach { fav ->

                    val key =
                        "${fav.wallpaper}_${fav.addNoise}_${fav.addStripes}_${fav.addOverlay}_${fav.addGeometric}"

                    val alreadyExistsInApp = favourites.any { existing ->
                        existing.wallpaper == fav.wallpaper &&
                                existing.addNoise == fav.addNoise &&
                                existing.addStripes == fav.addStripes &&
                                existing.addOverlay == fav.addOverlay &&
                                existing.addGeometric == fav.addGeometric
                    }

                    if (!alreadyExistsInApp && !importedKeys.contains(key)) {
                        importedKeys.add(key)
                        onAddFavourite(fav)
                        importedCount++
                    }
                }
            }

            val message = when {
                importedCount == 0 ->
                    context.getString(R.string.wallpaper_already_exists)

                importedCount == 1 ->
                    context.getString(R.string._1_wallpaper_imported)

                else ->
                    "$importedCount wallpapers imported"
            }

            android.widget.Toast
                .makeText(context, message, android.widget.Toast.LENGTH_SHORT)
                .show()
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
    var showPreview by remember { mutableStateOf(false) }
    var showImportExportDialog by remember { mutableStateOf(false) }

    val spanCount = if (isPortrait) 2 else 1
    val columns = GridCells.Fixed(spanCount)

    LazyVerticalGrid(
        columns = columns,
        state = gridState,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(
            top = 12.dp,
            bottom = 12.dp + 96.dp),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = if (favourites.isEmpty())
                        stringResource(R.string.favourites_empty)
                    else
                        stringResource(R.string.favourites_count, favourites.size),
                    style = MaterialTheme.typography.titleMedium
                )

                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .premiumAddColorBorder(
                            isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                        )
                        .clickable {
                            showImportExportDialog = true
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.import_export),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Grid
        items(favourites.asReversed()) { fav ->
            WallpaperItemCard(
                wallpaper = fav.wallpaper,
                isPortrait = isPortrait,
                addNoise = fav.addNoise,
                addStripes = fav.addStripes,
                addOverlay = fav.addOverlay,
                addGeometric = fav.addGeometric,
                noiseAlpha = fav.noiseAlpha,
                stripesAlpha = fav.stripesAlpha,
                overlayAlpha = fav.overlayAlpha,
                geometricAlpha = fav.geometricAlpha,
                isFavorite = true,
                onFavoriteToggle = { _, _, _, _, _, _, _, _, _ ->
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
                },
                onLongClick = {
                    pendingClickedWallpaper = fav
                    showApplyDialog = true
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
                        shape = RoundedCornerShape(14.dp),
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
            initialGeometricAlpha = fav.geometricAlpha,
            onFavoriteToggle = { snapshot, n, s, o, g, na, sa, oa, ga ->
                val updatedFav = FavoriteWallpaper(
                    wallpaper = snapshot,
                    addNoise = n,
                    addStripes = s,
                    addOverlay = o,
                    addGeometric = g,
                    noiseAlpha = na,
                    stripesAlpha = sa,
                    overlayAlpha = oa,
                    geometricAlpha = ga
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
    if (showImportExportDialog) {

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showImportExportDialog = false }
        ) {

            val isDark =
                MaterialTheme.colorScheme.background.luminance() < 0.5f

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .border(
                        width = 3.dp,
                        color = if (isDark) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                        },
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {

                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = "Import / Export",
                        style = MaterialTheme.typography.titleMedium
                    )

                    FilledTonalButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            showImportExportDialog = false
                            importWallLauncher.launch(arrayOf("*/*"))
                        }
                    ) {
                        Text(stringResource(R.string.import_wall))
                    }

                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            showImportExportDialog = false
                            WallFileManager.shareFavorites(context, favourites)
                        }
                    ) {
                        Text(stringResource(R.string.export_favourites))
                    }

                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showImportExportDialog = false }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
    ApplyDownloadDialog(
        interactionMode = interactionMode,
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
        geometricAlpha = pendingClickedWallpaper?.geometricAlpha ?: 1f,
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
