package com.weatherapp.libre

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
import com.weatherapp.libre.lunar.Observer as LunarObserver
import kotlinx.coroutines.CoroutineScope
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

    var currentLocation: LocationItem = LocationItem(0L, "Ottawa", "Ontario", "Canada", 45.4215, -75.6972)
    var locationTimezone: String? = null
    var currentWeatherData: CurrentData? = null
    var selectedDayForecast: DayForecast? = null
    var dailyForecasts: List<DayForecast> = emptyList()
    var forecastAdapter: DayForecastAdapter? = null
    var alertBannerManager: AlertBannerManager? = null
    var metricGridAdapter: MetricGridAdapter? = null
    var hourlyDisplayManager: HourlyDisplayManager? = null

    private val clockHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            if (locationTimezone == null) {
                clockHandler.removeCallbacks(this)
                return
            }

            val nowInstant = Instant.now()
            selectedDayForecast?.let { day ->
                val isToday = day.dayOfWeek.equals("Today", ignoreCase = true)
                if (isToday) {
                    val is24Hour = DateFormat.is24HourFormat(activity)
                    val baseDateStr = day.formattedDate.uppercase(Locale.getDefault())
                    val currentTime = UnitUtils.formatLocationTime(locationTimezone, is24Hour, nowInstant)
                    binding.tvSelectedDateHeader.text = "$baseDateStr • $currentTime • Local Time"
                    astronomyManager.updateInlineGoldenHourDisplay(currentLocation, nowInstant)
                }
            }
            
            alertBannerManager?.updateAlertBanner(currentLocation)
            
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
    }

    fun updateLocationHeader() {
        binding.tvLocationName.text = "${currentLocation.displayName} ▾"
    }

    fun loadWeather(forceRefresh: Boolean = false, onWeatherLoaded: (() -> Unit)? = null) {
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

        lifecycleScope.launch {
            val modelKey = activeModel.openMeteoKey
            binding.tvAttribution.text = activeModel.sourceAttribution

            val result = repository.getWeatherForecast(
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude,
                model = activeModel,
                forceRefresh = forceRefresh,
                isCacheEnabled = isCacheEnabled,
                ttlMinutes = ttlMinutes,
                locationPreferences = locationPreferences
            )

            binding.pbLoading.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false

            result.fold(
                onSuccess = { response ->
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

                    astronomyManager.setLocation(currentLocation, locationTimezone)
                    astronomyManager.updateInlineGoldenHourDisplay(currentLocation)
                    hourlyDisplayManager?.updateHourlyData(response, currentLocation, locationTimezone)

                    updateAlertBanner()
                    onWeatherLoaded?.invoke()
                },
                onFailure = {
                    binding.layoutError.visibility = View.VISIBLE
                    Toast.makeText(activity.applicationContext, "Failed to load weather data", Toast.LENGTH_SHORT).show()
                }
            )
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
            "$baseDateStr • $currentTime • Local Time"
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
        binding.tvMainCondition.text = day.weatherDescription

        val currentTemp = currentWeatherData?.temperature
        if (currentTemp != null && isToday) {
            binding.tvCurrentTemp.text = UnitUtils.formatTempPrecise(currentTemp, unitSystem)
        } else {
            binding.tvCurrentTemp.text = UnitUtils.formatTemp(day.tempMax, unitSystem)
        }
        binding.tvCurrentTemp.visibility = View.VISIBLE

        binding.root.findViewById<android.widget.TextView>(R.id.tvDailyHigh)?.text = UnitUtils.formatTemp(day.tempMax, unitSystem)
        binding.root.findViewById<android.widget.TextView>(R.id.tvDailyLow)?.text = UnitUtils.formatTemp(day.tempMin, unitSystem)

        
        val current = currentWeatherData
        val windSpeed = current?.windSpeed ?: day.windSpeedMaxKmH
        val windDir = current?.windDirection
        val windGust = current?.windGusts ?: day.windGustsMaxKmH

        val windSpeedStr = UnitUtils.formatWindSpeedPrimary(windSpeed, unitSystem, windDir)
        val gustText = UnitUtils.formatWindSpeedGusts(windGust, windSpeed, unitSystem)

        val aqi = current?.airQualityIndex ?: day.airQualityIndex
        val aqiStr = if (aqi != null) "$aqi • ${UnitUtils.getAqiDescription(aqi)}" else "Good"

        val humidity = current?.relativeHumidity?.toInt() ?: day.humidityPct
        val humidityStr = "Humidity: $humidity%"

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
        binding.root.findViewById<android.widget.TextView>(R.id.tvSunriseAzimuth)?.text = if (srAz != null) "Rising at $srAz° (${UnitUtils.getDegreesToCompass(srAz.toDouble())})" else "Rising at —"
        binding.root.findViewById<android.widget.TextView>(R.id.tvSunsetAzimuth)?.text = if (ssAz != null) "Setting at $ssAz° (${UnitUtils.getDegreesToCompass(ssAz.toDouble())})" else "Setting at —"

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

        val moonInfo = LunarCalculator.phaseAt(targetInstant, lunarObserver)
        val formattedPhase = LunarCalculator.formatPhaseDisplay(moonInfo)

        binding.root.findViewById<android.widget.TextView>(R.id.tvMoonPhase)?.text = formattedPhase.phaseNameWithEmoji
        binding.root.findViewById<android.widget.TextView>(R.id.tvMoonCyclePercentage)?.text = formattedPhase.illuminationText

        binding.root.findViewById<android.view.View>(R.id.layoutMetricMoon)?.setOnClickListener {
            showMoonPhaseDetailDialog(targetInstant, lunarObserver, zoneId)
        }
    }

    private fun showMoonPhaseDetailDialog(targetInstant: Instant, observer: LunarObserver, zoneId: ZoneId) {
        if (activity.isFinishing || activity.isDestroyed) return

        val palette = ColorPalette.forMode(locationPreferences.getThemeMode(), activity)

        val currMoonInfo = LunarCalculator.phaseAt(targetInstant, observer)
        val formatted = LunarCalculator.formatPhaseDisplay(currMoonInfo)

        val startUtc = Instant.now()
        val endUtc = startUtc.plus(30, ChronoUnit.DAYS)
        val upcomingPhases = LunarCalculator.phaseEventsBetween(startUtc, endUtc)

        val formatter = DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a", Locale.getDefault())
        val upcomingLines = upcomingPhases.take(4).map { ev ->
            val localTimeStr = ev.instantUtc.atZone(zoneId).format(formatter)
            val phaseNameStr = when (ev.type) {
                MoonPhaseName.NEW_MOON -> "🌑 New Moon"
                MoonPhaseName.FIRST_QUARTER -> "🌓 First Quarter"
                MoonPhaseName.FULL_MOON -> "🌕 Full Moon"
                MoonPhaseName.LAST_QUARTER -> "🌗 Last Quarter"
            }
            "• $phaseNameStr: $localTimeStr"
        }

        val todayDate = targetInstant.atZone(zoneId).toLocalDate()
        val moonEvents = LunarCalculator.moonEventsForLocalDate(todayDate, zoneId, observer)
        val riseEvent = moonEvents.find { it.type == MoonEventType.MOONRISE }
        val setEvent = moonEvents.find { it.type == MoonEventType.MOONSET }

        val riseStr = if (riseEvent != null) {
            val t = riseEvent.instantUtc.atZone(zoneId).format(formatter)
            "$t (${riseEvent.azimuthDegrees.toInt()}° ${UnitUtils.getDegreesToCompass(riseEvent.azimuthDegrees)})"
        } else "Moon does not rise today"

        val setStr = if (setEvent != null) {
            val t = setEvent.instantUtc.atZone(zoneId).format(formatter)
            "$t (${setEvent.azimuthDegrees.toInt()}° ${UnitUtils.getDegreesToCompass(setEvent.azimuthDegrees)})"
        } else "Moon does not set today"

        val message = (listOf(
            "Phase: ${formatted.phaseNameWithEmoji}",
            "Illumination: ${formatted.illuminationText}",
            "Moon Age: ${String.format(Locale.US, "%.1f", currMoonInfo.ageDays)} days",
            "Distance: ${String.format(Locale.US, "%,.0f", currMoonInfo.geocentricDistanceKm ?: 384400.0)} km",
            "",
            "Moonrise: $riseStr",
            "Moonset: $setStr",
            "",
            "Upcoming Moon Phases:"
        ) + upcomingLines).joinToString("\n")

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("Current Moon Phase")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(palette.surfaceCard))
            dialog.findViewById<android.widget.TextView>(androidx.appcompat.R.id.alertTitle)?.setTextColor(palette.textPrimary)
            dialog.findViewById<android.widget.TextView>(android.R.id.message)?.setTextColor(palette.textPrimary)
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)?.setTextColor(palette.brandPrimary)
        }

        dialog.show()
    }
}
