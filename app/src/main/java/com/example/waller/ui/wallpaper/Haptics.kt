package com.example.waller.ui.wallpaper

import android.view.HapticFeedbackConstants
import android.view.View

object Haptics {
    var enabled: Boolean = true

    fun light(view: View) {
        if (enabled) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun confirm(view: View) {
        if (enabled) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    fun longPress(view: View) {
        if (enabled) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}
