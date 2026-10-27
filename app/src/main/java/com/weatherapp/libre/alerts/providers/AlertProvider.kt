package com.weatherapp.libre.alerts.providers

import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.models.AlertFetchResult
import com.weatherapp.libre.alerts.models.AlertSource

interface AlertProvider {
    val source: AlertSource
    suspend fun fetchAlertsForLocation(location: LocationItem): AlertFetchResult
}
