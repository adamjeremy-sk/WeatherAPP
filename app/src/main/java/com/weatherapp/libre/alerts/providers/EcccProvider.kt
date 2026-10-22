package com.weatherapp.libre.alerts.providers

import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.AlertMatcher
import com.weatherapp.libre.alerts.CapXmlParser
import com.weatherapp.libre.alerts.models.AlertSeverity
import com.weatherapp.libre.alerts.models.AlertSource
import com.weatherapp.libre.alerts.models.CapAlert
import com.weatherapp.libre.alerts.models.UnifiedAlert
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

class EcccProvider(
    private val httpClient: OkHttpClient = defaultHttpClient()
) : AlertProvider {

    override val source: AlertSource = AlertSource.ECCC

    companion object {
        const val DATAMART_BASE_URL = "https://dd.weather.gc.ca/alerts/cap/"

        val PROVINCE_TO_ECCC_CODE = mapOf(
            "Ontario" to "CWTO",
            "ON" to "CWTO",
            "Quebec" to "CWUL",
            "QC" to "CWUL",
            "Alberta" to "CWAO",
            "AB" to "CWAO",
            "British Columbia" to "CWVR",
            "BC" to "CWVR",
            "Saskatchewan" to "CWRK",
            "SK" to "CWRK",
            "Manitoba" to "CWRN",
            "MB" to "CWRN",
            "Nova Scotia" to "CWHX",
            "NS" to "CWHX",
            "New Brunswick" to "CWHN",
            "NB" to "CWHN",
            "Newfoundland and Labrador" to "CWNT",
            "NL" to "CWNT",
            "Prince Edward Island" to "CWHX",
            "PE" to "CWHX",
            "PEI" to "CWHX",
            "Yukon" to "CWYK",
            "YT" to "CWYK",
            "Northwest Territories" to "CWNT",
            "NT" to "CWNT",
            "Nunavut" to "CWNT",
            "NU" to "CWNT"
        )

        private fun defaultHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
        }
    }

    override suspend fun fetchAlertsForLocation(location: LocationItem): List<UnifiedAlert> {
        return try {
            val ecccCode = resolveEcccCode(location) ?: return emptyList()
            val dateStr = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            val regionUrl = "$DATAMART_BASE_URL$dateStr/$ecccCode/"

            val request = Request.Builder().url(regionUrl).build()
            val htmlBody = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                response.body?.string()
            } ?: return emptyList()

            val capXmlUrls = collectCapXmlUrls(regionUrl, htmlBody)
            val matchedAlerts = mutableListOf<UnifiedAlert>()

            for (capUrl in capXmlUrls) {
                try {
                    val capReq = Request.Builder().url(capUrl).build()
                    httpClient.newCall(capReq).execute().use { capResp ->
                        if (capResp.isSuccessful) {
                            val xml = capResp.body?.string() ?: return@use
                            val capAlert = CapXmlParser.parse(xml) ?: return@use
                            val matchResult = AlertMatcher.matchesLocation(capAlert, location)
                            if (matchResult.isMatched) {
                                val unifiedAlert = mapCapAlertToUnifiedAlert(capAlert, location, matchResult.matchedAreaDesc)
                                if (unifiedAlert != null) {
                                    matchedAlerts.add(unifiedAlert)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Skip failed individual document fetch/parse
                }
            }

            matchedAlerts
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun resolveEcccCode(location: LocationItem): String? {
        val admin1 = location.admin1?.trim()
        if (!admin1.isNullOrBlank()) {
            val exactMatch = PROVINCE_TO_ECCC_CODE[admin1]
            if (exactMatch != null) return exactMatch

            val ignoreCaseMatch = PROVINCE_TO_ECCC_CODE.entries.firstOrNull { it.key.equals(admin1, ignoreCase = true) }?.value
            if (ignoreCaseMatch != null) return ignoreCaseMatch
        }

        // Fallback search in displayName
        val displayName = location.displayName
        for ((key, value) in PROVINCE_TO_ECCC_CODE) {
            if (displayName.contains(key, ignoreCase = true)) {
                return value
            }
        }

        // Fallback for Canada general
        if (location.countryCode.equals("CA", ignoreCase = true) || location.country.equals("Canada", ignoreCase = true)) {
            return "CWNT" // National/territorial fallback hub
        }

        return null
    }

    private fun collectCapXmlUrls(regionUrl: String, htmlBody: String): List<String> {
        val fileUrls = mutableListOf<String>()

        // Direct CAP/XML files in region directory
        val capFileRegex = Regex("""href=["']([^"']+\.(?:cap|xml))["']""", RegexOption.IGNORE_CASE)
        for (match in capFileRegex.findAll(htmlBody)) {
            val path = match.groupValues[1]
            val fullUrl = if (path.startsWith("http")) path else "$regionUrl${path.removePrefix("/")}"
            fileUrls.add(fullUrl)
        }

        // Hourly subdirectories (e.g., "00/", "01/", ..., "23/")
        val hourDirRegex = Regex("""href=["'](\d{2}/?)["']""")
        val hourDirs = hourDirRegex.findAll(htmlBody)
            .map { it.groupValues[1].removeSuffix("/") + "/" }
            .distinct()
            .sortedDescending()

        for (hourDir in hourDirs.take(3)) {
            try {
                val hourUrl = "$regionUrl$hourDir"
                val hourReq = Request.Builder().url(hourUrl).build()
                httpClient.newCall(hourReq).execute().use { hourResp ->
                    if (hourResp.isSuccessful) {
                        val hourBody = hourResp.body?.string() ?: ""
                        for (match in capFileRegex.findAll(hourBody)) {
                            val path = match.groupValues[1]
                            val fullUrl = if (path.startsWith("http")) path else "$hourUrl${path.removePrefix("/")}"
                            fileUrls.add(fullUrl)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore single hourly subfolder fetch failure
            }
        }

        return fileUrls.distinct().take(20)
    }

    private fun mapCapAlertToUnifiedAlert(
        capAlert: CapAlert,
        location: LocationItem,
        matchedAreaDesc: String?
    ): UnifiedAlert? {
        val info = capAlert.infoList.firstOrNull() ?: return null
        val severity = parseCapSeverity(info.severity)

        val eventName = info.event.ifBlank { info.headline ?: "Weather Alert" }
        val headlineText = info.headline ?: "$eventName in effect for ${matchedAreaDesc ?: location.displayName}"

        return UnifiedAlert(
            id = capAlert.identifier,
            source = AlertSource.ECCC,
            locationLat = location.latitude,
            locationLon = location.longitude,
            severity = severity,
            eventNative = eventName,
            headline = headlineText,
            description = info.description,
            instruction = info.instruction,
            onset = info.onset?.toString(),
            expires = info.expires?.toString(),
            areaDesc = matchedAreaDesc ?: info.areaList.firstOrNull()?.areaDesc ?: location.displayName
        )
    }

    private fun parseCapSeverity(severityStr: String?): AlertSeverity {
        return when (severityStr?.lowercase(Locale.US)?.trim()) {
            "extreme" -> AlertSeverity.RED
            "severe" -> AlertSeverity.ORANGE
            "moderate" -> AlertSeverity.YELLOW
            "minor" -> AlertSeverity.UNKNOWN
            else -> AlertSeverity.UNKNOWN
        }
    }
}
