package com.weatherapp.libre.alerts.providers

import android.util.Log
import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.AlertMatcher
import com.weatherapp.libre.alerts.models.AlertFetchResult
import com.weatherapp.libre.alerts.models.AlertSeverity
import com.weatherapp.libre.alerts.models.AlertSource
import com.weatherapp.libre.alerts.models.CapArea
import com.weatherapp.libre.alerts.models.CapAlert
import com.weatherapp.libre.alerts.models.CapInfo
import com.weatherapp.libre.alerts.models.UnifiedAlert
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateApi
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateCapParameter
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateClient
import com.weatherapp.libre.alerts.routing.AlertProviderRouter
import com.weatherapp.libre.alerts.utils.CountryCodeMapper
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class MeteoAlarmProvider(
    private val api: MeteoGateApi = MeteoGateClient.api
) : AlertProvider {

    override val source: AlertSource = AlertSource.METEOALARM

    override suspend fun fetchAlertsForLocation(location: LocationItem): AlertFetchResult {
        return try {
            val rawCountryCode = resolveCountryCode(location)
                ?: return AlertFetchResult.Success(emptyList())

            val targetCountryCode = if (rawCountryCode == "GB") "UK" else rawCountryCode

            if (!AlertProviderRouter.METEOALARM_COUNTRY_CODES.contains(targetCountryCode) &&
                !AlertProviderRouter.METEOALARM_COUNTRY_CODES.contains(rawCountryCode)) {
                return AlertFetchResult.Success(emptyList())
            }

            val now = Instant.now()
            val end = now.plus(23, ChronoUnit.HOURS)
            val formatter = DateTimeFormatter.ISO_INSTANT
            val datetimeParam = "${formatter.format(now)}/${formatter.format(end)}"

            val response = api.getWarningsForCountry(targetCountryCode, datetimeParam)
            val features = response.features ?: emptyList()

            val matchedAlerts = mutableListOf<UnifiedAlert>()

            for (feature in features) {
                val links = feature.links ?: emptyList()
                val jsonLink = links.firstOrNull { it.type == "application/json" }?.href
                    ?: continue

                val detailedCap = try {
                    api.getDetailedCapJson(jsonLink)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("MeteoAlarmProvider", "Failed to fetch/parse CAP detail for $jsonLink", e)
                    continue
                }

                val info = detailedCap.info?.firstOrNull() ?: continue
                val severity = parseSeverity(info.parameter, info.severity) ?: continue

                val capAreas = info.area?.map { area ->
                    val polygons = area.polygon?.mapNotNull { polyStr ->
                        parsePolygonString(polyStr)
                    } ?: emptyList()
                    CapArea(
                        areaDesc = area.areaDesc ?: "",
                        polygons = polygons
                    )
                } ?: emptyList()

                val referencesList = detailedCap.references?.trim()?.split("\\s+".toRegex())?.mapNotNull { token ->
                    val parts = token.split(",")
                    if (parts.size >= 2 && parts[1].isNotBlank()) parts[1].trim()
                    else if (token.isNotBlank()) token.trim()
                    else null
                }?.distinct() ?: emptyList()

                val capAlert = CapAlert(
                    identifier = detailedCap.identifier ?: feature.id ?: "",
                    sender = detailedCap.sender ?: "MeteoAlarm",
                    sent = parseInstant(detailedCap.sent),
                    status = "Actual",
                    msgType = detailedCap.msgType ?: "Alert",
                    references = detailedCap.references,
                    infoList = listOf(
                        CapInfo(
                            language = info.language ?: "en",
                            event = info.event ?: info.headline ?: "Weather Alert",
                            severity = info.severity ?: "Unknown",
                            headline = info.headline,
                            description = info.description,
                            instruction = info.instruction,
                            onset = parseInstant(info.onset),
                            expires = parseInstant(info.expires),
                            areaList = capAreas
                        )
                    )
                )

                val matchResult = AlertMatcher.matchesLocation(capAlert, location)
                if (matchResult.isMatched) {
                    val eventName = info.event ?: info.headline ?: "Weather Alert"
                    val areaName = matchResult.matchedAreaDesc
                        ?: info.area?.firstOrNull()?.areaDesc
                        ?: location.displayName
                    val alertId = detailedCap.identifier
                        ?: feature.id
                        ?: "meteoalarm_${location.latitude}_${location.longitude}_${eventName}"

                    matchedAlerts.add(
                        UnifiedAlert(
                            id = alertId,
                            source = AlertSource.METEOALARM,
                            locationLat = location.latitude,
                            locationLon = location.longitude,
                            severity = severity,
                            eventNative = eventName,
                            headline = info.headline ?: "$eventName in effect for $areaName",
                            description = info.description,
                            instruction = info.instruction,
                            onset = info.onset,
                            expires = info.expires,
                            areaDesc = areaName,
                            msgType = detailedCap.msgType ?: "Alert",
                            references = referencesList,
                            sent = detailedCap.sent,
                            isTest = false
                        )
                    )
                }
            }

            AlertFetchResult.Success(matchedAlerts.distinctBy { it.id })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AlertFetchResult.Failure(e)
        }
    }

    private fun resolveCountryCode(location: LocationItem): String? {
        val directCode = location.countryCode?.trim()
        if (!directCode.isNullOrBlank()) {
            return directCode.uppercase(Locale.US)
        }

        val countryName = location.country?.trim()
        if (!countryName.isNullOrBlank()) {
            if (countryName.length == 2 && countryName.all { it.isLetter() }) {
                return countryName.uppercase(Locale.US)
            }
            val mappedCode = CountryCodeMapper.getIsoCodeForCountryName(countryName)
            if (!mappedCode.isNullOrBlank()) {
                return mappedCode.uppercase(Locale.US)
            }
        }

        return null
    }

    private fun parsePolygonString(polyStr: String): List<Pair<Double, Double>>? {
        val tokens = polyStr.trim().split("\\s+".toRegex())
        if (tokens.size < 3) return null
        val points = mutableListOf<Pair<Double, Double>>()
        for (token in tokens) {
            val parts = token.split(",")
            if (parts.size == 2) {
                val lat = parts[0].toDoubleOrNull()
                val lon = parts[1].toDoubleOrNull()
                if (lat != null && lon != null) {
                    points.add(Pair(lat, lon))
                }
            }
        }
        return if (points.size >= 3) points else null
    }

    private fun parseInstant(isoStr: String?): Instant {
        if (isoStr.isNullOrBlank()) return Instant.now()
        return try {
            ZonedDateTime.parse(isoStr).toInstant()
        } catch (e: Exception) {
            Instant.now()
        }
    }

    private fun parseSeverity(
        parameters: List<MeteoGateCapParameter>?,
        fallbackSeverityStr: String?
    ): AlertSeverity? {
        val levelParam = parameters?.firstOrNull {
            it.valueName.equals("awareness_level", ignoreCase = true)
        }?.value?.trim()

        if (!levelParam.isNullOrBlank()) {
            val lowerParam = levelParam.lowercase(Locale.US)
            when {
                lowerParam.startsWith("4") || lowerParam.contains("level 4") || lowerParam.contains("red") || lowerParam.contains("extreme") -> {
                    return AlertSeverity.RED
                }
                lowerParam.startsWith("3") || lowerParam.contains("level 3") || lowerParam.contains("orange") || lowerParam.contains("severe") -> {
                    return AlertSeverity.ORANGE
                }
                lowerParam.startsWith("2") || lowerParam.contains("level 2") || lowerParam.contains("yellow") || lowerParam.contains("moderate") -> {
                    return AlertSeverity.YELLOW
                }
                lowerParam.startsWith("1") || lowerParam.contains("level 1") || lowerParam.contains("green") || lowerParam.contains("minor") -> {
                    return null
                }
            }
        }

        val sev = fallbackSeverityStr?.trim()?.lowercase(Locale.US) ?: return null
        return when {
            sev.contains("extreme") || sev.contains("red") -> AlertSeverity.RED
            sev.contains("severe") || sev.contains("orange") -> AlertSeverity.ORANGE
            sev.contains("moderate") || sev.contains("yellow") -> AlertSeverity.YELLOW
            else -> null
        }
    }
}
