package com.example.waller.ui.wallpaper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.SurfaceHolder
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.example.waller.R
import com.example.waller.ui.wallfile.WallFavorite
import com.example.waller.ui.wallfile.toFavoriteWallpaper
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

class LiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return LiveEngine()
    }

    inner class LiveEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private var visible = false
        private var currentAngle = 0f
        private var speed = 0.05f
        private var lastTickTime = 0L
        private var favorite: FavoriteWallpaper? = null
        private val prefs = getSharedPreferences("waller_prefs", Context.MODE_PRIVATE)

        private var cachedOverlay: Bitmap? = null
        private var cachedGeometric: Bitmap? = null
        
        private var offscreenBitmap: Bitmap? = null
        private var offscreenCanvas: Canvas? = null

        private val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (visible) {
                    draw()
                    Choreographer.getInstance().postFrameCallback(this)
                }
            }
        }

        init {
            loadConfig()
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        private fun loadConfig() {
            val json = prefs.getString("live_wallpaper_config", null)
            speed = prefs.getFloat("live_wallpaper_speed", 0.05f)
            favorite = if (json != null) {
                try {
                    Json.decodeFromString<WallFavorite>(json).toFavoriteWallpaper()
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            favorite?.let {
                currentAngle = it.wallpaper.angleDeg
                cachedOverlay?.recycle()
                cachedOverlay = null
                cachedGeometric?.recycle()
                cachedGeometric = null
            }
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key == "live_wallpaper_config" || key == "live_wallpaper_speed") {
                loadConfig()
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                lastTickTime = System.currentTimeMillis()
                Choreographer.getInstance().postFrameCallback(frameCallback)
            } else {
                Choreographer.getInstance().removeFrameCallback(frameCallback)
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            cachedOverlay?.recycle()
            cachedOverlay = null
            cachedGeometric?.recycle()
            cachedGeometric = null
            offscreenBitmap?.recycle()
            offscreenBitmap = null
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            this.visible = false
            Choreographer.getInstance().removeFrameCallback(frameCallback)
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                val fav = favorite ?: return
                
                val blurState = fav.effects["blur"]
                val hasBlur = blurState != null && blurState.enabled && blurState.alpha > 0.01f

                // Use regular lockCanvas for blur operations to ensure consistency, 
                // hardware canvas for standard rendering.
                canvas = if (hasBlur) holder.lockCanvas() else holder.lockHardwareCanvas()
                
                if (canvas != null) {
                    // Animation logic
                    val now = System.currentTimeMillis()
                    if (lastTickTime == 0L) lastTickTime = now
                    val deltaSeconds = (now - lastTickTime) / 1000f
                    lastTickTime = now

                    val degPerSec = speed * 360f
                    currentAngle = (currentAngle + degPerSec * deltaSeconds) % 360f
                    
                    val wallpaper = fav.wallpaper.copy(angleDeg = currentAngle)

                    // Resource caching
                    if (fav.effects.isEnabled("overlay") && cachedOverlay == null) {
                        val raw = android.graphics.BitmapFactory.decodeResource(resources, R.drawable.overlay_stripes)
                        cachedOverlay = raw.scale(canvas.width, canvas.height)
                        raw.recycle()
                    }
                    if (fav.effects.isEnabled("geometric") && cachedGeometric == null) {
                        val raw = android.graphics.BitmapFactory.decodeResource(resources, R.drawable.overlay_geometric)
                        val scale = canvas.width.toFloat() / raw.width.toFloat()
                        val sw = canvas.width
                        val sh = (raw.height * scale).roundToInt()
                        cachedGeometric = raw.scale(sw, sh)
                        raw.recycle()
                    }

                    if (hasBlur) {
                        // High-reliability Software Blur Path
                        val scaleDown = 4
                        val sw = (canvas.width / scaleDown).coerceAtLeast(1)
                        val sh = (canvas.height / scaleDown).coerceAtLeast(1)
                        
                        if (offscreenBitmap == null || offscreenBitmap?.width != sw || offscreenBitmap?.height != sh) {
                            offscreenBitmap?.recycle()
                            offscreenBitmap = createBitmap(sw, sh)
                            offscreenCanvas = Canvas(offscreenBitmap!!)
                        }
                        
                        offscreenCanvas?.let { osCanvas ->
                            drawWallpaperOnCanvas(
                                context         = this@LiveWallpaperService,
                                canvas          = osCanvas,
                                width           = sw,
                                height          = sh,
                                wallpaper       = wallpaper,
                                effects         = fav.effects,
                                cachedOverlay   = cachedOverlay,
                                cachedGeometric = cachedGeometric
                            )
                            
                            val radius = (25f * blurState!!.alpha / scaleDown).coerceIn(1f, 25f).toInt()
                            val blurred = stackBlur(offscreenBitmap!!, radius)
                            canvas.drawBitmap(blurred, null, Rect(0, 0, canvas.width, canvas.height), Paint(Paint.FILTER_BITMAP_FLAG))
                            blurred.recycle()
                        }
                    } else {
                        // High-performance Standard Path
                        drawWallpaperOnCanvas(
                            context         = this@LiveWallpaperService,
                            canvas          = canvas,
                            width           = canvas.width,
                            height          = canvas.height,
                            wallpaper       = wallpaper,
                            effects         = fav.effects,
                            cachedOverlay   = cachedOverlay,
                            cachedGeometric = cachedGeometric
                        )
                    }
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
}
