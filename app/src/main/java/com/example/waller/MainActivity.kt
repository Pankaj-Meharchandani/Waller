/**
 * Host Activity for the Compose UI.
 *
 * Responsibilities:
 * - Sets the Compose content tree (WallerApp)
 * - Bridges SimpleColorDialog (Android View system) with Compose
 * - Provides callbacks to set selected colors
 *
 * No UI logic here — only platform integration.
 */

package com.example.waller

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.waller.ui.WallerApp
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.color.SimpleColorDialog

class MainActivity : FragmentActivity(), SimpleDialog.OnDialogResultListener {

    companion object {
        private const val COLOR_DIALOG_TAG = "COLOR_DIALOG"
    }

    private var pendingColorCallback: ((Int?) -> Unit)? = null

    fun openColorDialog(initialColor: Int?, callback: (Int?) -> Unit) {
        pendingColorCallback = callback

        val builder = SimpleColorDialog.build()
            .allowCustom(true)
            .neg(R.string.cancel)

        if (initialColor != null) {
            builder.colorPreset(initialColor)
        }

        builder.show(this, COLOR_DIALOG_TAG)
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (dialogTag == COLOR_DIALOG_TAG) {
            val callback = pendingColorCallback
            pendingColorCallback = null

            if (callback != null) {
                if (which == SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE) {
                    val colorInt = extras.getInt(SimpleColorDialog.COLOR)
                    callback(colorInt)
                } else {
                    callback(null)
                }
            }
            return true
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WallerApp()
        }
    }
}