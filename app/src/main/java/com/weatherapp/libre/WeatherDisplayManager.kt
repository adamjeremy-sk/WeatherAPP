package com.weatherapp.libre

import android.util.Log
import com.weatherapp.theme.BoutiqueThemeEngine
import com.weatherapp.theme.ColorPalette
import android.content.res.ColorStateList
import android.text.format.DateFormat
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.weatherapp.libre.alerts.AlertBannerManager
import com.weatherapp.libre.databinding.ActivityMainBinding
import com.weatherapp.libre.lunar.LunarCalculator
import com.weatherapp.libre.lunar.MoonEventType
import com.weatherapp.libre.lunar.MoonPhaseName
import com.weatherapp.libre.lunar.getEmoji
import com.weatherapp.libre.lunar.getNameRes
import com.weatherapp.libre.lunar.Observer as LunarObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class WeatherDisplayManager(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val locationPreferences: LocationPreferences,
    private val repository: WeatherRepository,
    private val astronomyManager: AstronomyDisplayManager,
    private val lifecycleScope: CoroutineScope
) {

    var currentLocation: LocationItem = LocationItem(id = 0L, name = "Ottawa", admin1 = "Ontario", country = "Canada", latitude = 45.4215, longitude = -75.6972)
    var locationTimezone: String? = null
    var currentWeatherData: CurrentData? = null
    var currentResponse: WeatherResponse? = null
    var selectedDayForecast: DayForecast? = null
    var dailyForecasts: List<DayForecast> = emptyList()
    var forecastAdapter: DayForecastAdapter? = null
    var alertBannerManager: AlertBannerManager? = null
    var metricGridAdapter: MetricGridAdapter? = null
    var hourlyDisplayManager: HourlyDisplayManager? = null

    private val clockHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            val nowInstant = Instant.now()
            if (locationTimezone != null) {
                selectedDayForecast?.let { day ->
                    val isToday = day.dayOfWeek.equals("Today", ignoreCase = true)
                    if (isToday) {
                        val is24Hour = DateFormat.is24HourFormat(activity)
                        val baseDateStr = day.formattedDate.uppercase(Locale.getDefault())
                        val currentTime = UnitUtils.formatLocationTime(locationTimezone, is24Hour, nowInstant)
                        val localTimeLabel = activity.getString(R.string.label_local_time)
                        binding.tvSelectedDateHeader.text = "$baseDateStr • $currentTime • $localTimeLabel"
                        astronomyManager.updateInlineGoldenHourDisplay(currentLocation, nowInstant)
                    }
                }

                alertBannerManager?.updateAlertBanner(currentLocation)
            }

            // Align the tick to the top of the next minute
            val delayMs = 60000L - (System.currentTimeMillis() % 60000L)
            clockHandler.postDelayed(this, delayMs)
        }
    }

    init {
        activity.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
                clockHandler.post(clockRunnable)
            }
            override fun onPause(owner: androidx.lifecycle.LifecycleOwner) {
                clockHandler.removeCallbacks(clockRunnable)
            }
        })
        setupHeroCardClickListener()
    }

    fun cleanup() {
        clockHandler.removeCallbacksAndMessages(null)
    }

    fun updateLocationHeader() {
        val density = binding.root.resources.displayMetrics.density
        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())
        binding.tvLocationName.text = currentLocation.displayName
        binding.tvLocationName.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_chevron_down, 0)
        androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(binding.tvLocationName, android.content.res.ColorStateList.valueOf(palette.textPrimary))
        binding.tvLocationName.compoundDrawablePadding = (4 * density).toInt()
    }

    private var loadWeatherJob: Job? = null

    fun loadWeather(forceRefresh: Boolean = false, onWeatherLoaded: (() -> Unit)? = null) {
        loadWeatherJob?.cancel()
        locationTimezone = null
        val targetLocation = currentLocation

        val activeModel = locationPreferences.getActiveModelForLocation(currentLocation)
        val isCacheEnabled = locationPreferences.isCacheTtlEnabled()
        val ttlMinutes = locationPreferences.getCacheTtlMinutes()

        val hasCache = isCacheEnabled && repository.hasValidCache(
            latitude = currentLocation.latitude,
            longitude = currentLocation.longitude,
            model = activeModel,
            ttlMinutes = ttlMinutes,
            locationPreferences = locationPreferences
        )

        if (forceRefresh || !hasCache) {
            binding.pbLoading.visibility = View.VISIBLE
        }
        binding.layoutError.visibility = View.GONE

        loadWeatherJob = lifecycleScope.launch {
            binding.tvAttribution.text = activeModel.getSourceAttribution(activity)

            val result = repository.getWeatherForecast(
                latitude = targetLocation.latitude,
                longitude = targetLocation.longitude,
                model = activeModel,
                forceRefresh = forceRefresh,
                isCacheEnabled = isCacheEnabled,
                ttlMinutes = ttlMinutes,
                locationPreferences = locationPreferences
            )

            ensureActive()
            if (targetLocation.latitude != currentLocation.latitude || targetLocation.longitude != currentLocation.longitude || targetLocation.name != currentLocation.name) {
                android.util.Log.d("WeatherDisplayManager", "Discarding stale weather response for ${targetLocation.name} (current is ${currentLocation.name})")
                return@launch
            }

            binding.pbLoading.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false

            if (isActive) {
                result.fold(
                    onSuccess = { response ->
                        currentResponse = response
                        locationTimezone = response.timezone
                        currentWeatherData = response.current

                        val unitSystem = locationPreferences.getUnitSystem()

                        val todayForecast = repository.processTodayForecast(
                            response = response,
                            latitude = currentLocation.latitude,
                            longitude = currentLocation.longitude
                        )

                        dailyForecasts = repository.processDailyForecast(
                            response = response,
                            latitude = currentLocation.latitude,
                            longitude = currentLocation.longitude
                        )

                        forecastAdapter?.setUnitSystem(unitSystem)
                        forecastAdapter?.setLocation(currentLocation, locationTimezone)
                        forecastAdapter?.submitList(dailyForecasts)

                        if (todayForecast != null) {
                            selectedDayForecast = todayForecast
                            updateDetailCard(todayForecast)
                        } else if (dailyForecasts.isNotEmpty()) {
                            selectedDayForecast = dailyForecasts.first()
                            updateDetailCard(dailyForecasts.first())
                        }

                        val cloudMap = extractHourlyCloudCoverMap(response)
                        astronomyManager.setLocation(currentLocation, locationTimezone)
                        astronomyManager.currentHourlyCloudCoverMap = cloudMap

                        when (astronomyManager.activeInlineSubTab) {
                            InlineSubTab.NATURAL_LIGHT -> astronomyManager.updateInlineGoldenHourDisplay(currentLocation)
                            InlineSubTab.DARK_SKY -> astronomyManager.updateDarkSkyDisplay(currentLocation)
                            InlineSubTab.NIGHT_VISIBILITY -> astronomyManager.updateNightVisDisplay(currentLocation, cloudMap)
                            InlineSubTab.SOLUNAR -> astronomyManager.updateSolunarDisplay(currentLocation)
                            InlineSubTab.LUNAR_CALENDAR -> astronomyManager.updateInlineLunarCalendarDisplay(currentLocation)
                            else -> {}
                        }

                        hourlyDisplayManager?.updateHourlyData(response, currentLocation, locationTimezone)

                        updateAlertBanner()
                        onWeatherLoaded?.invoke()
                    },
                    onFailure = {
                        binding.layoutError.visibility = View.VISIBLE
                        Toast.makeText(activity.applicationContext, activity.getString(R.string.toast_failed_load_weather), Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    fun updateAlertBanner() {
        alertBannerManager?.updateAlertBanner(currentLocation)
    }

    fun updateDetailCard(day: DayForecast) {
        selectedDayForecast = day
        val unitSystem = locationPreferences.getUnitSystem()
        val is24Hour = DateFormat.is24HourFormat(activity)

        val isToday = day.dayOfWeek.equals("Today", ignoreCase = true)
        val dateHeader = if (isToday) {
            val baseDateStr = day.formattedDate.uppercase(Locale.getDefault())
            val currentTime = UnitUtils.formatLocationTime(locationTimezone, is24Hour)
            "$baseDateStr • $currentTime • ${activity.getString(R.string.label_local_time)}"
        } else {
            day.formattedDate.uppercase(Locale.getDefault())
        }

        binding.tvSelectedDateHeader.text = dateHeader
        val iconRes = WeatherCodeUtils.conditionIconRes(day.weatherCode)
        binding.root.findViewById<android.widget.ImageView>(R.id.ivMainConditionIcon)?.apply {
            setImageResource(iconRes)
            val currentPalette = ColorPalette.forMode(locationPreferences.getThemeMode(), activity)
            setColorFilter(currentPalette.brandPrimary)
        }
        binding.tvMainCondition.text = WeatherCodeUtils.getWeatherDescription(activity, day.weatherCode)

        val currentTemp = currentWeatherData?.temperature
        if (currentTemp != null && isToday) {
            binding.tvCurrentTemp.text = UnitUtils.formatTempPrecise(currentTemp, unitSystem)
        } else {
            binding.tvCurrentTemp.text = UnitUtils.formatTemp(day.tempMax, unitSystem)
        }
        binding.tvCurrentTemp.visibility = View.VISIBLE

        val apparentTemp = currentWeatherData?.apparentTemperature
        if (shouldShowFeelsLike(isToday, apparentTemp)) {
            val formattedFeelsLike = UnitUtils.formatTempPrecise(apparentTemp!!, unitSystem)
            binding.tvFeelsLike.text = activity.getString(R.string.feels_like_format, formattedFeelsLike)
            binding.tvFeelsLike.visibility = View.VISIBLE
        } else {
            binding.tvFeelsLike.visibility = View.GONE
        }

        binding.root.findViewById<android.widget.TextView>(R.id.tvDailyHigh)?.text = UnitUtils.formatTemp(day.tempMax, unitSystem)
        binding.root.findViewById<android.widget.TextView>(R.id.tvDailyLow)?.text = UnitUtils.formatTemp(day.tempMin, unitSystem)

        
        val current = currentWeatherData
        val windSpeed = current?.windSpeed ?: day.windSpeedMaxKmH
        val windDir = current?.windDirection
        val windGust = current?.windGusts ?: day.windGustsMaxKmH

        val windSpeedStr = UnitUtils.formatWindSpeedPrimary(windSpeed, unitSystem, windDir)
        val gustVal = UnitUtils.formatWindGusts(windSpeed, windGust, unitSystem)
        val gustText = if (gustVal != null) activity.getString(R.string.gusts_secondary_format, gustVal) else null

        val aqi = current?.airQualityIndex ?: day.airQualityIndex
        val aqiStr = if (aqi != null) "$aqi • ${UnitUtils.getAqiDescription(aqi)}" else "Good"

        val humidity = current?.relativeHumidity?.toInt() ?: day.humidityPct
        val humidityStr = activity.getString(R.string.humidity_label_format, humidity)

        val uv = current?.uvIndex ?: day.uvIndexMax
        val uvStr = String.format(java.util.Locale.getDefault(), "%.1f • %s", uv, UnitUtils.getUvDescription(uv))

        val visibility = current?.visibilityMeters ?: day.visibilityMeters
        val visStr = UnitUtils.formatVisibility(visibility, unitSystem)

        binding.root.findViewById<android.widget.TextView>(R.id.tvWindSpeed)?.text = windSpeedStr
        binding.root.findViewById<android.widget.TextView>(R.id.tvWindGusts)?.text = gustText
        binding.root.findViewById<android.widget.TextView>(R.id.tvAqi)?.text = aqiStr
        binding.root.findViewById<android.widget.TextView>(R.id.tvHumidity)?.text = humidityStr
        binding.root.findViewById<android.widget.TextView>(R.id.tvUvIndex)?.text = uvStr
        binding.root.findViewById<android.widget.TextView>(R.id.tvVisibility)?.text = visStr


        // Solar Summary
        val zoneId = astronomyManager.resolveZoneId(locationTimezone)
        val solar = day.solarSummary

        val srTimeStr = astronomyManager.formatInstant(solar?.sunrise, zoneId, is24Hour)
        val ssTimeStr = astronomyManager.formatInstant(solar?.sunset, zoneId, is24Hour)

        binding.root.findViewById<android.widget.TextView>(R.id.tvSunrise)?.text = if (solar?.sunrise != null) srTimeStr else "No Sunrise"
        binding.root.findViewById<android.widget.TextView>(R.id.tvSunset)?.text = if (solar?.sunset != null) ssTimeStr else "No Sunset"

        val srAz = solar?.sunriseAzimuthDegrees
        val ssAz = solar?.sunsetAzimuthDegrees
        binding.root.findViewById<android.widget.TextView>(R.id.tvSunriseAzimuth)?.text = if (srAz != null) activity.getString(R.string.label_rising_at, "$srAz° (${UnitUtils.getDegreesToCompass(srAz.toDouble())})") else activity.getString(R.string.label_rising_at, "—")
        binding.root.findViewById<android.widget.TextView>(R.id.tvSunsetAzimuth)?.text = if (ssAz != null) activity.getString(R.string.label_setting_at, "$ssAz° (${UnitUtils.getDegreesToCompass(ssAz.toDouble())})") else activity.getString(R.string.label_setting_at, "—")

        val durationMins = solar?.daylightDurationMinutes
        if (durationMins != null) {
            val h = durationMins / 60
            val m = durationMins % 60
            binding.root.findViewById<android.widget.TextView>(R.id.tvTotalDaylight)?.text = "${h}h ${String.format(Locale.getDefault(), "%02dm", m)}"
        } else {
            binding.root.findViewById<android.widget.TextView>(R.id.tvTotalDaylight)?.text = "——"
        }

        // Single reference Instant and single Observer for Lunar calculations
        val targetInstant = if (isToday) Instant.now() else day.date.atStartOfDay(zoneId).toInstant()
        val lunarObserver = LunarObserver(currentLocation.latitude, currentLocation.longitude)

        val moonInfo = LunarCalculator.phaseAt(targetInstant)
        val formattedPhase = LunarCalculator.formatPhaseDisplay(moonInfo, activity)

        binding.root.findViewById<android.widget.TextView>(R.id.tvMoonPhase)?.text = formattedPhase.phaseNameWithEmoji
        binding.root.findViewById<android.widget.TextView>(R.id.tvMoonCyclePercentage)?.text = formattedPhase.illuminationText

        binding.root.findViewById<android.view.View>(R.id.layoutMetricMoon)?.setOnClickListener {
            showMoonPhaseDetailDialog(targetInstant, lunarObserver, zoneId)
        }
    }

    private fun showMoonPhaseDetailDialog(targetInstant: Instant, observer: LunarObserver, zoneId: ZoneId) {
        if (activity.isFinishing || activity.isDestroyed) return

        val palette = ColorPalette.forMode(locationPreferences.getThemeMode(), activity)

        val currMoonInfo = LunarCalculator.phaseAt(targetInstant)
        val formatted = LunarCalculator.formatPhaseDisplay(currMoonInfo, activity)

        val startUtc = Instant.now()
        val endUtc = startUtc.plus(30, ChronoUnit.DAYS)
        val upcomingPhases = LunarCalculator.phaseEventsBetween(startUtc, endUtc)

        val formatter = DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a", Locale.getDefault())
        val upcomingLines = upcomingPhases.take(4).map { ev ->
            val localTimeStr = ev.instantUtc.atZone(zoneId).format(formatter)
            val phaseNameStr = "${ev.type.getEmoji()} ${activity.getString(ev.type.getNameRes())}"
            "• $phaseNameStr: $localTimeStr"
        }

        val todayDate = targetInstant.atZone(zoneId).toLocalDate()
        val moonEvents = LunarCalculator.moonEventsForLocalDate(todayDate, zoneId, observer)
        val riseEvent = moonEvents.find { it.type == MoonEventType.MOONRISE }
        val setEvent = moonEvents.find { it.type == MoonEventType.MOONSET }

        val riseStr = if (riseEvent != null) {
            val t = riseEvent.instantUtc.atZone(zoneId).format(formatter)
            "$t (${riseEvent.azimuthDegrees.toInt()}° ${UnitUtils.getDegreesToCompass(riseEvent.azimuthDegrees)})"
        } else activity.getString(R.string.moon_no_rise)

        val setStr = if (setEvent != null) {
            val t = setEvent.instantUtc.atZone(zoneId).format(formatter)
            "$t (${setEvent.azimuthDegrees.toInt()}° ${UnitUtils.getDegreesToCompass(setEvent.azimuthDegrees)})"
        } else activity.getString(R.string.moon_no_set)

        val dayDataList = com.weatherapp.libre.almanac.MoonAlmanacManager.getMoonAlmanac(
            startDate = todayDate,
            daysForward = 1,
            location = currentLocation,
            zoneId = zoneId,
            hourlyCloudCoverMap = astronomyManager.currentHourlyCloudCoverMap
        )
        val dayData = dayDataList.firstOrNull()

        val atmosphericLines = mutableListOf<String>()
        if (dayData != null) {
            if (dayData.moonbowProbability > 0.6) {
                val pct = kotlin.math.round(dayData.moonbowProbability * 100).toInt()
                atmosphericLines.add(activity.getString(R.string.lunar_moonbow_likely, pct))
            }
            if (dayData.haloProbability > 0.6) {
                val pct = kotlin.math.round(dayData.haloProbability * 100).toInt()
                atmosphericLines.add(activity.getString(R.string.lunar_halo_possible, pct))
            }
            if (dayData.coronaProbability > 0.6) {
                val pct = kotlin.math.round(dayData.coronaProbability * 100).toInt()
                atmosphericLines.add(activity.getString(R.string.lunar_corona_possible, pct))
            }
            if (dayData.parseleneProbability > 0.5) {
                val pct = kotlin.math.round(dayData.parseleneProbability * 100).toInt()
                atmosphericLines.add(activity.getString(R.string.lunar_parselene_possible, pct))
            }

            if (atmosphericLines.isEmpty()) {
                atmosphericLines.add(activity.getString(R.string.lunar_atmospheric_unlikely_single))
            }
        }

        val baseLines = mutableListOf(
            activity.getString(R.string.moon_phase_label, formatted.phaseNameWithEmoji),
            activity.getString(R.string.moon_illumination_label, formatted.illuminationText),
            activity.getString(R.string.moon_age_label, String.format(Locale.US, "%.1f", currMoonInfo.ageDays)),
            activity.getString(R.string.moon_distance_label, String.format(Locale.US, "%,.0f", currMoonInfo.geocentricDistanceKm)),
            "",
            activity.getString(R.string.moonrise_label, riseStr),
            activity.getString(R.string.moonset_label, setStr)
        )

        if (atmosphericLines.isNotEmpty()) {
            baseLines.add("")
            baseLines.addAll(atmosphericLines)
        }

        baseLines.add("")
        baseLines.add(activity.getString(R.string.upcoming_moon_phases_label))

        val message = (baseLines + upcomingLines).joinToString("\n")

        val density = activity.resources.displayMetrics.density

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(activity.getString(R.string.title_current_moon_phase))
            .setMessage(message)
            .setPositiveButton(activity.getString(R.string.btn_close), null)
            .create()
        SystemBarContrastHelper.applyDialogEdgeToEdge(dialog)

        dialog.setOnShowListener {
            val shapeDrawable = com.google.android.material.shape.MaterialShapeDrawable(
                com.google.android.material.shape.ShapeAppearanceModel.builder()
                    .setAllCornerSizes(16 * density)
                    .build()
            ).apply {
                fillColor = ColorStateList.valueOf(palette.surfaceCard)
            }
            dialog.window?.setBackgroundDrawable(shapeDrawable)

            dialog.findViewById<android.widget.TextView>(androidx.appcompat.R.id.alertTitle)?.setTextColor(palette.textPrimary)
            dialog.findViewById<android.widget.TextView>(android.R.id.message)?.setTextColor(palette.textPrimary)
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)?.setTextColor(palette.brandPrimary)
        }

        dialog.show()
    }

    private fun extractHourlyCloudCoverMap(response: WeatherResponse): Map<Instant, Double>? {
        val h = response.hourly ?: return null
        val times = h.time ?: return null
        val covers = h.cloudCover ?: return null
        val zoneId = astronomyManager.resolveZoneId(response.timezone)

        val map = mutableMapOf<Instant, Double>()
        val count = minOf(times.size, covers.size)
        for (i in 0 until count) {
            val tStr = times[i]
            val cPct = covers[i]
            if (cPct != null) {
                try {
                    val inst = java.time.LocalDateTime.parse(tStr).atZone(zoneId).toInstant()
                    map[inst] = cPct / 100.0
                } catch (e: Exception) {
                    Log.w("WeatherDisplayManager", "Failed to parse hourly cloud cover time string: $tStr", e)
                }
            }
        }
        return map.ifEmpty { null }
    }

    fun setupHeroCardClickListener() {
        val heroView = binding.root.findViewById<View>(R.id.layoutHeroSection) ?: return
        heroView.setOnClickListener {
            val response = currentResponse
            val fetchedAt = response?.fetchedAtEpochMillis ?: 0L
            val activeModel = locationPreferences.getActiveModelForLocation(currentLocation)
            val sourceModel = response?.sourceModelName?.ifEmpty { activeModel.idKey } ?: activeModel.idKey

            val formattedDateStr = if (fetchedAt > 0L) {
                val zoneId = try {
                    if (!response?.timezone.isNullOrEmpty()) ZoneId.of(response.timezone)
                    else ZoneId.systemDefault()
                } catch (e: Exception) {
                    ZoneId.systemDefault()
                }
                val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.getDefault())
                Instant.ofEpochMilli(fetchedAt).atZone(zoneId).format(formatter)
            } else {
                "Unknown"
            }

            val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())
            val density = activity.resources.displayMetrics.density

            val dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.title_data_information))
                .setMessage(activity.getString(R.string.msg_data_information, formattedDateStr, sourceModel))
                .setPositiveButton(activity.getString(R.string.btn_ok), null)
                .create()
            SystemBarContrastHelper.applyDialogEdgeToEdge(dialog)

            dialog.setOnShowListener {
                val shapeDrawable = com.google.android.material.shape.MaterialShapeDrawable(
                    com.google.android.material.shape.ShapeAppearanceModel.builder()
                        .setAllCornerSizes(16 * density)
                        .build()
                ).apply {
                    fillColor = ColorStateList.valueOf(palette.surfaceCard)
                }
                dialog.window?.setBackgroundDrawable(shapeDrawable)

                dialog.findViewById<android.widget.TextView>(androidx.appcompat.R.id.alertTitle)?.setTextColor(palette.textPrimary)
                dialog.findViewById<android.widget.TextView>(android.R.id.message)?.setTextColor(palette.textSecondary)
                dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)?.setTextColor(palette.brandPrimary)
            }

            dialog.show()
        }
    }

    companion object {
        fun shouldShowFeelsLike(isToday: Boolean, apparentTemperature: Double?): Boolean {
            return isToday && apparentTemperature != null
        }
    }
}
