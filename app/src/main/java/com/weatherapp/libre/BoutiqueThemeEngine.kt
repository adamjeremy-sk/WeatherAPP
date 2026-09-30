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
) {
    val background: Int get() = backgroundLight
    val brandPrimary: Int get() = primary

    companion object {
        fun forMode(themeMode: Int, context: android.content.Context? = null): ColorPalette {
            return BoutiqueThemeEngine.getThemePalette(themeMode)
        }
    }
}

object BoutiqueThemeEngine {

    val LIGHT_PALETTE = ColorPalette(
        primary = Color.parseColor("#0284C7"),
        surfaceCard = Color.parseColor("#FFFFFF"),
        surfaceCardSecondary = Color.parseColor("#F1F5F9"),
        backgroundLight = Color.parseColor("#F8FAFC"),
        textPrimary = Color.parseColor("#0F172A"),
        textSecondary = Color.parseColor("#475569"),
        tempHigh = Color.parseColor("#DC2626"),
        tempHighBg = Color.parseColor("#FEE2E2"),
        tempLow = Color.parseColor("#0284C7"),
        tempLowBg = Color.parseColor("#E0F2FE"),
        goldenHourPrimary = Color.parseColor("#D97706"),
        blueHourPrimary = Color.parseColor("#4F46E5"),
        dividerColor = Color.parseColor("#CBD5E1")
    )

    val DARK_PALETTE = ColorPalette(
        primary = Color.parseColor("#38BDF8"),
        surfaceCard = Color.parseColor("#1E293B"),
        surfaceCardSecondary = Color.parseColor("#334155"),
        backgroundLight = Color.parseColor("#0F172A"),
        textPrimary = Color.parseColor("#FFFFFF"),
        textSecondary = Color.parseColor("#CBD5E1"),
        tempHigh = Color.parseColor("#FF7A59"),
        tempHighBg = Color.parseColor("#3E2723"),
        tempLow = Color.parseColor("#38BDF8"),
        tempLowBg = Color.parseColor("#0D3349"),
        goldenHourPrimary = Color.parseColor("#FBBF24"),
        blueHourPrimary = Color.parseColor("#818CF8"),
        dividerColor = Color.parseColor("#475569")
    )

    val CREMA_PALETTE = ColorPalette(
        primary = Color.parseColor("#C05621"),
        surfaceCard = Color.parseColor("#FAF5EF"),
        surfaceCardSecondary = Color.parseColor("#EADFD5"),
        backgroundLight = Color.parseColor("#F3EBE1"),
        textPrimary = Color.parseColor("#1C120C"),
        textSecondary = Color.parseColor("#5A4B43"),
        tempHigh = Color.parseColor("#B91C1C"),
        tempHighBg = Color.parseColor("#FCE8E6"),
        tempLow = Color.parseColor("#15803D"),
        tempLowBg = Color.parseColor("#E8F4F1"),
        goldenHourPrimary = Color.parseColor("#D97706"),
        blueHourPrimary = Color.parseColor("#4338CA"),
        dividerColor = Color.parseColor("#D5C5B8")
    )

    val CHARCOAL_PALETTE = ColorPalette(
        primary = Color.parseColor("#64B5F6"),
        surfaceCard = Color.parseColor("#212121"),
        surfaceCardSecondary = Color.parseColor("#333333"),
        backgroundLight = Color.parseColor("#121212"),
        textPrimary = Color.parseColor("#FFFFFF"),
        textSecondary = Color.parseColor("#CCCCCC"),
        tempHigh = Color.parseColor("#FF8A65"),
        tempHighBg = Color.parseColor("#331D1D"),
        tempLow = Color.parseColor("#64B5F6"),
        tempLowBg = Color.parseColor("#162836"),
        goldenHourPrimary = Color.parseColor("#FBBF24"),
        blueHourPrimary = Color.parseColor("#818CF8"),
        dividerColor = Color.parseColor("#424242")
    )

    val COBALT_PALETTE = ColorPalette(
        primary = Color.parseColor("#38BDF8"),
        surfaceCard = Color.parseColor("#001F54"),
        surfaceCardSecondary = Color.parseColor("#034078"),
        backgroundLight = Color.parseColor("#0A1128"),
        textPrimary = Color.parseColor("#FFFFFF"),
        textSecondary = Color.parseColor("#B8C0E0"),
        tempHigh = Color.parseColor("#FF6B6B"),
        tempHighBg = Color.parseColor("#3A0B14"),
        tempLow = Color.parseColor("#38BDF8"),
        tempLowBg = Color.parseColor("#002855"),
        goldenHourPrimary = Color.parseColor("#FFC107"),
        blueHourPrimary = Color.parseColor("#60A5FA"),
        dividerColor = Color.parseColor("#1282A2")
    )

