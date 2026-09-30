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

        binding.tvDailyHigh.text = UnitUtils.formatTemp(day.tempMax, unitSystem)
        binding.tvDailyLow.text = UnitUtils.formatTemp(day.tempMin, unitSystem)

        val current = currentWeatherData
        val windSpeed = current?.windSpeed ?: day.windSpeedMaxKmH
        val windDir = current?.windDirection
        val windGust = current?.windGusts ?: day.windGustsMaxKmH

        binding.tvWindSpeed.text = UnitUtils.formatWindSpeedPrimary(windSpeed, unitSystem, windDir)
        val gustText = UnitUtils.formatWindSpeedGusts(windGust, windSpeed, unitSystem)
        if (gustText != null) {
            binding.tvWindGust.text = gustText
            binding.tvWindGust.visibility = View.VISIBLE
        } else {
            binding.tvWindGust.visibility = View.GONE
        }

        val aqi = current?.airQualityIndex ?: day.airQualityIndex
        val aqiStr = if (aqi != null) "$aqi • ${UnitUtils.getAqiDescription(aqi)}" else "Good"
        binding.tvAirQuality.text = aqiStr

        val humidity = current?.relativeHumidity?.toInt() ?: day.humidityPct
        binding.tvHumidity.text = "Humidity: $humidity%"

        val uv = current?.uvIndex ?: day.uvIndexMax
        binding.tvUvIndex.text = String.format(Locale.getDefault(), "%.1f • %s", uv, UnitUtils.getUvDescription(uv))

        val visibility = current?.visibilityMeters ?: day.visibilityMeters
        binding.tvVisibility.text = UnitUtils.formatVisibility(visibility, unitSystem)

        val cloudCover = current?.cloudCoverPct?.toInt() ?: day.cloudCoverPct
        val rawCeiling = UnitUtils.formatCeiling(cloudCover, humidity, unitSystem)
        binding.tvCeiling.text = "Ceiling: $rawCeiling"

        // Solar Summary
        val zoneId = astronomyManager.resolveZoneId(locationTimezone)
        val solar = day.solarSummary

        val srTimeStr = astronomyManager.formatInstant(solar?.sunrise, zoneId, is24Hour)
        val ssTimeStr = astronomyManager.formatInstant(solar?.sunset, zoneId, is24Hour)

        binding.tvSunrise.text = if (solar?.sunrise != null) srTimeStr else "No Sunrise"
        binding.tvSunset.text = if (solar?.sunset != null) ssTimeStr else "No Sunset"

        val srAz = solar?.sunriseAzimuthDegrees
        val ssAz = solar?.sunsetAzimuthDegrees
        binding.tvSunriseAzimuth.text = if (srAz != null) "Rising at $srAz° (${UnitUtils.getDegreesToCompass(srAz.toDouble())})" else "Rising at —"
        binding.tvSunsetAzimuth.text = if (ssAz != null) "Setting at $ssAz° (${UnitUtils.getDegreesToCompass(ssAz.toDouble())})" else "Setting at —"

        val durationMins = solar?.daylightDurationMinutes
        if (durationMins != null) {
            val h = durationMins / 60
            val m = durationMins % 60
            binding.tvTotalDaylight.text = "${h}h ${String.format(Locale.getDefault(), "%02dm", m)}"
        } else {
            binding.tvTotalDaylight.text = "——"
        }

        // Single reference Instant and single Observer for Lunar calculations
        val targetInstant = if (isToday) Instant.now() else day.date.atStartOfDay(zoneId).toInstant()
        val lunarObserver = LunarObserver(currentLocation.latitude, currentLocation.longitude)

        val moonInfo = LunarCalculator.phaseAt(targetInstant, lunarObserver)
        val formattedPhase = LunarCalculator.formatPhaseDisplay(moonInfo)

        binding.tvMoonPhase.text = formattedPhase.phaseNameWithEmoji
        binding.tvMoonCyclePercentage.text = formattedPhase.illuminationText

        binding.layoutMetricGoldenBlueHourContainer.visibility =
            if (locationPreferences.isShowGoldenBlueHourMainPageEnabled()) View.VISIBLE else View.GONE

        // Golden Hour Start & End
        val mgStart = astronomyManager.formatInstant(solar?.morningGoldenHourStart, zoneId, is24Hour)
        val mgEnd = astronomyManager.formatInstant(solar?.morningGoldenHourEnd, zoneId, is24Hour)
        val egStart = astronomyManager.formatInstant(solar?.eveningGoldenHourStart, zoneId, is24Hour)
        val egEnd = astronomyManager.formatInstant(solar?.eveningGoldenHourEnd, zoneId, is24Hour)

        binding.tvMorningGolden.text = "$mgStart – $mgEnd"
        binding.tvEveningGolden.text = "$egStart – $egEnd"

        // Blue Hour Start & End
        val mbStart = astronomyManager.formatInstant(solar?.morningBlueHourStart, zoneId, is24Hour)
        val mbEnd = astronomyManager.formatInstant(solar?.morningBlueHourEnd, zoneId, is24Hour)
        val ebStart = astronomyManager.formatInstant(solar?.eveningBlueHourStart, zoneId, is24Hour)
        val ebEnd = astronomyManager.formatInstant(solar?.eveningBlueHourEnd, zoneId, is24Hour)

        binding.tvMorningBlue.text = "$mbStart – $mbEnd"
        binding.tvEveningBlue.text = "$ebStart – $ebEnd"

        binding.layoutMetricMoon.setOnClickListener {
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
