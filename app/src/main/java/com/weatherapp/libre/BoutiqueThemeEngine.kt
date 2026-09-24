package com.weatherapp.libre

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
    val dividerColor: Int
)

object BoutiqueThemeEngine {

    val LIGHT_PALETTE = ColorPalette(
        primary = Color.parseColor("#007AFF"),
        surfaceCard = Color.parseColor("#FFFFFF"),
        surfaceCardSecondary = Color.parseColor("#F2F2F7"),
        backgroundLight = Color.parseColor("#F8F9FA"),
        textPrimary = Color.parseColor("#1C1C1E"),
        textSecondary = Color.parseColor("#8E8E93"),
        tempHigh = Color.parseColor("#DC2626"),
        tempHighBg = Color.parseColor("#FEE2E2"),
        tempLow = Color.parseColor("#0284C7"),
        tempLowBg = Color.parseColor("#E0F2FE"),
        goldenHourPrimary = Color.parseColor("#FF9500"),
        blueHourPrimary = Color.parseColor("#5856D6"),
        dividerColor = Color.parseColor("#E5E5EA")
    )

    val DARK_PALETTE = ColorPalette(
        primary = Color.parseColor("#60A5FA"),
        surfaceCard = Color.parseColor("#1E293B"),
        surfaceCardSecondary = Color.parseColor("#334155"),
        backgroundLight = Color.parseColor("#0F172A"),
        textPrimary = Color.parseColor("#F8FAFC"),
        textSecondary = Color.parseColor("#94A3B8"),
        tempHigh = Color.parseColor("#FF8A65"),
        tempHighBg = Color.parseColor("#3E2723"),
        tempLow = Color.parseColor("#64B5F6"),
        tempLowBg = Color.parseColor("#0D3349"),
        goldenHourPrimary = Color.parseColor("#FFB74D"),
        blueHourPrimary = Color.parseColor("#9FA8DA"),
        dividerColor = Color.parseColor("#334155")
    )

    val CREMA_PALETTE = ColorPalette(
        primary = Color.parseColor("#D97757"),
        surfaceCard = Color.parseColor("#FAF5EF"),
        surfaceCardSecondary = Color.parseColor("#F0E8DF"),
        backgroundLight = Color.parseColor("#F4ECE1"),
        textPrimary = Color.parseColor("#2D231E"),
        textSecondary = Color.parseColor("#7C6C62"),
        tempHigh = Color.parseColor("#C0392B"),
        tempHighBg = Color.parseColor("#FCE8E6"),
        tempLow = Color.parseColor("#27AE60"),
        tempLowBg = Color.parseColor("#E8F4F1"),
        goldenHourPrimary = Color.parseColor("#D98A32"),
        blueHourPrimary = Color.parseColor("#6A7B9B"),
        dividerColor = Color.parseColor("#E0D4C8")
    )

    val CHARCOAL_PALETTE = ColorPalette(
        primary = Color.parseColor("#9E9E9E"),
        surfaceCard = Color.parseColor("#212121"),
        surfaceCardSecondary = Color.parseColor("#2C2C2C"),
        backgroundLight = Color.parseColor("#121212"),
        textPrimary = Color.parseColor("#EEEEEE"),
        textSecondary = Color.parseColor("#9E9E9E"),
        tempHigh = Color.parseColor("#EF9A9A"),
        tempHighBg = Color.parseColor("#2D1C1C"),
        tempLow = Color.parseColor("#90CAF9"),
        tempLowBg = Color.parseColor("#1C2833"),
        goldenHourPrimary = Color.parseColor("#FFB74D"),
        blueHourPrimary = Color.parseColor("#7986CB"),
        dividerColor = Color.parseColor("#383838")
    )

    val COBALT_PALETTE = ColorPalette(
        primary = Color.parseColor("#00A8E8"),
        surfaceCard = Color.parseColor("#001F54"),
        surfaceCardSecondary = Color.parseColor("#034078"),
        backgroundLight = Color.parseColor("#0A1128"),
        textPrimary = Color.parseColor("#FEFCFB"),
        textSecondary = Color.parseColor("#8DA9C4"),
        tempHigh = Color.parseColor("#FF6B6B"),
        tempHighBg = Color.parseColor("#2D0A12"),
        tempLow = Color.parseColor("#00E676"),
        tempLowBg = Color.parseColor("#002855"),
        goldenHourPrimary = Color.parseColor("#FFB703"),
        blueHourPrimary = Color.parseColor("#4895EF"),
        dividerColor = Color.parseColor("#1282A2")
    )

    val JADE_PALETTE = ColorPalette(
        primary = Color.parseColor("#2EC4B6"),
        surfaceCard = Color.parseColor("#132E27"),
        surfaceCardSecondary = Color.parseColor("#1A3E34"),
        backgroundLight = Color.parseColor("#0B1D17"),
        textPrimary = Color.parseColor("#E8F5F1"),
        textSecondary = Color.parseColor("#80B9AD"),
        tempHigh = Color.parseColor("#FF7675"),
        tempHighBg = Color.parseColor("#2A1215"),
        tempLow = Color.parseColor("#55E6C1"),
        tempLowBg = Color.parseColor("#0F382C"),
        goldenHourPrimary = Color.parseColor("#FFB703"),
        blueHourPrimary = Color.parseColor("#3A86FF"),
        dividerColor = Color.parseColor("#235245")
    )

