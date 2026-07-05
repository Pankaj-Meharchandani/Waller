package com.example.waller.data.network

import com.example.waller.ui.wallfile.WallFile
import com.example.waller.ui.wallfile.toFavoriteWallpaper
import com.example.waller.ui.wallpaper.FavoriteWallpaper
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json

data class MarketplaceItem(
    val messageId: Long,
    val wallFileUrl: String,
    val previewImageUrl: String? = null,
    val date: String? = null,
    val wallpaper: FavoriteWallpaper? = null
)

object TelegramScraper {
    private val client = HttpClient(OkHttp)
    private const val CHANNEL_NAME = "waller_wallpapers" 

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    /**
     * Scrapes the public Telegram channel preview for marketplace items.
     * @param beforeMessageId For pagination, fetch messages older than this.
     */
    suspend fun fetchItems(beforeMessageId: Long? = null): List<MarketplaceItem> {
        return try {
            val url = "https://t.me/s/$CHANNEL_NAME" + if (beforeMessageId != null) "?before=$beforeMessageId" else ""
            val html = client.get(url).bodyAsText()
            
            parseHtml(html)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseHtml(html: String): List<MarketplaceItem> {
        val items = mutableListOf<MarketplaceItem>()
        
        // Telegram's public preview HTML structure:
        // Message wraps: tgme_widget_message_wrap
        // Message ID: data-post="waller_wallpapers/123"
        // Text: <div class="tgme_widget_message_text js-message_text" dir="auto">...</div>
        // Photo: background-image:url('...')
        
        val messageBlocks = html.split("tgme_widget_message_wrap")
        val messageIdRegex = """data-post="[^"]+/(\d+)"""".toRegex()
        val textRegex = """<div class="tgme_widget_message_text[^>]*>(.*?)</div>""".toRegex()
        val photoRegex = """background-image:url\('([^']+)'\)""".toRegex()

        for (block in messageBlocks) {
            val idMatch = messageIdRegex.find(block) ?: continue
            val id = idMatch.groupValues[1].toLong()
            
            val textMatch = textRegex.find(block)
            val text = textMatch?.groupValues?.get(1) ?: ""
            
            // Clean HTML tags and entities
            val cleanText = text.replace("<br/>", "\n")
                .replace("(<[^>]+>)".toRegex(), "") // remove other tags
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")

            var wallpaper: FavoriteWallpaper? = null
            if (cleanText.contains("[WallerData]")) {
                try {
                    val jsonPart = cleanText.substringAfter("[WallerData]").trim()
                    val wallFile = json.decodeFromString(WallFile.serializer(), jsonPart)
                    wallpaper = wallFile.walls.firstOrNull()?.toFavoriteWallpaper()
                } catch (e: Exception) {
                    // Log or handle parsing error
                }
            }

            // Skip messages that don't have wallpaper data
            if (wallpaper == null) continue

            val photoMatch = photoRegex.find(block)
            val previewUrl = photoMatch?.groupValues?.get(1)

            items.add(MarketplaceItem(
                messageId = id,
                wallFileUrl = "", // Deprecated, using caption data
                previewImageUrl = previewUrl,
                wallpaper = wallpaper
            ))
        }
        
        // Return items newest first (LIFO)
        return items.sortedByDescending { it.messageId }
    }
}
