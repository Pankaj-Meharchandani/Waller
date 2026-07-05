package com.example.waller.data.network

import android.graphics.Bitmap
import com.example.waller.ui.wallfile.WallFile
import com.example.waller.ui.wallfile.WallFavorite
import com.example.waller.ui.wallpaper.FavoriteWallpaper
import com.example.waller.ui.wallfile.toFavoriteWallpaper
import com.example.waller.ui.wallfile.toWallFavorite
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File

@Serializable
private data class TelegramResponse<T>(
    val ok: Boolean,
    val result: T? = null,
    val description: String? = null
)

@Serializable
private data class TelegramMessage(
    val message_id: Long,
    val document: TelegramDocument? = null,
    val text: String? = null
)

@Serializable
private data class TelegramDocument(
    val file_id: String,
    val file_name: String? = null
)

@Serializable
private data class TelegramFile(
    val file_id: String,
    val file_path: String? = null
)

object TelegramMarketplaceService {
    private const val BOT_TOKEN = "YOUR_BOT_TOKEN" // Placeholder
    private const val CHANNEL_ID = "@YOUR_CHANNEL_ID" // Placeholder
    private const val BASE_URL = "https://api.telegram.org/bot$BOT_TOKEN"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    /**
     * Uploads a wallpaper to the Telegram channel with a preview image.
     */
    suspend fun uploadWallpaper(fav: FavoriteWallpaper, previewBitmap: Bitmap): Result<Long> {
        return try {
            val wallFile = WallFile(walls = listOf(fav.toWallFavorite()))
            val jsonString = json.encodeToString(WallFile.serializer(), wallFile)
            
            // 1. Convert bitmap to byte array
            val stream = ByteArrayOutputStream()
            previewBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val previewBytes = stream.toByteArray()

            // 2. Send Photo first
            val photoResponse: TelegramResponse<TelegramMessage> = client.submitFormWithBinaryData(
                url = "$BASE_URL/sendPhoto",
                formData = formData {
                    append("chat_id", CHANNEL_ID)
                    append("photo", previewBytes, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"preview.jpg\"")
                        append(HttpHeaders.ContentType, "image/jpeg")
                    })
                    append("caption", "New wallpaper from Waller!")
                }
            ).body()

            if (!photoResponse.ok) return Result.failure(Exception(photoResponse.description))

            // 3. Send Document (the .wall file)
            val docResponse: TelegramResponse<TelegramMessage> = client.submitFormWithBinaryData(
                url = "$BASE_URL/sendDocument",
                formData = formData {
                    append("chat_id", CHANNEL_ID)
                    append("document", jsonString.toByteArray(), Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"waller_${System.currentTimeMillis()}.wall\"")
                        append(HttpHeaders.ContentType, "application/json")
                    })
                }
            ).body()

            if (docResponse.ok && docResponse.result != null) {
                Result.success(docResponse.result.message_id)
            } else {
                Result.failure(Exception(docResponse.description ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads a file from Telegram given its file_id.
     */
    suspend fun downloadWallFile(fileId: String): Result<FavoriteWallpaper> {
        return try {
            // 1. Get file path from Telegram
            val fileInfoResponse: TelegramResponse<TelegramFile> = client.get("$BASE_URL/getFile") {
                parameter("file_id", fileId)
            }.body()

            val filePath = fileInfoResponse.result?.file_path
                ?: return Result.failure(Exception("Could not get file path"))

            // 2. Download the actual file
            val fileUrl = "https://api.telegram.org/file/bot$BOT_TOKEN/$filePath"
            val jsonString = client.get(fileUrl).body<String>()

            val wallFile = json.decodeFromString(WallFile.serializer(), jsonString)
            val fav = wallFile.walls.firstOrNull()?.toFavoriteWallpaper()
                ?: return Result.failure(Exception("Empty wall file"))

            Result.success(fav)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads a wallpaper from a direct URL.
     */
    suspend fun downloadFromUrl(url: String): Result<FavoriteWallpaper> {
        return try {
            val jsonString = client.get(url).body<String>()
            val wallFile = json.decodeFromString(WallFile.serializer(), jsonString)
            val fav = wallFile.walls.firstOrNull()?.toFavoriteWallpaper()
                ?: return Result.failure(Exception("Empty wall file"))
            Result.success(fav)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * This is the "Registry" approach:
     * We read a pinned message that contains a list of Message IDs or File IDs.
     */
    suspend fun fetchMarketplaceItems(registryMessageId: Long): Result<List<String>> {
        return try {
            // Note: bots can't easily get a message by ID unless they just sent it or it's in updates.
            // But we can use getChat to get the pinned message ID.
            Result.failure(Exception("Registry logic needed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
