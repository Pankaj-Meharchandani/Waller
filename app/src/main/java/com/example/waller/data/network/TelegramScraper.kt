package com.example.waller.data.network

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable

@Serializable
data class MarketplaceItem(
    val messageId: Long,
    val wallFileUrl: String,
    val previewImageUrl: String? = null,
    val date: String? = null
)

object TelegramScraper {
    private val client = HttpClient(OkHttp)
    private const val CHANNEL_NAME = "waller_wallpapers" 

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
        
        // Very basic regex-based parsing for MVP. 
        // In a real app, use a proper HTML parser like Jsoup.
        // We look for message wraps and extract document links and photo backgrounds.
        
        val messageRegex = """data-post="[^"]+/(\d+)"""".toRegex()
        val docRegex = """href="([^"]+\.wall)"""".toRegex()
        val photoRegex = """background-image:url\('([^']+)'\)""".toRegex()
        
        // This is a simplified approach. Telegram's HTML is complex.
        // A better way would be to split by message blocks.
        
        val messageBlocks = html.split("tgme_widget_message_wrap")
        for (block in messageBlocks) {
            val idMatch = messageRegex.find(block)
            val docMatch = docRegex.find(block)
            val photoMatch = photoRegex.find(block)
            
            if (idMatch != null && docMatch != null) {
                items.add(MarketplaceItem(
                    messageId = idMatch.groupValues[1].toLong(),
                    wallFileUrl = docMatch.groupValues[1],
                    previewImageUrl = photoMatch?.groupValues[1]
                ))
            }
        }
        
        return items.sortedByDescending { it.messageId }
    }
}
