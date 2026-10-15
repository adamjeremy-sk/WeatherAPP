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
    val alertMinor: Int = Color.parseColor("#0369A1")
) {
    val background: Int get() = backgroundLight
    val brandPrimary: Int get() = primary

    companion object {
        fun forMode(themeMode: Int, context: Context? = null): ColorPalette {
            return BoutiqueThemeEngine.getThemePalette(themeMode)
        }
    }
}
