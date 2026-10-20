package com.weatherapp.libre

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log

abstract class BaseWeatherWidgetProvider : AppWidgetProvider() {

    companion object {
        private var lastUpdateTime = 0L
        private const val DEBOUNCE_WINDOW_MS = 10_000L // 10 seconds
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val now = System.currentTimeMillis()
        if (now - lastUpdateTime < DEBOUNCE_WINDOW_MS) {
            Log.i("WidgetSync", "onUpdate DEBOUNCED (last update ${now - lastUpdateTime}ms ago)")
            return
        }
        lastUpdateTime = now

        WidgetSyncWorker.enqueueImmediate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WeatherWidgetProvider.ACTION_WIDGET_SWITCH_TAB) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            val tabIndex = intent.getIntExtra(WeatherWidgetProvider.EXTRA_TAB_INDEX, 0)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val widgetPrefs = WidgetPreferences(context)
                widgetPrefs.saveActiveTab(appWidgetId, tabIndex)

                // Synchronous immediate redraw from cache
                WidgetSyncWorker.rebuildAndUpdate(context, appWidgetId)

                // Secondary background pass
                WidgetSyncWorker.enqueueImmediate(context)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetSyncWorker.enqueuePeriodic(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetSyncWorker.cancelPeriodic(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val widgetPrefs = WidgetPreferences(context)
        for (id in appWidgetIds) {
            widgetPrefs.deleteWidgetPrefs(id)
        }
    }
}

open class WeatherWidgetProvider : BaseWeatherWidgetProvider() {
    companion object {
        const val ACTION_WIDGET_SWITCH_TAB = "com.weatherapp.libre.ACTION_WIDGET_SWITCH_TAB"
        const val EXTRA_TAB_INDEX = "extra_tab_index"
        const val EXTRA_WIDGET_TAB_INDEX = "extra_widget_tab_index"
        const val EXTRA_WIDGET_SUBTAB = "extra_widget_subtab"
    }
}

class WeatherWidgetProvider4x2 : BaseWeatherWidgetProvider()
class WeatherWidgetProvider4x1 : BaseWeatherWidgetProvider()
class WeatherWidgetProvider2x2 : BaseWeatherWidgetProvider()
