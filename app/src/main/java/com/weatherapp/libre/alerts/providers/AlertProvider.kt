package com.weatherapp.libre.alerts.providers

import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.models.AlertSource
import com.weatherapp.libre.alerts.models.UnifiedAlert

interface AlertProvider {
    val source: AlertSource
    suspend fun fetchAlertsForLocation(location: LocationItem): List<UnifiedAlert>
}
