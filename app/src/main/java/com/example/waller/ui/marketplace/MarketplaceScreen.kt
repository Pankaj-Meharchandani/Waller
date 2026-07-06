package com.example.waller.ui.marketplace

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.waller.R
import com.example.waller.data.network.MarketplaceItem
import com.example.waller.data.network.TelegramScraper
import com.example.waller.ui.wallpaper.*
import com.example.waller.ui.wallpaper.components.Header
import com.example.waller.ui.wallpaper.components.WallpaperItemCard
import com.example.waller.ui.wallpaper.components.previewOverlay.WallpaperPreviewOverlay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    favouriteWallpapers: List<FavoriteWallpaper>,
    onToggleFavorite: (FavoriteWallpaper) -> Unit,
    isPortrait: Boolean,
    onOrientationChange: (Boolean) -> Unit,
    isAppDarkMode: Boolean,
    onThemeChange: () -> Unit,
    interactionMode: InteractionMode,
    onPreviewVisibilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val items = remember { mutableStateListOf<MarketplaceItem>() }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var canLoadMore by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val view = androidx.compose.ui.platform.LocalView.current
    val gridState = rememberLazyGridState()

    val refreshState = rememberPullToRefreshState()

    fun loadItems(refresh: Boolean = false) {
        if (isLoading || isRefreshing) return
        scope.launch {
            if (refresh) {
                isRefreshing = true
                canLoadMore = true
            } else {
                isLoading = true
            }

            val beforeId = if (refresh) null else items.lastOrNull()?.messageId
            val fetched = TelegramScraper.fetchItems(beforeId)

            if (refresh) items.clear()
            
            if (fetched.isEmpty()) {
                canLoadMore = false
            } else {
                items.addAll(fetched)
                if (fetched.size < 15) canLoadMore = false // Telegram typically returns 20, if less we are likely at the end
            }

            if (refresh) isRefreshing = false else isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (items.isEmpty()) {
            loadItems()
        }
    }

    // Infinite scroll logic
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo }
            .filter { it.isNotEmpty() }
            .map { it.last().index }
            .distinctUntilChanged()
            .collect { lastIndex ->
                // Load more when user reaches index (size - 6) i.e. around 14 if batch is 20
                if (canLoadMore && !isLoading && !isRefreshing && lastIndex >= items.size - 6 && items.isNotEmpty()) {
                    loadItems()
                }
            }
    }

    val writePermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) android.widget.Toast.makeText(
                context, R.string.storage_permission_denied, android.widget.Toast.LENGTH_SHORT
            ).show()
        }

    var pendingClickedWallpaper by remember { mutableStateOf<FavoriteWallpaper?>(null) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }

    val spanCount = if (isPortrait) 2 else 1

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { loadItems(refresh = true) },
        state = refreshState,
        modifier = modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(spanCount),
            state = gridState,
            contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp + 96.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxSize()
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.market_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (items.isEmpty() && !isLoading) {
                item(span = { GridItemSpan(spanCount) }) {
                    Box(Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.market_empty))
                    }
                }
            } else {
                items(items, key = { it.messageId }) { item ->
                    val fav = item.wallpaper
                    if (fav != null) {
                        val isFavorite = favouriteWallpapers.any { existing ->
                            existing.wallpaper.type == fav.wallpaper.type &&
                                    existing.wallpaper.angleDeg.compareTo(fav.wallpaper.angleDeg) == 0 &&
                                    existing.wallpaper.colors.size == fav.wallpaper.colors.size &&
                                    existing.wallpaper.colors.zip(fav.wallpaper.colors).all { (x, y) -> x.toHexString() == y.toHexString() } &&
                                    existing.effects == fav.effects
                        }

                        WallpaperItemCard(
                            wallpaper = fav.wallpaper,
                            effects = fav.effects,
                            isPortrait = isPortrait,
                            isFavorite = isFavorite,
                            onFavoriteToggle = { _, _ ->
                                Haptics.confirm(view)
                                onToggleFavorite(fav)
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
                                        onPreviewVisibilityChanged(true)
                                    }
                                }
                            },
                            onLongClick = {
                                pendingClickedWallpaper = fav
                                showApplyDialog = true
                            }
                        )
                    }
                }
            }
            
            if (isLoading && items.isNotEmpty()) {
                item(span = { GridItemSpan(spanCount) }) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
            }
        }

        if (isLoading && items.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }

    // ── Preview overlay ───────────────────────────────────────────────────────
    if (showPreview && pendingClickedWallpaper != null) {
        val fav = pendingClickedWallpaper!!
        val isFav = favouriteWallpapers.any { existing ->
            existing.wallpaper.type == fav.wallpaper.type &&
                    existing.wallpaper.angleDeg.compareTo(fav.wallpaper.angleDeg) == 0 &&
                    existing.wallpaper.colors.size == fav.wallpaper.colors.size &&
                    existing.wallpaper.colors.zip(fav.wallpaper.colors).all { (x, y) -> x.toHexString() == y.toHexString() } &&
                    existing.effects == fav.effects
        }
        WallpaperPreviewOverlay(
            wallpaper = fav.wallpaper,
            isPortrait = isPortrait,
            onOrientationChange = onOrientationChange,
            isFavorite = isFav,
            initialEffects = fav.effects,
            onFavoriteToggle = { snapshot, fx ->
                onToggleFavorite(FavoriteWallpaper(wallpaper = snapshot, effects = fx))
            },
            onDismiss = {
                showPreview = false
                pendingClickedWallpaper = null
                onPreviewVisibilityChanged(false)
            },
            writePermissionLauncher = writePermissionLauncher,
            context = context,
            coroutineScope = scope
        )
    }

    // ── Apply dialog ──────────────────────────────────────────────────────────
    ApplyDownloadDialog(
        interactionMode = interactionMode,
        show = showApplyDialog,
        wallpaper = pendingClickedWallpaper?.wallpaper,
        isPortrait = isPortrait,
        effects = pendingClickedWallpaper?.effects ?: WallpaperEffects.defaultMap(),
        isWorking = isWorking,
        onWorkingChange = { isWorking = it },
        onDismiss = {
            showApplyDialog = false
            pendingClickedWallpaper = null
        },
        writePermissionLauncher = writePermissionLauncher,
        context = context,
        coroutineScope = scope
    )
}
