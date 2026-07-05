package com.example.waller.ui.marketplace

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
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
import com.example.waller.ui.wallpaper.FavoriteWallpaper
import com.example.waller.ui.wallpaper.Haptics
import com.example.waller.ui.wallpaper.components.WallpaperItemCard
import com.example.waller.ui.wallpaper.toHexString
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    favouriteWallpapers: List<FavoriteWallpaper>,
    onWallpaperSelected: (FavoriteWallpaper) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_market)) },
                actions = {
                    IconButton(onClick = { loadItems(refresh = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { loadItems(refresh = true) },
            state = refreshState,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            if (items.isEmpty() && !isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.market_empty))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp + 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxSize()
                ) {
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
                                isPortrait = true,
                                isFavorite = isFavorite,
                                onFavoriteToggle = { _, _ -> }, // Not needed for market browse
                                onClick = {
                                    Haptics.confirm(view)
                                    onWallpaperSelected(fav)
                                    Toast.makeText(context, context.getString(R.string.added_to_favourites), Toast.LENGTH_SHORT).show()
                                },
                                onLongClick = {
                                    Haptics.confirm(view)
                                    onWallpaperSelected(fav)
                                    Toast.makeText(context, context.getString(R.string.added_to_favourites), Toast.LENGTH_SHORT).show()
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
}
