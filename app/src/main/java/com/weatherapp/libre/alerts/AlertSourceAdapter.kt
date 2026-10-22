package com.weatherapp.libre.alerts

import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.models.CapAlert
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

interface AlertSourceAdapter {
    fun fetchAlerts(location: LocationItem? = null, etag: String? = null, lastModified: String? = null): FetchResult
}

data class FetchResult(
    val statusCode: Int,
    val etag: String? = null,
    val lastModified: String? = null,
    val alerts: List<CapAlert> = emptyList(),
    val errorMessage: String? = null
)

class EcccCapSourceAdapter(
    private val httpClient: OkHttpClient = defaultHttpClient()
) : AlertSourceAdapter {

    companion object {
        const val ECCC_PRIMARY_ATOM_URL = "https://dd.weather.gc.ca/alerts/cap/atom.xml"
        const val ECCC_FALLBACK_URL = "https://dd.weather.gc.ca/alerts/cap/"

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

    override fun fetchAlerts(location: LocationItem?, etag: String?, lastModified: String?): FetchResult {
        val province = location?.admin1?.trim()
        val ecccCode = if (!province.isNullOrBlank()) {
            PROVINCE_TO_ECCC_CODE[province]
                ?: PROVINCE_TO_ECCC_CODE.entries.firstOrNull { it.key.equals(province, ignoreCase = true) }?.value
        } else null

        val targetUrl = if (ecccCode != null) {
            "https://dd.weather.gc.ca/alerts/cap/$ecccCode/atom.xml"
        } else {
            ECCC_PRIMARY_ATOM_URL
        }

        val requestBuilder = Request.Builder().url(targetUrl)
        if (!etag.isNullOrBlank()) {
            requestBuilder.header("If-None-Match", etag)
        }
        if (!lastModified.isNullOrBlank()) {
            requestBuilder.header("If-Modified-Since", lastModified)
        }

        try {
            var response = httpClient.newCall(requestBuilder.build()).execute()
            var code = response.code

            // Fallback if regional atom.xml returns 404
            if (code == 404 && targetUrl != ECCC_PRIMARY_ATOM_URL) {
                val fallbackRequest = Request.Builder().url(ECCC_PRIMARY_ATOM_URL).build()
                response = httpClient.newCall(fallbackRequest).execute()
                code = response.code
            }

            val newEtag = response.header("ETag")
            val newLastModified = response.header("Last-Modified")

            if (code == 304) {
                return FetchResult(statusCode = 304, etag = newEtag ?: etag, lastModified = newLastModified ?: lastModified)
            }

            if (!response.isSuccessful) {
                return FetchResult(statusCode = code, errorMessage = "HTTP Error $code: ${response.message}")
            }

            val bodyString = response.body?.string() ?: ""
            if (bodyString.isBlank()) {
                return FetchResult(statusCode = code, etag = newEtag, lastModified = newLastModified, alerts = emptyList())
            }

            val capXmlUrls = extractCapXmlUrlsFromFeed(bodyString)
            val alerts = mutableListOf<CapAlert>()

            for (capUrl in capXmlUrls.take(20)) {
                try {
                    val capRequest = Request.Builder().url(capUrl).build()
                    val capResponse = httpClient.newCall(capRequest).execute()
                    if (capResponse.isSuccessful) {
                        val xml = capResponse.body?.string() ?: ""
                        val alert = CapXmlParser.parse(xml)
                        if (alert != null) {
                            alerts.add(alert)
                        }
                    }
                } catch (e: Exception) {
                    // Skip individual failed document
                }
            }

            return FetchResult(
                statusCode = code,
                etag = newEtag,
                lastModified = newLastModified,
                alerts = alerts
            )
        } catch (e: Exception) {
            return FetchResult(statusCode = -1, errorMessage = e.localizedMessage ?: "Network error")
        }
    }

    private fun extractCapXmlUrlsFromFeed(feedBody: String): List<String> {
        val urls = mutableListOf<String>()
        val regex = Regex("""href=["'](https?://[^"']+\.(?:cap|xml))["']""", RegexOption.IGNORE_CASE)
        for (match in regex.findAll(feedBody)) {
            urls.add(match.groupValues[1])
        }
        return urls.distinct()
    }
}
