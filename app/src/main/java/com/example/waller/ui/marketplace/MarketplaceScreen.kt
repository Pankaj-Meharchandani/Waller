package com.example.waller.ui.marketplace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.waller.data.network.MarketplaceItem
import com.example.waller.data.network.TelegramScraper
import com.example.waller.data.network.TelegramMarketplaceService
import com.example.waller.ui.wallpaper.FavoriteWallpaper
import com.example.waller.ui.wallpaper.Haptics
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    onWallpaperSelected: (FavoriteWallpaper) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val items = remember { mutableStateListOf<MarketplaceItem>() }
    var isLoading by remember { mutableStateOf(false) }
    var downloadingItem by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    val view = androidx.compose.ui.platform.LocalView.current

    LaunchedEffect(Unit) {
        if (items.isEmpty()) {
            isLoading = true
            items.addAll(TelegramScraper.fetchItems())
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Marketplace") },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        if (items.isEmpty() && !isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(androidx.compose.ui.res.stringResource(com.example.waller.R.string.market_empty))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(items) { item ->
                    MarketplaceItemCard(
                        item = item,
                        isDownloading = downloadingItem == item.messageId,
                        onClick = {
                            if (downloadingItem == null) {
                                Haptics.confirm(view)
                                if (item.wallpaper != null) {
                                    onWallpaperSelected(item.wallpaper)
                                } else if (item.wallFileUrl.isNotEmpty()) {
                                    // Fallback for older items with separate file uploads
                                    downloadingItem = item.messageId
                                    scope.launch {
                                        val result = TelegramMarketplaceService.downloadFromUrl(item.wallFileUrl)
                                        downloadingItem = null
                                        if (result.isSuccess) {
                                            onWallpaperSelected(result.getOrThrow())
                                        } else {
                                            android.widget.Toast.makeText(context, "Download failed", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MarketplaceItemCard(
    item: MarketplaceItem,
    isDownloading: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.aspectRatio(0.7f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.previewImageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(32.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(topStart = 16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = "Download",
                        modifier = Modifier.padding(8.dp).size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
