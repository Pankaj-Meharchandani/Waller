package com.example.waller.ui.wallpaper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
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
        private var favorite: FavoriteWallpaper? = null
        private val prefs = getSharedPreferences("waller_prefs", Context.MODE_PRIVATE)

        private var cachedOverlay: Bitmap? = null
        private var cachedGeometric: Bitmap? = null

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
                        // Slowly animate the angle
                        currentAngle = (currentAngle + speed) % 360f
                        
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
