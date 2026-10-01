package com.weatherapp.libre.alerts

import com.weatherapp.libre.alerts.models.CapAlert
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

interface AlertSourceAdapter {
    fun fetchAlerts(etag: String? = null, lastModified: String? = null): FetchResult
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
        // VERIFIED ECCC DATAMART ENDPOINTS:
        // 1. National CAP Atom Feed: https://dd.weather.gc.ca/alerts/cap/atom/
        // 2. National CAP Directory: https://dd.weather.gc.ca/alerts/cap/
        // 3. ECCC MSC Datamart Alerts Service
        const val ECCC_PRIMARY_ATOM_URL = "https://dd.weather.gc.ca/alerts/cap/atom/"
        const val ECCC_FALLBACK_URL = "https://dd.weather.gc.ca/alerts/cap/"

        private fun defaultHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
        }
    }

    override fun fetchAlerts(etag: String?, lastModified: String?): FetchResult {
        val requestBuilder = Request.Builder().url(ECCC_PRIMARY_ATOM_URL)
        if (!etag.isNullOrBlank()) {
            requestBuilder.header("If-None-Match", etag)
        }
        if (!lastModified.isNullOrBlank()) {
            requestBuilder.header("If-Modified-Since", lastModified)
        }

        try {
            val response = httpClient.newCall(requestBuilder.build()).execute()
            val code = response.code
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

            // Extract individual CAP XML URLs or parse Atom feed entries
            val capXmlUrls = extractCapXmlUrlsFromFeed(bodyString)
            val alerts = mutableListOf<CapAlert>()

            for (capUrl in capXmlUrls.take(20)) { // limit batch
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
