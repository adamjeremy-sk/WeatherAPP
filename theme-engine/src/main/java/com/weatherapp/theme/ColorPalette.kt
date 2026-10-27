package com.weatherapp.theme

import android.content.Context
import android.graphics.Color

data class ColorPalette(
    val primary: Int,
    val surfaceCard: Int,
    val surfaceCardSecondary: Int,
    val backgroundLight: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val tempHigh: Int,
    val tempHighBg: Int,
    val tempLow: Int,
    val tempLowBg: Int,
    val goldenHourPrimary: Int,
    val blueHourPrimary: Int,
    val dividerColor: Int,
    val alertExtreme: Int = Color.parseColor("#DC2626"),
    val alertSevere: Int = Color.parseColor("#EA580C"),
    val alertModerate: Int = Color.parseColor("#92400E"),
    val alertMinor: Int = Color.parseColor("#0369A1"),
    val precipitation: Int = Color.parseColor("#0288D1"),
    val precipitationBg: Int = Color.parseColor("#E1F5FE")
) {
    val background: Int get() = backgroundLight
    val brandPrimary: Int get() = primary

    companion object {
        fun forMode(themeMode: Int, context: Context? = null): ColorPalette {
            val effectiveMode = context?.let {
                val nightMode = it.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                if (themeMode == ThemeMode.SYSTEM) {
                    if (nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) ThemeMode.DARK else ThemeMode.LIGHT
                } else {
                    themeMode
                }
            } ?: themeMode
            return BoutiqueThemeEngine.getThemePalette(effectiveMode)
        }
    }
}
