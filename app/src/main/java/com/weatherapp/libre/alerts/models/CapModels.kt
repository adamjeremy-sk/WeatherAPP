package com.weatherapp.libre.alerts.models

import java.time.Instant

data class CapAlert(
    val identifier: String,
    val sender: String,
    val sent: Instant,
    val status: String, // "Actual", "Test", etc.
    val msgType: String, // "Alert", "Update", "Cancel"
    val scope: String = "Public",
    val references: String? = null,
    val infoList: List<CapInfo> = emptyList(),
    val isTestFixture: Boolean = false,
    val isSyntheticTest: Boolean = false
) {
    fun getReferencedIdentifiers(): List<String> {
        if (references.isNullOrBlank()) return emptyList()
        // Format of references is space or comma-separated tuples: "sender,identifier,sent" or just IDs
        return references.split(" ", ",").mapNotNull { part ->
            val trimmed = part.trim()
            if (trimmed.startsWith("urn:oid:")) trimmed
            else if (trimmed.contains("urn:oid:")) {
                trimmed.substring(trimmed.indexOf("urn:oid:"))
            } else null
        }.distinct()
    }
}

data class CapInfo(
    val language: String = "en-CA",
    val category: String = "Met",
    val event: String = "Weather Alert",
    val urgency: String = "Immediate", // Immediate, Expected, Future, Past, Unknown
    val severity: String = "Severe", // Extreme, Severe, Moderate, Minor, Unknown
    val certainty: String = "Observed", // Observed, Likely, Possible, Unlikely, Unknown
    val effective: Instant? = null,
    val onset: Instant? = null,
    val expires: Instant? = null,
    val senderName: String? = null,
    val headline: String? = null,
    val description: String? = null,
    val instruction: String? = null,
    val web: String? = null,
    val areaList: List<CapArea> = emptyList()
)

data class CapArea(
    val areaDesc: String = "",
    val polygons: List<List<Pair<Double, Double>>> = emptyList(),
    val circles: List<CapCircle> = emptyList(),
    val geocodes: List<CapGeocode> = emptyList()
)

data class CapCircle(
    val centerLat: Double,
    val centerLon: Double,
    val radiusKm: Double
)

data class CapGeocode(
    val valueName: String,
    val value: String
)
