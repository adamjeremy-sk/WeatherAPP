package com.weatherapp.libre

import android.text.format.DateFormat
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    var metricGridAdapter: MetricGridAdapter? = null

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
        val isAlertsEnabled = locationPreferences.isWeatherAlertsEnabled()
        if (!isAlertsEnabled) {
            binding.cardAlertBanner.visibility = View.GONE
            return
        }

        val activeAlert = locationPreferences.getActiveWeatherAlert()
        if (activeAlert != null && !locationPreferences.isAlertDismissed(activeAlert.id)) {
            binding.cardAlertBanner.visibility = View.VISIBLE
            binding.tvAlertHeaderTitle.text = "🚨 ${activeAlert.event.uppercase(Locale.getDefault())}"
            binding.tvAlertHeadline.text = activeAlert.headline
            binding.tvAlertInstructions.text = activeAlert.description
            binding.btnDismissAlert.setOnClickListener {
                locationPreferences.dismissAlert(activeAlert.id)
                binding.cardAlertBanner.visibility = View.GONE
            }
        } else {
            binding.cardAlertBanner.visibility = View.GONE
        }
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
        binding.tvMainEmoji.text = day.iconEmoji
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

        val cloudCover = current?.cloudCoverPct?.toInt() ?: day.cloudCoverPct
        val rawCeiling = UnitUtils.formatCeiling(cloudCover, humidity, unitSystem)
        val ceilingStr = "Ceiling: $rawCeiling"
        
        val metrics = mutableListOf<MetricCell>()
        metrics.add(MetricCell(MetricType.WIND, "Wind Speed", windSpeedStr, gustText, R.drawable.ic_wind))
        metrics.add(MetricCell(MetricType.AIR_QUALITY, "Air Quality & Humidity", aqiStr, humidityStr, R.drawable.ic_aqi))
        metrics.add(MetricCell(MetricType.UV, "UV Index", uvStr, null, R.drawable.ic_uv))
        
        if (rawCeiling.equals("Unlimited", ignoreCase = true) || cloudCover < 10) {
            metrics.add(MetricCell(MetricType.VISIBILITY, "Visibility", visStr, null, R.drawable.ic_visibility))
        } else {
            metrics.add(MetricCell(MetricType.VISIBILITY, "Visibility & Ceiling", visStr, ceilingStr, R.drawable.ic_visibility))
        }
        
        metricGridAdapter?.submitList(metrics)


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

        activity.findViewById<android.view.View>(com.weatherapp.libre.R.id.layoutMetricGoldenBlueHourContainer)?.visibility =
            if (locationPreferences.isShowGoldenBlueHourMainPageEnabled()) View.VISIBLE else View.GONE

        // Golden Hour Start & End
        val mgStart = astronomyManager.formatInstant(solar?.morningGoldenHourStart, zoneId, is24Hour)
        val mgEnd = astronomyManager.formatInstant(solar?.morningGoldenHourEnd, zoneId, is24Hour)
        val egStart = astronomyManager.formatInstant(solar?.eveningGoldenHourStart, zoneId, is24Hour)
        val egEnd = astronomyManager.formatInstant(solar?.eveningGoldenHourEnd, zoneId, is24Hour)

        activity.findViewById<android.widget.TextView>(com.weatherapp.libre.R.id.tvMorningGolden)?.text = "$mgStart – $mgEnd"
        activity.findViewById<android.widget.TextView>(com.weatherapp.libre.R.id.tvEveningGolden)?.text = "$egStart – $egEnd"

        // Blue Hour Start & End
        val mbStart = astronomyManager.formatInstant(solar?.morningBlueHourStart, zoneId, is24Hour)
        val mbEnd = astronomyManager.formatInstant(solar?.morningBlueHourEnd, zoneId, is24Hour)
        val ebStart = astronomyManager.formatInstant(solar?.eveningBlueHourStart, zoneId, is24Hour)
        val ebEnd = astronomyManager.formatInstant(solar?.eveningBlueHourEnd, zoneId, is24Hour)

        activity.findViewById<android.widget.TextView>(com.weatherapp.libre.R.id.tvMorningBlue)?.text = "$mbStart – $mbEnd"
        activity.findViewById<android.widget.TextView>(com.weatherapp.libre.R.id.tvEveningBlue)?.text = "$ebStart – $ebEnd"

        binding.root.findViewById<android.view.View>(R.id.layoutMetricMoon)?.setOnClickListener {
            showMoonPhaseDetailDialog(targetInstant, lunarObserver, zoneId)
        }
    }

    private fun showMoonPhaseDetailDialog(targetInstant: Instant, observer: LunarObserver, zoneId: ZoneId) {
        if (activity.isFinishing || activity.isDestroyed) return

        val currMoonInfo = LunarCalculator.phaseAt(targetInstant, observer)
        val formatted = LunarCalculator.formatPhaseDisplay(currMoonInfo)

        val startUtc = Instant.now()
        val endUtc = startUtc.plus(30, ChronoUnit.DAYS)
        val upcomingPhases = LunarCalculator.phaseEventsBetween(startUtc, endUtc)

        val formatter = DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a", Locale.getDefault())
        val upcomingText = upcomingPhases.take(4).joinToString("\n") { ev ->
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

        val message = """
            Phase: ${formatted.phaseNameWithEmoji}
            Illumination: ${formatted.illuminationText}
            Moon Age: ${String.format(Locale.US, "%.1f", currMoonInfo.ageDays)} days
            Distance: ${String.format(Locale.US, "%,.0f", currMoonInfo.geocentricDistanceKm ?: 384400.0)} km
            
            Moonrise: $riseStr
            Moonset: $setStr
            
            Upcoming Moon Phases:
            $upcomingText
        """.trimIndent()

        MaterialAlertDialogBuilder(activity)
            .setTitle("Current Moon Phase")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }
}
