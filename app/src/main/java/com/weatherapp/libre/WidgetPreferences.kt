package com.weatherapp.libre

import android.content.Context
import android.content.SharedPreferences

class WidgetPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("weather_widget_prefs", Context.MODE_PRIVATE)

    companion object {
        const val WIDGET_THEME_MATERIAL_YOU = 0
        const val WIDGET_THEME_BOUTIQUE = 1
    }

    fun getWidgetTheme(appWidgetId: Int): Int {
        return prefs.getInt("widget_theme_$appWidgetId", WIDGET_THEME_MATERIAL_YOU)
    }

    fun saveWidgetTheme(appWidgetId: Int, themeMode: Int) {
        prefs.edit().putInt("widget_theme_$appWidgetId", themeMode).apply()
    }

    fun getShowNightVis(appWidgetId: Int): Boolean {
        return prefs.getBoolean("widget_show_night_vis_$appWidgetId", true)
    }

    fun saveShowNightVis(appWidgetId: Int, show: Boolean) {
        prefs.edit().putBoolean("widget_show_night_vis_$appWidgetId", show).apply()
    }

    fun getShowSolunar(appWidgetId: Int): Boolean {
        return prefs.getBoolean("widget_show_solunar_$appWidgetId", true)
    }

    fun saveShowSolunar(appWidgetId: Int, show: Boolean) {
        prefs.edit().putBoolean("widget_show_solunar_$appWidgetId", show).apply()
    }

    fun getActiveTab(appWidgetId: Int): Int {
        return prefs.getInt("widget_active_tab_$appWidgetId", 0).coerceIn(0, 3)
    }

    fun saveActiveTab(appWidgetId: Int, tabIndex: Int) {
        prefs.edit().putInt("widget_active_tab_$appWidgetId", tabIndex.coerceIn(0, 3)).apply()
    }

    fun deleteWidgetPrefs(appWidgetId: Int) {
        prefs.edit()
            .remove("widget_theme_$appWidgetId")
            .remove("widget_show_night_vis_$appWidgetId")
            .remove("widget_show_solunar_$appWidgetId")
            .remove("widget_active_tab_$appWidgetId")
            .apply()
    }
}
