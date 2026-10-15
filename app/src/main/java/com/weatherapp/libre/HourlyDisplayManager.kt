package com.weatherapp.libre

import com.weatherapp.theme.ColorPalette
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.Instant
import java.time.ZoneId

class HourlyDisplayManager(
    private val activity: AppCompatActivity,
    private val locationPreferences: LocationPreferences
) {

    var hourlyAdapter: HourlyForecastAdapter = HourlyForecastAdapter()
    private var lastResponse: WeatherResponse? = null
    private var cachedHourlyForecasts: List<HourlyForecast> = emptyList()
    private var locationTimezoneStr: String? = null
    private var currentLocationItem: LocationItem? = null

    fun setupRecyclerView() {
        val rv = activity.findViewById<RecyclerView>(R.id.rvInlineHourly) ?: return
        rv.layoutManager = LinearLayoutManager(activity)
        rv.adapter = hourlyAdapter
    }

    fun updateHourlyData(
        response: WeatherResponse,
        location: LocationItem,
        timezone: String?
    ) {
        lastResponse = response
        currentLocationItem = location
        locationTimezoneStr = timezone

        val rawHourly = response.hourly
        cachedHourlyForecasts = HourlyMapper.mapRawHourly(rawHourly)
        rebuildHourlyList()
    }

    fun rebuildHourlyList() {
        if (cachedHourlyForecasts.isEmpty() && lastResponse?.hourly != null) {
            cachedHourlyForecasts = HourlyMapper.mapRawHourly(lastResponse?.hourly)
        }

        val zoneId = try {
            if (!locationTimezoneStr.isNullOrEmpty()) ZoneId.of(locationTimezoneStr) else ZoneId.systemDefault()
        } catch (e: Exception) {
            ZoneId.systemDefault()
        }

        val unitSystem = locationPreferences.getUnitSystem()
        hourlyAdapter.setUnitSystem(unitSystem)
        hourlyAdapter.submitForecastData(
            forecasts = cachedHourlyForecasts,
            timezone = locationTimezoneStr
        )
    }

    fun setPalette(palette: ColorPalette) {
        hourlyAdapter.setPalette(palette)
    }
}
