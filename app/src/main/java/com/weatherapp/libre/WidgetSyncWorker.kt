package com.weatherapp.libre

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.weatherapp.libre.lunar.LunarCalculator
import com.weatherapp.libre.lunar.getEmoji
import com.weatherapp.libre.solar.Observer as SolarObserver
import com.weatherapp.libre.solar.SolarLightingEngine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class WidgetSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val WORK_NAME_PERIODIC = "widget_sync_periodic"
        private const val WORK_NAME_IMMEDIATE = "widget_immediate_sync"

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetSyncWorker>(60, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancelPeriodic(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
        }

        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetSyncWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_IMMEDIATE,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun notifyWidgetsInvalidated(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetTypes = listOf(
                ComponentName(context, WeatherWidgetProvider4x2::class.java),
                ComponentName(context, WeatherWidgetProvider4x1::class.java),
                ComponentName(context, WeatherWidgetProvider2x2::class.java),
                ComponentName(context, WeatherWidgetProvider::class.java)
            )

            var hasActiveWidgets = false
            val activeWidgetIds = mutableListOf<Int>()

            for (component in widgetTypes) {
                val ids = appWidgetManager.getAppWidgetIds(component)
                if (ids.isNotEmpty()) {
                    hasActiveWidgets = true
                    activeWidgetIds.addAll(ids.toList())
                }
            }

            if (!hasActiveWidgets || activeWidgetIds.isEmpty()) return

            for (appWidgetId in activeWidgetIds.distinct()) {
                rebuildAndUpdate(context, appWidgetId)
            }

            enqueueImmediate(context)
        }

        fun rebuildAndUpdate(context: Context, appWidgetId: Int) = kotlinx.coroutines.runBlocking {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId) ?: return@runBlocking
            val layoutId = widgetInfo.initialLayout
            val providerComponent = widgetInfo.provider ?: ComponentName(context, WeatherWidgetProvider::class.java)

            val widgetPrefs = WidgetPreferences(context)
            val locationPreferences = LocationPreferences(context)
            val weatherRepository = WeatherRepository()

            val activeTab = widgetPrefs.getActiveTab(appWidgetId)
            val showNightVis = widgetPrefs.getShowNightVis(appWidgetId)
            val showSolunar = widgetPrefs.getShowSolunar(appWidgetId)

            val tabCount = locationPreferences.getTabCount()
            val safeActiveTab = activeTab.coerceIn(0, tabCount - 1)
            val location = locationPreferences.getTabLocation(safeActiveTab)
            val unitSystem = locationPreferences.getUnitSystem()

            val weatherResult = weatherRepository.getWeatherForecast(
                latitude = location.latitude,
                longitude = location.longitude,
                model = locationPreferences.getActiveModelForLocation(location),
                forceRefresh = false,
                isCacheEnabled = locationPreferences.isCacheTtlEnabled(),
                ttlMinutes = locationPreferences.getCacheTtlMinutes(),
                locationPreferences = locationPreferences
            )

            val remoteViews = RemoteViews(context.packageName, layoutId)

            remoteViews.setTextViewText(R.id.tv_widget_location_name, location.name)

            if (weatherResult.isSuccess) {
                val response = weatherResult.getOrNull()

                val tempStr = response?.current?.temperature?.let {
                    UnitUtils.formatTemp(it, unitSystem)
                } ?: context.getString(R.string.position_unavailable)
                remoteViews.setTextViewText(R.id.tv_widget_temp, tempStr)

                val weatherCode = response?.current?.weatherCode ?: 0
                val condIconRes = WeatherCodeUtils.conditionIconRes(weatherCode)
                val condText = WeatherCodeUtils.getWeatherDescription(context, weatherCode)

                remoteViews.setImageViewResource(R.id.iv_widget_condition_icon, condIconRes)
                remoteViews.setTextViewText(R.id.tv_widget_condition, condText)

                val maxTemp = response?.daily?.temperatureMax?.firstOrNull()
                val minTemp = response?.daily?.temperatureMin?.firstOrNull()
                val highLowStr = if (maxTemp != null && minTemp != null) {
                    "H: ${UnitUtils.formatTemp(maxTemp, unitSystem)} / L: ${UnitUtils.formatTemp(minTemp, unitSystem)}"
                } else {
                    "--"
                }
                remoteViews.setTextViewText(R.id.tv_widget_high_low, highLowStr)

                // Moon Phase calculation
                val moonInfo = LunarCalculator.phaseAt(Instant.now())
                val moonEmoji = moonInfo.phaseCategory.getEmoji()
                val moonPct = (moonInfo.illuminationFraction * 100).roundToInt().coerceIn(0, 100)
                remoteViews.setTextViewText(R.id.tv_widget_moon_phase, "$moonEmoji $moonPct%")

                val zoneId = try {
                    ZoneId.of(response?.timezone ?: ZoneId.systemDefault().id)
                } catch (e: Exception) {
                    ZoneId.systemDefault()
                }

                // Solar lighting calculations (Sunrise & Sunset)
                val solarObserver = SolarObserver(
                    latitudeDegrees = location.latitude,
                    longitudeDegrees = location.longitude,
                    elevationMeters = location.elevationMeters
                )
                val solarLighting = SolarLightingEngine.calculateSolarLightingPhases(
                    LocalDate.now(),
                    zoneId,
                    solarObserver
                )

                // Bind Discrete Sunrise & Sunset Rows (2x2, 4x1, 4x2)
                val sunriseStr = formatInstantTime(solarLighting.sunrise?.instantUtc, zoneId)
                val sunsetStr = formatInstantTime(solarLighting.sunset?.instantUtc, zoneId)
                val sunriseLabel = context.getString(R.string.phase_sunrise)
                val sunsetLabel = context.getString(R.string.phase_sunset)

                if (solarLighting.sunrise?.instantUtc != null) {
                    remoteViews.setTextViewText(R.id.tv_widget_sunrise, "$sunriseLabel: $sunriseStr")
                    remoteViews.setViewVisibility(R.id.tv_widget_sunrise, View.VISIBLE)
                } else {
                    remoteViews.setViewVisibility(R.id.tv_widget_sunrise, View.GONE)
                }

                if (solarLighting.sunset?.instantUtc != null) {
                    remoteViews.setTextViewText(R.id.tv_widget_sunset, "$sunsetLabel: $sunsetStr")
                    remoteViews.setViewVisibility(R.id.tv_widget_sunset, View.VISIBLE)
                } else {
                    remoteViews.setViewVisibility(R.id.tv_widget_sunset, View.GONE)
                }

                // Compute Active Natural Light Period (4x1 & 4x2)
                if (layoutId == R.layout.widget_layout_4x2 || layoutId == R.layout.widget_layout_4x1) {
                    val now = Instant.now()
                    val solarPhases = SolarLightingEngine.calculateChronologicalSolarPhases(
                        date = LocalDate.now(),
                        zoneId = zoneId,
                        observer = solarObserver,
                        nowInstant = now
                    )
                    val activePhase = solarPhases.find { phase ->
                        phase.isCurrentActivePhase || (phase.startInstant != null && phase.endInstant != null && !now.isBefore(phase.startInstant) && now.isBefore(phase.endInstant))
                    } ?: solarPhases.firstOrNull()

                    val activePhaseLabel = activePhase?.getDisplayName(context)
                    if (!activePhaseLabel.isNullOrEmpty()) {
                        remoteViews.setTextViewText(R.id.tv_widget_natural_light_period, activePhaseLabel)
                        remoteViews.setViewVisibility(R.id.tv_widget_natural_light_period, View.VISIBLE)
                    } else {
                        remoteViews.setViewVisibility(R.id.tv_widget_natural_light_period, View.GONE)
                    }
                }

                // 4x2 Feature Toggles & Lunar Timings Row
                if (layoutId == R.layout.widget_layout_4x2) {
                    val solunarSummaries = SolunarManager().calculateSolunarForecast(
                        dates = listOf(LocalDate.now()),
                        zoneId = zoneId,
                        location = location
                    )
                    val todaySolunar = solunarSummaries.firstOrNull()

                    val riseStr = formatInstantTime(todaySolunar?.moonrise, zoneId)
                    val setStr = formatInstantTime(todaySolunar?.moonset, zoneId)
                    val transitStr = formatInstantTime(todaySolunar?.transit, zoneId)
                    val nadirStr = formatInstantTime(todaySolunar?.nadir, zoneId)

                    val riseLabel = context.getString(R.string.label_moonrise)
                    val setLabel = context.getString(R.string.label_moonset)
                    val transitLabel = context.getString(R.string.label_transit)
                    val nadirLabel = context.getString(R.string.label_nadir)

                    val timingsStr = "$riseLabel: $riseStr  •  $setLabel: $setStr  •  $transitLabel: $transitStr  •  $nadirLabel: $nadirStr"
                    remoteViews.setTextViewText(R.id.tv_widget_lunar_timings, timingsStr)

                    if (showNightVis) {
                        val visSummaries = NightVisibilityManager().calculateVisibilityPeriods(
                            dates = listOf(LocalDate.now()),
                            zoneId = zoneId,
                            location = location
                        )
                        val category = visSummaries.firstOrNull()?.periods?.firstOrNull()?.getLocalizedCategory(context)
                            ?: context.getString(R.string.rating_moderate)
                        remoteViews.setTextViewText(
                            R.id.tv_widget_night_vis,
                            "${context.getString(R.string.widget_night_vis_label)} $category"
                        )
                        remoteViews.setViewVisibility(R.id.tv_widget_night_vis, View.VISIBLE)
                    } else {
                        remoteViews.setViewVisibility(R.id.tv_widget_night_vis, View.GONE)
                    }

                    if (showSolunar) {
                        val rating = todaySolunar?.getLocalizedRatingLabel(context)
                            ?: context.getString(R.string.rating_moderate)
                        remoteViews.setTextViewText(
                            R.id.tv_widget_solunar,
                            "${context.getString(R.string.widget_solunar_label)} $rating"
                        )
                        remoteViews.setViewVisibility(R.id.tv_widget_solunar, View.VISIBLE)
                    } else {
                        remoteViews.setViewVisibility(R.id.tv_widget_solunar, View.GONE)
                    }

                    if (!showNightVis && !showSolunar) {
                        remoteViews.setViewVisibility(R.id.layout_widget_features, View.GONE)
                    } else {
                        remoteViews.setViewVisibility(R.id.layout_widget_features, View.VISIBLE)
                    }
                }
            } else {
                remoteViews.setTextViewText(R.id.tv_widget_temp, "--")
                remoteViews.setTextViewText(R.id.tv_widget_condition, context.getString(R.string.widget_no_data))
                remoteViews.setTextViewText(R.id.tv_widget_high_low, "--")
                remoteViews.setViewVisibility(R.id.tv_widget_sunrise, View.GONE)
                remoteViews.setViewVisibility(R.id.tv_widget_sunset, View.GONE)
                remoteViews.setViewVisibility(R.id.tv_widget_natural_light_period, View.GONE)
            }

            // Configure Tab Pills (for 4x2, 4x1, and 2x2)
            val pillIds = arrayOf(
                R.id.btn_widget_tab_0,
                R.id.btn_widget_tab_1,
                R.id.btn_widget_tab_2,
                R.id.btn_widget_tab_3
            )

            for (i in 0 until 4) {
                if (i < tabCount) {
                    val tabLoc = locationPreferences.getTabLocation(i)
                    val isSelected = (i == safeActiveTab)

                    remoteViews.setTextViewText(pillIds[i], tabLoc.name)
                    remoteViews.setViewVisibility(pillIds[i], View.VISIBLE)

                    val bgRes = if (isSelected) {
                        R.drawable.bg_widget_pill_selected
                    } else {
                        R.drawable.bg_widget_pill_unselected
                    }
                    remoteViews.setInt(pillIds[i], "setBackgroundResource", bgRes)

                    val textColor = if (isSelected) {
                        ContextCompat.getColor(context, R.color.widget_text_primary)
                    } else {
                        ContextCompat.getColor(context, R.color.widget_text_secondary)
                    }
                    remoteViews.setTextColor(pillIds[i], textColor)

                    val switchIntent = Intent().apply {
                        component = providerComponent
                        action = WeatherWidgetProvider.ACTION_WIDGET_SWITCH_TAB
                        data = Uri.parse("weatherwidget://tab/$appWidgetId/$i")
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        putExtra(WeatherWidgetProvider.EXTRA_TAB_INDEX, i)
                    }

                    val switchPendingIntent = PendingIntent.getBroadcast(
                        context,
                        (appWidgetId * 100) + i,
                        switchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    remoteViews.setOnClickPendingIntent(pillIds[i], switchPendingIntent)
                } else {
                    remoteViews.setViewVisibility(pillIds[i], View.GONE)
                }
            }

            // Attach Deep-Linking PendingIntent to main body
            val deepLinkIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(WeatherWidgetProvider.EXTRA_WIDGET_TAB_INDEX, safeActiveTab)
                putExtra(WeatherWidgetProvider.EXTRA_WIDGET_SUBTAB, "CURRENT")
            }
            val deepLinkPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                deepLinkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            remoteViews.setOnClickPendingIntent(R.id.widget_main_body, deepLinkPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }

        private fun formatInstantTime(instant: Instant?, zoneId: ZoneId): String {
            if (instant == null) return "——"
            val zdt = ZonedDateTime.ofInstant(instant, zoneId)
            val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
            return zdt.format(formatter)
        }
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(context)

        val widgetTypes = listOf(
            ComponentName(context, WeatherWidgetProvider4x2::class.java),
            ComponentName(context, WeatherWidgetProvider4x1::class.java),
            ComponentName(context, WeatherWidgetProvider2x2::class.java),
            ComponentName(context, WeatherWidgetProvider::class.java)
        )

        for (component in widgetTypes) {
            val widgetIds = appWidgetManager.getAppWidgetIds(component)
            for (appWidgetId in widgetIds) {
                rebuildAndUpdate(context, appWidgetId)
            }
        }

        return Result.success()
    }
}
