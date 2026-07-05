package com.example.waller.ui.marketplace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.waller.R
import com.example.waller.data.network.MarketplaceItem
import com.example.waller.data.network.TelegramScraper
import com.example.waller.ui.wallpaper.FavoriteWallpaper
import com.example.waller.ui.wallpaper.Haptics
import com.example.waller.ui.wallpaper.components.Header
import com.example.waller.ui.wallpaper.components.WallpaperItemCard
import com.example.waller.ui.wallpaper.toHexString
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
    modifier: Modifier = Modifier
) {
    val items = remember { mutableStateListOf<MarketplaceItem>() }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val view = androidx.compose.ui.platform.LocalView.current

    val refreshState = rememberPullToRefreshState()

    fun loadItems(refresh: Boolean = false) {
        scope.launch {
            if (refresh) isRefreshing = true else isLoading = true
            val fetched = TelegramScraper.fetchItems()
            if (refresh) items.clear()
            items.addAll(fetched)
            if (refresh) isRefreshing = false else isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (items.isEmpty()) {
            loadItems()
        }
    }

    val spanCount = if (isPortrait) 2 else 1

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { loadItems(refresh = true) },
        state = refreshState,
        modifier = modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(spanCount),
            contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp + 96.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxSize()
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(spanCount) }) {
                Header(
                    onThemeChange = onThemeChange,
                    isAppDarkMode = isAppDarkMode,
                    showOrientationToggle = true,
                    isPortrait = isPortrait,
                    onOrientationChange = onOrientationChange
                )
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(spanCount) }) {
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
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(spanCount) }) {
                    Box(Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.market_empty))
                    }
                }
            } else {
                items(items) { item ->
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
                                Haptics.confirm(view)
                                onToggleFavorite(fav)
                            },
                            onLongClick = {
                                Haptics.confirm(view)
                                onToggleFavorite(fav)
                            }
                        )
                    }
                }
            }
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}
