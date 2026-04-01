package com.example.waller.ui.wallpaper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
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

        private val handler = Handler(Looper.getMainLooper())
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
        private val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        private val drawRunnable = Runnable { draw() }

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
                draw()
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawRunnable)
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            cachedOverlay?.recycle()
            cachedGeometric?.recycle()
            offscreenBitmap?.recycle()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            this.visible = false
            handler.removeCallbacks(drawRunnable)
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    val fav = favorite
                    if (fav != null) {
                        // Animation logic matching preview
                        val now = System.currentTimeMillis()
                        if (lastTickTime == 0L) lastTickTime = now
                        val deltaSeconds = (now - lastTickTime) / 1000f
                        lastTickTime = now

                        val degPerSec = speed * 360f
                        currentAngle = (currentAngle + degPerSec * deltaSeconds) % 360f
                        
                        val wallpaper = fav.wallpaper.copy(angleDeg = currentAngle)

                        // Ensure cache is ready
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

                        val blurState = fav.effects["blur"]
                        val hasBlur = blurState != null && blurState.enabled && blurState.alpha > 0f

                        if (hasBlur) {
                            // Render to offscreen first to apply blur
                            if (offscreenBitmap == null || offscreenBitmap?.width != canvas.width || offscreenBitmap?.height != canvas.height) {
                                offscreenBitmap = createBitmap(canvas.width, canvas.height)
                                offscreenCanvas = Canvas(offscreenBitmap!!)
                            }
                            
                            offscreenCanvas?.let { osCanvas ->
                                drawWallpaperOnCanvas(
                                    context         = this@LiveWallpaperService,
                                    canvas          = osCanvas,
                                    width           = osCanvas.width,
                                    height          = osCanvas.height,
                                    wallpaper       = wallpaper,
                                    effects         = fav.effects,
                                    cachedOverlay   = cachedOverlay,
                                    cachedGeometric = cachedGeometric
                                )

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val radius = 22f * blurState.alpha
                                    // Use reflection to set render effect to avoid potential compilation issues with unresolved references
                                    try {
                                        val effect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
                                        blurPaint.javaClass.getMethod("setRenderEffect", RenderEffect::class.java).invoke(blurPaint, effect)
                                        canvas.drawBitmap(offscreenBitmap!!, 0f, 0f, blurPaint)
                                    } catch (e: Exception) {
                                        val r = (25f * blurState.alpha).coerceIn(1f, 25f).toInt()
                                        val blurred = stackBlur(offscreenBitmap!!, r)
                                        canvas.drawBitmap(blurred, 0f, 0f, null)
                                        blurred.recycle()
                                    }
                                } else {
                                    // Fallback for older Android: using stackBlur might be slow but it's consistent
                                    val radius = (25f * blurState.alpha).coerceIn(1f, 25f).toInt()
                                    val blurred = stackBlur(offscreenBitmap!!, radius)
                                    canvas.drawBitmap(blurred, 0f, 0f, null)
                                    blurred.recycle()
                                }
                            }
                        } else {
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
                    } else {
                        canvas.drawColor(android.graphics.Color.BLACK)
                    }
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }

            handler.removeCallbacks(drawRunnable)
            if (visible) {
                handler.postDelayed(drawRunnable, 32) // ~30 FPS
            }
        }
    }
}
