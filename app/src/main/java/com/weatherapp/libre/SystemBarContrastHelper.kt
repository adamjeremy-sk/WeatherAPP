package com.weatherapp.libre

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.pow

object SystemBarContrastHelper {

    @JvmStatic
    fun applyDialogEdgeToEdge(dialog: Dialog) {
        // Defensive guard: Edge-to-edge system bar insets corrupt the measure/positioning 
        // of wrap_content floating dialogs. Only apply to fullscreen dialogs.
        if (dialog is androidx.appcompat.app.AlertDialog ||
            dialog is android.app.AlertDialog ||
            dialog is com.google.android.material.bottomsheet.BottomSheetDialog ||
            dialog is android.app.DatePickerDialog) {
            return
        }

        val window = dialog.window ?: return
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }
    }

    fun updateSystemBarContrast(activity: Activity, backgroundColor: Int) {
        val window = activity.window ?: return
        val controller = WindowCompat.getInsetsController(window, window.decorView)

        val isLight = isLuminanceHigh(backgroundColor)
        controller.isAppearanceLightStatusBars = isLight
        controller.isAppearanceLightNavigationBars = isLight
    }

    fun isLuminanceHigh(color: Int): Boolean {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0

        val rs = if (r <= 0.03928) r / 12.92 else ((r + 0.055) / 1.055).pow(2.4)
        val gs = if (g <= 0.03928) g / 12.92 else ((g + 0.055) / 1.055).pow(2.4)
        val bs = if (b <= 0.03928) b / 12.92 else ((b + 0.055) / 1.055).pow(2.4)

        val luminance = (0.2126 * rs) + (0.7152 * gs) + (0.0722 * bs)
        return luminance > 0.4
    }
}
