/**
 * Favourites screen for Waller.
 * Shows only the wallpapers the user has marked with a heart.
 * Uses the stored EffectMap snapshot from FavoriteWallpaper.
 * Uses shared orientation state from WallerApp and lets user toggle it via the Header chip.
 *
 * Adding a new effect requires NO change here.
 */

package com.example.waller.ui.wallpaper

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
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
    onAddFavourites: (List<FavoriteWallpaper>) -> Unit,
    interactionMode: InteractionMode
) {
    val context        = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gridState      = rememberLazyGridState()

    // ── Import launcher ───────────────────────────────────────────────────────
    val importWallLauncher = rememberLauncherForActivityResult(
        object : ActivityResultContracts.OpenMultipleDocuments() {
            override fun createIntent(context: Context, input: Array<String>): Intent =
                super.createIntent(context, input).apply {
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream", "application/*", "*/*"))
                }
        }
    ) { uris ->
        // Resolve display name from ContentResolver to reliably detect .wall files,
        // because uri.lastPathSegment is often an opaque id on content:// URIs.
        fun isWallUri(uri: android.net.Uri): Boolean {
            val name = try {
                context.contentResolver
                    .query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }
            } catch (_: Exception) { null }
            // Fall back to last path segment if cursor gave nothing
            val fileName = name ?: uri.lastPathSegment ?: uri.toString()
            return fileName.endsWith(".wall", ignoreCase = true)
        }

        val wallUris    = uris.filter { isWallUri(it) }
        val nonWallCount = uris.size - wallUris.size

        // If nothing selected was a .wall file show a clear error and stop
        if (wallUris.isEmpty()) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.error_unsupported_file),
                android.widget.Toast.LENGTH_LONG
            ).show()
            return@rememberLauncherForActivityResult
        }

        val importedKeys = mutableSetOf<String>()
        val toAdd        = mutableListOf<FavoriteWallpaper>()

        wallUris.forEach { uri ->
            WallFileManager.importWallFile(context, uri)?.forEach { fav ->
                val key = "${fav.wallpaper.type}_${fav.wallpaper.angleDeg}_" +
                        "${fav.wallpaper.colors.joinToString()}_${fav.effects}"
                val alreadyExistsInApp = favourites.any { existing ->
                    existing.wallpaper == fav.wallpaper && existing.effects == fav.effects
                }
                if (!alreadyExistsInApp && !importedKeys.contains(key)) {
                    importedKeys.add(key)
                    toAdd.add(fav)
                }
            }
        }

        // One atomic state write for all imported wallpapers
        val importedCount = toAdd.size
        if (toAdd.isNotEmpty()) onAddFavourites(toAdd)

        val message = buildString {
            when {
                importedCount == 0 -> append(context.getString(R.string.wallpaper_already_exists))
                importedCount == 1 -> append(context.getString(R.string._1_wallpaper_imported))
                else -> append(context.getString(R.string.wallpapers_imported, importedCount))
            }
            if (nonWallCount > 0) append(context.getString(R.string.non_wall_skipped, nonWallCount))
        }

        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    val writePermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) android.widget.Toast.makeText(
                context, context.getString(R.string.storage_permission_denied), android.widget.Toast.LENGTH_SHORT
            ).show()
        }

    var pendingClickedWallpaper by remember { mutableStateOf<FavoriteWallpaper?>(null) }
    var showApplyDialog       by remember { mutableStateOf(false) }
    var isWorking             by remember { mutableStateOf(false) }
    var showPreview           by remember { mutableStateOf(false) }
    var showImportExportDialog by remember { mutableStateOf(false) }

    val spanCount = if (isPortrait) 2 else 1
    val columns   = GridCells.Fixed(spanCount)

    // ── Grid ──────────────────────────────────────────────────────────────────
    LazyVerticalGrid(
        columns = columns,
        state = gridState,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp + 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item(span = { GridItemSpan(spanCount) }) {
            Header(
                onThemeChange = onThemeChange,
                isAppDarkMode = isAppDarkMode,
                showOrientationToggle = true,
                isPortrait = isPortrait,
                onOrientationChange = onOrientationChange
            )
        }

        item(span = { GridItemSpan(spanCount) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (favourites.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(text = stringResource(R.string.favourites_empty), style = MaterialTheme.typography.titleMedium)
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .premiumAddColorBorder(isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f)
                                .clickable { importWallLauncher.launch(arrayOf("*/*")) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = stringResource(R.string.import_wall),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.favourites_count, favourites.size),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .premiumAddColorBorder(isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f)
                        .clickable { showImportExportDialog = true }
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

        // Favourites cards
        items(favourites.asReversed()) { fav ->
            WallpaperItemCard(
                wallpaper        = fav.wallpaper,
                isPortrait       = isPortrait,
                effects          = fav.effects,
                isFavorite       = true,
                onFavoriteToggle = { _, _ -> onRemoveFavourite(fav) },
                onClick = {
                    when (interactionMode) {
                        InteractionMode.SIMPLE   -> { pendingClickedWallpaper = fav; showApplyDialog = true }
                        InteractionMode.ADVANCED -> { pendingClickedWallpaper = fav; showPreview = true }
                    }
                },
                onLongClick = { pendingClickedWallpaper = fav; showApplyDialog = true }
            )
        }

        if (favourites.isNotEmpty()) {
            item(span = { GridItemSpan(spanCount) }) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedButton(
                        shape = RoundedCornerShape(14.dp),
                        onClick = { coroutineScope.launch { gridState.animateScrollToItem(1) } },
                        modifier = Modifier.fillMaxWidth(0.6f).height(44.dp)
                    ) {
                        Text(stringResource(R.string.scroll_to_top))
                    }
                }
            }
        }
    }

    // ── Preview overlay ───────────────────────────────────────────────────────
    if (showPreview && pendingClickedWallpaper != null) {
        val fav = pendingClickedWallpaper!!
        WallpaperPreviewOverlay(
            wallpaper        = fav.wallpaper,
            isPortrait       = isPortrait,
            isFavorite       = true,
            initialEffects   = fav.effects,
            onFavoriteToggle = { snapshot, fx ->
                val updatedFav = FavoriteWallpaper(wallpaper = snapshot, effects = fx)
                onRemoveFavourite(fav)
                onAddFavourite(updatedFav)
                pendingClickedWallpaper = updatedFav
            },
            onDismiss        = { showPreview = false; pendingClickedWallpaper = null },
            writePermissionLauncher = writePermissionLauncher,
            context          = context,
            coroutineScope   = coroutineScope
        )
    }

    // ── Import / Export dialog ────────────────────────────────────────────────
    if (showImportExportDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showImportExportDialog = false }) {
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .border(
                        width = 3.dp,
                        color = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        else        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Import / Export", style = MaterialTheme.typography.titleMedium)

                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { showImportExportDialog = false; importWallLauncher.launch(arrayOf("*/*")) }
                    ) { Text(stringResource(R.string.import_wall)) }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { showImportExportDialog = false; WallFileManager.shareFavorites(context, favourites) }
                    ) { Text(stringResource(R.string.export_favourites)) }

                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showImportExportDialog = false }
                    ) { Text(stringResource(R.string.cancel)) }
                }
            }
        }
    }

    // ── Apply dialog ──────────────────────────────────────────────────────────
    ApplyDownloadDialog(
        interactionMode  = interactionMode,
        show             = showApplyDialog,
        wallpaper        = pendingClickedWallpaper?.wallpaper,
        isPortrait       = isPortrait,
        effects          = pendingClickedWallpaper?.effects ?: WallpaperEffects.defaultMap(),
        isWorking        = isWorking,
        onWorkingChange  = { isWorking = it },
        onDismiss        = { showApplyDialog = false; pendingClickedWallpaper = null },
        writePermissionLauncher = writePermissionLauncher,
        context          = context,
        coroutineScope   = coroutineScope
    )
}