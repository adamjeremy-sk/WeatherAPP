package com.weatherapp.libre.alerts

import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.models.CapAlert
import kotlin.math.*

object AlertMatcher {

    enum class MatchRule {
        POLYGON,
        CIRCLE,
        GEOCODE,
        PUBLIC_CANADA,
        NONE
    }

    data class MatchResult(
        val isMatched: Boolean,
        val rule: MatchRule,
        val matchedAreaDesc: String? = null
    )

    fun matchesLocation(alert: CapAlert, location: LocationItem): MatchResult {
        // If alert is expired or cancelled, it doesn't match
        if (alert.infoList.isEmpty()) {
            return MatchResult(false, MatchRule.NONE)
        }

        for (info in alert.infoList) {
            // Check areas
            if (info.areaList.isEmpty()) {
                if (alert.scope.equals("Public", ignoreCase = true)) {
                    val isCanada = location.countryCode.equals("CA", ignoreCase = true) ||
                            location.country.equals("Canada", ignoreCase = true)
                    if (isCanada) {
                        return MatchResult(true, MatchRule.PUBLIC_CANADA, "All Canada (Public Scope)")
                    } else {
                        return MatchResult(false, MatchRule.NONE)
                    }
                }
            }

            for (area in info.areaList) {
                // Rule 1: Point in polygon
                for (polygon in area.polygons) {
                    if (isPointInPolygon(location.latitude, location.longitude, polygon)) {
                        return MatchResult(true, MatchRule.POLYGON, area.areaDesc)
                    }
                }

                // Rule 2: Circle radius (Haversine)
                for (circle in area.circles) {
                    val distKm = haversineKm(
                        location.latitude, location.longitude,
                        circle.centerLat, circle.centerLon
                    )
                    if (distKm <= circle.radiusKm) {
                        return MatchResult(true, MatchRule.CIRCLE, area.areaDesc)
                    }
                }

                // Rule 3: CLC Geocode matching (if location has CLC or name match)
                for (geocode in area.geocodes) {
                    if (geocode.valueName.equals("CLC", ignoreCase = true)) {
                        // Check if areaDesc contains location name or CLC value matches
                        if (area.areaDesc.contains(location.name, ignoreCase = true) ||
                            area.areaDesc.contains(location.displayName, ignoreCase = true)) {
                            return MatchResult(true, MatchRule.GEOCODE, area.areaDesc)
                        }
                    }
                }
            }
        }

        return MatchResult(false, MatchRule.NONE)
    }

    /**
     * Ray-casting point-in-polygon algorithm
     */
    fun isPointInPolygon(lat: Double, lon: Double, polygon: List<Pair<Double, Double>>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val xi = polygon[i].first
            val yi = polygon[i].second
            val xj = polygon[j].first
            val yj = polygon[j].second

            // Inclusive crossing test with a 1e-9 epsilon so points exactly on the polygon
            // edge (or within floating-point ULP noise of it) count as inside, matching the
            // inclusive (<=) semantics of the circle rule.
            val intersect = ((yi > lon) != (yj > lon)) &&
                    (lat <= (xj - xi) * (lon - yi) / (yj - yi) + xi + 1e-9)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    /**
     * Haversine distance in kilometers
     */
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
