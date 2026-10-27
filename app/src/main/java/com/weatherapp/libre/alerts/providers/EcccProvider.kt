package com.weatherapp.libre.alerts.providers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.Keep
import com.google.gson.Gson
import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.AlertMatcher
import com.weatherapp.libre.alerts.CapXmlParser
import com.weatherapp.libre.alerts.models.AlertFetchResult
import com.weatherapp.libre.alerts.models.AlertSeverity
import com.weatherapp.libre.alerts.models.AlertSource
import com.weatherapp.libre.alerts.models.CapAlert
import com.weatherapp.libre.alerts.models.UnifiedAlert
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

interface EcccMemoStore {
    fun get(key: String): EcccMemoEntry?
    fun put(key: String, entry: EcccMemoEntry)
    fun clear()
}

@Keep
data class EcccMemoEntry(
    val matched: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val alert: UnifiedAlert? = null
)

class InMemoryEcccMemoStore : EcccMemoStore {
    private val memoMap = ConcurrentHashMap<String, EcccMemoEntry>()

    override fun get(key: String): EcccMemoEntry? {
        val entry = memoMap[key] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > 48 * 3600 * 1000L) {
            memoMap.remove(key)
            return null
        }
        return entry
    }

    override fun put(key: String, entry: EcccMemoEntry) {
        purgeOldEntries()
        memoMap[key] = entry
    }

    override fun clear() {
        memoMap.clear()
    }

    private fun purgeOldEntries() {
        val now = System.currentTimeMillis()
        val cutoff = 48 * 3600 * 1000L
        memoMap.entries.removeIf { now - it.value.timestamp > cutoff }
    }
}

object DefaultEcccMemoStore : EcccMemoStore {
    private val memoryStore = InMemoryEcccMemoStore()
    private var prefs: SharedPreferences? = null
    private val gson = Gson()

    fun init(context: Context) {
        synchronized(this) {
            if (prefs == null) {
                try {
                    val appCtx = context.applicationContext ?: context
                    prefs = appCtx.getSharedPreferences("eccc_memo_prefs", Context.MODE_PRIVATE)
                    loadFromPrefs()
                } catch (e: Exception) {
                    // Ignore in test environments with unmocked Context
                }
            }
        }
    }

    private fun loadFromPrefs() {
        val p = prefs ?: return
        val now = System.currentTimeMillis()
        val cutoff = 48 * 3600 * 1000L
        for ((k, v) in p.all) {
            if (k.startsWith("eccc_memo_")) {
                try {
                    val json = v as? String ?: continue
                    val entry = gson.fromJson(json, EcccMemoEntry::class.java)
                    if (now - entry.timestamp <= cutoff) {
                        memoryStore.put(k, entry)
                    }
                } catch (e: Exception) {
                    // Ignore corrupted entry
                }
            }
        }
    }

    override fun get(key: String): EcccMemoEntry? {
        return memoryStore.get(key)
    }

    override fun put(key: String, entry: EcccMemoEntry) {
        memoryStore.put(key, entry)
        val p = prefs ?: return
        try {
            val json = gson.toJson(entry)
            p.edit().putString(key, json).apply()
            purgePrefsOldEntries()
        } catch (e: Exception) {
            Log.w("EcccProvider", "Failed to persist ECCC memo entry for key $key", e)
        }
    }

    override fun clear() {
        memoryStore.clear()
        prefs?.edit()?.clear()?.apply()
    }

    private fun purgePrefsOldEntries() {
        val p = prefs ?: return
        val now = System.currentTimeMillis()
        val cutoff = 48 * 3600 * 1000L
        val editor = p.edit()
        var changed = false
        for ((k, v) in p.all) {
            if (k.startsWith("eccc_memo_")) {
                try {
                    val json = v as? String ?: continue
                    val entry = gson.fromJson(json, EcccMemoEntry::class.java)
                    if (now - entry.timestamp > cutoff) {
                        editor.remove(k)
                        changed = true
                    }
                } catch (e: Exception) {
                    editor.remove(k)
                    changed = true
                }
            }
        }
        if (changed) editor.apply()
    }
}