    val JADE_PALETTE = ColorPalette(
        primary = Color.parseColor("#2DD4BF"),
        surfaceCard = Color.parseColor("#132E27"),
        surfaceCardSecondary = Color.parseColor("#1D4435"),
        backgroundLight = Color.parseColor("#0B1D17"),
        textPrimary = Color.parseColor("#FFFFFF"),
        textSecondary = Color.parseColor("#A7F3D0"),
        tempHigh = Color.parseColor("#FF7675"),
        tempHighBg = Color.parseColor("#3B181A"),
        tempLow = Color.parseColor("#2DD4BF"),
        tempLowBg = Color.parseColor("#0F382C"),
        goldenHourPrimary = Color.parseColor("#FBBF24"),
        blueHourPrimary = Color.parseColor("#60A5FA"),
        dividerColor = Color.parseColor("#235245")
    )

    val AURORA_PALETTE = ColorPalette(
        primary = Color.parseColor("#818CF8"),
        surfaceCard = Color.parseColor("#1B1D28"),
        surfaceCardSecondary = Color.parseColor("#252838"),
        backgroundLight = Color.parseColor("#0D0E15"),
        textPrimary = Color.parseColor("#FFFFFF"),
        textSecondary = Color.parseColor("#CBD5E1"),
        tempHigh = Color.parseColor("#FF75C3"),
        tempHighBg = Color.parseColor("#3B1728"),
        tempLow = Color.parseColor("#38BDF8"),
        tempLowBg = Color.parseColor("#0F2D3A"),
        goldenHourPrimary = Color.parseColor("#FBBF24"),
        blueHourPrimary = Color.parseColor("#A5B4FC"),
        dividerColor = Color.parseColor("#33374C")
    )

    val TERRACOTTA_PALETTE = ColorPalette(
        primary = Color.parseColor("#9A3412"),
        surfaceCard = Color.parseColor("#FDF8F5"),
        surfaceCardSecondary = Color.parseColor("#F3E2DA"),
        backgroundLight = Color.parseColor("#EADCD3"),
        textPrimary = Color.parseColor("#1F110B"),
        textSecondary = Color.parseColor("#5C3C32"),
        tempHigh = Color.parseColor("#B91C1C"),
        tempHighBg = Color.parseColor("#FADBD8"),
        tempLow = Color.parseColor("#15803D"),
        tempLowBg = Color.parseColor("#E6F4EA"),
        goldenHourPrimary = Color.parseColor("#D97706"),
        blueHourPrimary = Color.parseColor("#4338CA"),
        dividerColor = Color.parseColor("#D9C8BC")
    )

    val FROST_PALETTE = ColorPalette(
        primary = Color.parseColor("#0284C7"),
        surfaceCard = Color.parseColor("#E0F2FE"),
        surfaceCardSecondary = Color.parseColor("#BAE6FD"),
        backgroundLight = Color.parseColor("#F0F9FF"),
        textPrimary = Color.parseColor("#031B2E"),
        textSecondary = Color.parseColor("#1E40AF"),
        tempHigh = Color.parseColor("#E11D48"),
        tempHighBg = Color.parseColor("#FFE4E6"),
        tempLow = Color.parseColor("#0284C7"),
        tempLowBg = Color.parseColor("#93C5FD"),
        goldenHourPrimary = Color.parseColor("#D97706"),
        blueHourPrimary = Color.parseColor("#1D4ED8"),
        dividerColor = Color.parseColor("#7DD3FC")
    )

    val FIRE_ENGINE_PALETTE = ColorPalette(
        primary = Color.parseColor("#DC2626"),
        surfaceCard = Color.parseColor("#FFFFFF"),
        surfaceCardSecondary = Color.parseColor("#F3F4F6"),
        backgroundLight = Color.parseColor("#F9FAFB"),
        textPrimary = Color.parseColor("#1A1A1A"),
        textSecondary = Color.parseColor("#4B5563"),
        tempHigh = Color.parseColor("#DC2626"),
        tempHighBg = Color.parseColor("#FEE2E2"),
        tempLow = Color.parseColor("#2563EB"),
        tempLowBg = Color.parseColor("#DBEAFE"),
        goldenHourPrimary = Color.parseColor("#D97706"),
        blueHourPrimary = Color.parseColor("#4338CA"),
        dividerColor = Color.parseColor("#E5E7EB")
    )

    val ORANGE_GROVE_PALETTE = ColorPalette(
        primary = Color.parseColor("#D97706"),
        surfaceCard = Color.parseColor("#FAF7EF"),
        surfaceCardSecondary = Color.parseColor("#D8E6CC"),
        backgroundLight = Color.parseColor("#EEF4E6"),
        textPrimary = Color.parseColor("#0F240A"),
        textSecondary = Color.parseColor("#2D4A21"),
        tempHigh = Color.parseColor("#EA580C"),
        tempHighBg = Color.parseColor("#FFEDD5"),
        tempLow = Color.parseColor("#15803D"),
        tempLowBg = Color.parseColor("#DCFCE7"),
        goldenHourPrimary = Color.parseColor("#D97706"),
        blueHourPrimary = Color.parseColor("#4338CA"),
        dividerColor = Color.parseColor("#C2D4B2")
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
