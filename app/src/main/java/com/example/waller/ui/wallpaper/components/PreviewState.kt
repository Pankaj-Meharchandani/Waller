/**
 * PreviewState.kt
 * Small, focused constants and tiny state helpers used by WallpaperPreviewOverlay.
 * - Default effect alpha values and a small init helper.
 */

package com.example.waller.ui.wallpaper.components

// default alpha presets when enabling an effect
const val DEFAULT_NOISE_ALPHA = 1f
const val DEFAULT_STRIPES_ALPHA = 1f
const val DEFAULT_OVERLAY_ALPHA = 1f

fun initAlphaFor(enabled: Boolean, default: Float) = if (enabled) default else 0f