class EcccProvider(
    private val httpClient: OkHttpClient = defaultHttpClient(),
    private val memoStore: EcccMemoStore = DefaultEcccMemoStore
) : AlertProvider {

    override val source: AlertSource = AlertSource.ECCC

    companion object {
        const val DATAMART_BASE_URL = "https://dd.weather.gc.ca/today/alerts/cap/"

        val PROVINCE_TO_ECCC_CODE = mapOf(
            "Ontario" to "CWTO",
            "ON" to "CWTO",
            "Quebec" to "CWUL",
            "QC" to "CWUL",
            "Alberta" to "CWEG",
            "AB" to "CWEG",
            "British Columbia" to "CWVR",
            "BC" to "CWVR",
            "Saskatchewan" to "CWWG",
            "SK" to "CWWG",
            "Manitoba" to "CWWG",
            "MB" to "CWWG",
            "Nova Scotia" to "CWHX",
            "NS" to "CWHX",
            "New Brunswick" to "CWHX",
            "NB" to "CWHX",
            "Newfoundland and Labrador" to "CWHX",
            "Newfoundland" to "CWHX",
            "NL" to "CWHX",
            "Prince Edward Island" to "CWHX",
            "PE" to "CWHX",
            "PEI" to "CWHX"
        )

        private fun defaultHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
        }
    }

    override suspend fun fetchAlertsForLocation(location: LocationItem): AlertFetchResult {
        return try {
            val ecccCode = resolveEcccCode(location) ?: return AlertFetchResult.Success(emptyList())
            val now = ZonedDateTime.now(ZoneOffset.UTC)
            val todayStr = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            val yesterdayStr = now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            val candidateDates = listOf(todayStr, yesterdayStr)

            val capXmlUrls = collectCapXmlUrls(ecccCode, candidateDates, now).distinctBy { it.substringAfterLast("/") }
            val matchedAlerts = mutableListOf<UnifiedAlert>()

            for (capUrl in capXmlUrls) {
                val filename = capUrl.substringAfterLast("/")
                val memoKey = "eccc_memo_${ecccCode}_${filename}"

                val cachedEntry = memoStore.get(memoKey)
                if (cachedEntry != null) {
                    if (cachedEntry.matched && cachedEntry.alert != null) {
                        if (matchedAlerts.none { it.id == cachedEntry.alert.id }) {
                            matchedAlerts.add(cachedEntry.alert)
                        }
                    }
                    continue
                }

                try {
                    val capReq = Request.Builder().url(capUrl).build()
                    httpClient.newCall(capReq).execute().use { capResp ->
                        if (capResp.isSuccessful) {
                            val xml = capResp.body?.string()
                            if (xml != null) {
                                val capAlert = CapXmlParser.parse(xml)
                                if (capAlert != null) {
                                    val matchResult = AlertMatcher.matchesLocation(capAlert, location)
                                    if (matchResult.isMatched) {
                                        val unifiedAlert = mapCapAlertToUnifiedAlert(capAlert, location, matchResult.matchedAreaDesc)
                                        memoStore.put(memoKey, EcccMemoEntry(matched = true, alert = unifiedAlert))
                                        if (unifiedAlert != null && matchedAlerts.none { it.id == unifiedAlert.id }) {
                                            matchedAlerts.add(unifiedAlert)
                                        }
                                    } else {
                                        memoStore.put(memoKey, EcccMemoEntry(matched = false))
                                    }
                                } else {
                                    memoStore.put(memoKey, EcccMemoEntry(matched = false))
                                }
                            } else {
                                memoStore.put(memoKey, EcccMemoEntry(matched = false))
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Skip failed individual document fetch/parse
                }
            }

            AlertFetchResult.Success(matchedAlerts)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AlertFetchResult.Failure(e)
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

        return null
    }

    private fun collectCapXmlUrls(
        ecccCode: String,
        candidateDates: List<String>,
        now: ZonedDateTime
    ): List<String> {
        val fileUrls = mutableListOf<String>()
        val capFileRegex = Regex("""href=["']([^"']+\.(?:cap|xml))["']""", RegexOption.IGNORE_CASE)
        val hourDirRegex = Regex("""href=["'](\d{2}/?)["']""")
        val cutoff = now.minusHours(24)

        var atLeastOneSuccess = false
        var lastException: Exception? = null

        for (dateStr in candidateDates) {
            val regionUrl = "$DATAMART_BASE_URL$dateStr/$ecccCode/"
            try {
                val request = Request.Builder().url(regionUrl).build()
                val htmlBody = httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        if (response.code != 404) {
                            lastException = IOException("HTTP ${response.code} fetching ECCC alerts")
                        } else {
                            atLeastOneSuccess = true
                        }
                        return@use null
                    }
                    atLeastOneSuccess = true
                    response.body?.string()
                } ?: continue

                // Direct CAP/XML files in region directory (if any)
                for (match in capFileRegex.findAll(htmlBody)) {
                    val path = match.groupValues[1]
                    val fullUrl = if (path.startsWith("http")) path else "$regionUrl${path.removePrefix("/")}"
                    fileUrls.add(fullUrl)
                }

                // Hourly subdirectories (e.g., "00/", "01/", ..., "23/")
                val hourDirs = hourDirRegex.findAll(htmlBody)
                    .map { it.groupValues[1].removeSuffix("/") }
                    .distinct()

                val year = dateStr.substring(0, 4).toIntOrNull() ?: continue
                val month = dateStr.substring(4, 6).toIntOrNull() ?: continue
                val day = dateStr.substring(6, 8).toIntOrNull() ?: continue

                for (hh in hourDirs) {
                    val hour = hh.toIntOrNull() ?: continue
                    val folderTime = try {
                        ZonedDateTime.of(year, month, day, hour, 0, 0, 0, ZoneOffset.UTC)
                    } catch (e: Exception) {
                        continue
                    }

                    val folderEnd = folderTime.plusHours(1)

                    // Filter hourly folders within 24 hours from current UTC time
                    if (folderEnd.isBefore(cutoff) || folderTime.isAfter(now)) {
                        continue
                    }

                    val hourUrl = "$regionUrl$hh/"
                    try {
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
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Ignore single hourly subfolder fetch failure
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastException = e
            }
        }

        val ex = lastException
        if (!atLeastOneSuccess && ex != null) {
            throw ex
        }

        return fileUrls.distinct()
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

        val referencedIds = parseCapReferences(capAlert.references)

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
            areaDesc = matchedAreaDesc ?: info.areaList.firstOrNull()?.areaDesc ?: location.displayName,
            msgType = capAlert.msgType,
            references = referencedIds,
            sent = capAlert.sent.toString(),
            isTest = capAlert.isTestFixture
        )
    }

    private fun parseCapReferences(referencesStr: String?): List<String> {
        if (referencesStr.isNullOrBlank()) return emptyList()
        return referencesStr.trim().split("\\s+".toRegex()).mapNotNull { token ->
            val parts = token.split(",")
            if (parts.size >= 2 && parts[1].isNotBlank()) {
                parts[1].trim()
            } else if (token.isNotBlank()) {
                token.trim()
            } else null
        }.distinct()
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