    val AURORA_PALETTE = ColorPalette(
        primary = Color.parseColor("#5B5F97"),
        surfaceCard = Color.parseColor("#1B1D28"),
        surfaceCardSecondary = Color.parseColor("#204047"),
        backgroundLight = Color.parseColor("#0D0E15"),
        textPrimary = Color.parseColor("#E2E8F0"),
        textSecondary = Color.parseColor("#94A3B8"),
        tempHigh = Color.parseColor("#E84393"),
        tempHighBg = Color.parseColor("#2B141D"),
        tempLow = Color.parseColor("#00CEC9"),
        tempLowBg = Color.parseColor("#0B2527"),
        goldenHourPrimary = Color.parseColor("#FDCB6E"),
        blueHourPrimary = Color.parseColor("#A29BFE"),
        dividerColor = Color.parseColor("#2A2D3D")
    )

    val TERRACOTTA_PALETTE = ColorPalette(
        primary = Color.parseColor("#C05621"),
        surfaceCard = Color.parseColor("#FDF8F5"),
        surfaceCardSecondary = Color.parseColor("#F3E8E2"),
        backgroundLight = Color.parseColor("#EDE3DA"),
        textPrimary = Color.parseColor("#3B2219"),
        textSecondary = Color.parseColor("#7C594F"),
        tempHigh = Color.parseColor("#B91C1C"),
        tempHighBg = Color.parseColor("#FADBD8"),
        tempLow = Color.parseColor("#15803D"),
        tempLowBg = Color.parseColor("#E6F4EA"),
        goldenHourPrimary = Color.parseColor("#D97706"),
        blueHourPrimary = Color.parseColor("#6574CD"),
        dividerColor = Color.parseColor("#E2D8CE")
    )

    val FROST_PALETTE = ColorPalette(
        primary = Color.parseColor("#0284C7"),
        surfaceCard = Color.parseColor("#E0F2FE"),
        surfaceCardSecondary = Color.parseColor("#BAE6FD"),
        backgroundLight = Color.parseColor("#F0F9FF"),
        textPrimary = Color.parseColor("#032B45"),
        textSecondary = Color.parseColor("#335C7A"),
        tempHigh = Color.parseColor("#E11D48"),
        tempHighBg = Color.parseColor("#FFE4E6"),
        tempLow = Color.parseColor("#0284C7"),
        tempLowBg = Color.parseColor("#BAE6FD"),
        goldenHourPrimary = Color.parseColor("#D97706"),
        blueHourPrimary = Color.parseColor("#2563EB"),
        dividerColor = Color.parseColor("#7DD3FC")
    )

    val FIRE_ENGINE_PALETTE = ColorPalette(
        primary = Color.parseColor("#CE2029"),
        surfaceCard = Color.parseColor("#FAFAFA"),
        surfaceCardSecondary = Color.parseColor("#F3F4F6"),
        backgroundLight = Color.parseColor("#FFFFFF"),
        textPrimary = Color.parseColor("#1A1A1A"),
        textSecondary = Color.parseColor("#4B5563"),
        tempHigh = Color.parseColor("#CE2029"),
        tempHighBg = Color.parseColor("#FEE2E2"),
        tempLow = Color.parseColor("#2563EB"),
        tempLowBg = Color.parseColor("#DBEAFE"),
        goldenHourPrimary = Color.parseColor("#D97706"),
        blueHourPrimary = Color.parseColor("#4F46E5"),
        dividerColor = Color.parseColor("#E5E7EB")
    )

    val ORANGE_GROVE_PALETTE = ColorPalette(
        primary = Color.parseColor("#FF6F00"),
        surfaceCard = Color.parseColor("#FAF7EF"),
        surfaceCardSecondary = Color.parseColor("#E2ECD8"),
        backgroundLight = Color.parseColor("#F2F5EB"),
        textPrimary = Color.parseColor("#1B3310"),
        textSecondary = Color.parseColor("#47633B"),
        tempHigh = Color.parseColor("#E65100"),
        tempHighBg = Color.parseColor("#FFE0B2"),
        tempLow = Color.parseColor("#2D5016"),
        tempLowBg = Color.parseColor("#DCFCE7"),
        goldenHourPrimary = Color.parseColor("#FFC107"),
        blueHourPrimary = Color.parseColor("#5C6BC0"),
        dividerColor = Color.parseColor("#D5DFC8")
    )

    fun getPalette(isDarkMode: Boolean): ColorPalette {
        return if (isDarkMode) DARK_PALETTE else LIGHT_PALETTE
    }

    fun getThemePalette(themeMode: Int): ColorPalette {
        return when (themeMode) {
            LocationPreferences.THEME_LIGHT -> LIGHT_PALETTE
            LocationPreferences.THEME_DARK -> DARK_PALETTE
            LocationPreferences.THEME_CREMA -> CREMA_PALETTE
            LocationPreferences.THEME_CHARCOAL -> CHARCOAL_PALETTE
            LocationPreferences.THEME_COBALT -> COBALT_PALETTE
            LocationPreferences.THEME_JADE -> JADE_PALETTE
            LocationPreferences.THEME_AURORA -> AURORA_PALETTE
            LocationPreferences.THEME_TERRACOTTA -> TERRACOTTA_PALETTE
            LocationPreferences.THEME_FROST -> FROST_PALETTE
            LocationPreferences.THEME_FIRE_ENGINE -> FIRE_ENGINE_PALETTE
            LocationPreferences.THEME_ORANGE_GROVE -> ORANGE_GROVE_PALETTE
            else -> DARK_PALETTE
        }
    }
}
