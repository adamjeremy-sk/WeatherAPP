package com.weatherapp.libre.alerts.providers

import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.models.AlertFetchResult
import com.weatherapp.libre.alerts.models.AlertSeverity
import com.weatherapp.libre.alerts.models.AlertSource
import com.weatherapp.libre.alerts.models.UnifiedAlert
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateApi
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateClient
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateFeature
import kotlinx.coroutines.CancellationException
import java.util.Locale

class MeteoAlarmProvider(
    private val api: MeteoGateApi = MeteoGateClient.api
) : AlertProvider {

    override val source: AlertSource = AlertSource.METEOALARM

    override suspend fun fetchAlertsForLocation(location: LocationItem): AlertFetchResult {
        return try {
            val coordsParam = String.format(Locale.US, "POINT(%.5f %.5f)", location.longitude, location.latitude)
            val response = api.getWarningsForPosition(coordsParam)
            val features = response.features ?: emptyList()

            val alerts = features.mapNotNull { feature ->
                parseFeatureToUnifiedAlert(feature, location)
            }
            AlertFetchResult.Success(alerts)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AlertFetchResult.Failure(e)
        }
    }

    private fun parseFeatureToUnifiedAlert(
        feature: MeteoGateFeature,
        location: LocationItem
    ): UnifiedAlert? {
        val props = feature.properties ?: return null
        val severity = parseSeverity(props.awarenessLevel, props.severity) ?: return null

        val eventName = props.event ?: props.phenomenon ?: "Weather Alert"
        val alertId = feature.id ?: "meteoalarm_${location.latitude}_${location.longitude}_${eventName}_${props.onset ?: System.currentTimeMillis()}"

        return UnifiedAlert(
            id = alertId,
            source = AlertSource.METEOALARM,
            locationLat = location.latitude,
            locationLon = location.longitude,
            severity = severity,
            eventNative = eventName,
            headline = props.headline ?: "$eventName in effect for ${props.areaDesc ?: location.displayName}",
            description = props.description,
            instruction = props.instruction,
            onset = props.onset,
            expires = props.expires,
            areaDesc = props.areaDesc ?: props.regionName ?: location.displayName
        )
    }

    private fun parseSeverity(awarenessLevel: String?, severityStr: String?): AlertSeverity? {
        val level = awarenessLevel?.trim() ?: ""
        val sev = severityStr?.trim()?.lowercase(Locale.US) ?: ""

        if (level.startsWith("4") || level.lowercase(Locale.US).contains("level 4") || sev.contains("red") || sev.contains("extreme")) {
            return AlertSeverity.RED
        }
        if (level.startsWith("3") || level.lowercase(Locale.US).contains("level 3") || sev.contains("orange") || sev.contains("severe")) {
            return AlertSeverity.ORANGE
        }
        if (level.startsWith("2") || level.lowercase(Locale.US).contains("level 2") || sev.contains("yellow") || sev.contains("moderate")) {
            return AlertSeverity.YELLOW
        }

        val lowerLevel = level.lowercase(Locale.US)
        return when {
            lowerLevel.contains("red") || lowerLevel.contains("4") -> AlertSeverity.RED
            lowerLevel.contains("orange") || lowerLevel.contains("3") -> AlertSeverity.ORANGE
            lowerLevel.contains("yellow") || lowerLevel.contains("2") -> AlertSeverity.YELLOW
            else -> null
        }
    }
}
