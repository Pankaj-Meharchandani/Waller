/**
 * PreviewState.kt
 *
 * Small constants and helper functions used by the preview feature.
 *
 * Responsibilities:
 * - Defines default alpha values for preview effects
 * - Provides lightweight helpers for initializing effect state
 */

package com.example.waller.ui.wallpaper.components.previewOverlay

// default alpha presets when enabling an effect
const val DEFAULT_NOISE_ALPHA = 1f
const val DEFAULT_STRIPES_ALPHA = 1f
const val DEFAULT_OVERLAY_ALPHA = 1f

const val DEFAULT_GEOMETRIC_ALPHA = 1f

fun initAlphaFor(enabled: Boolean, default: Float) = if (enabled) default else 0f
