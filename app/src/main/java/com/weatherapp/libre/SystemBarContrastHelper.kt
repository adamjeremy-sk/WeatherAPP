package com.weatherapp.libre

import android.app.Activity
import android.graphics.Color
import androidx.core.view.WindowCompat
import kotlin.math.pow

object SystemBarContrastHelper {

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
