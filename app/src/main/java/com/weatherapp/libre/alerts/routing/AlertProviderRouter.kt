package com.weatherapp.libre.alerts.routing

import com.weatherapp.libre.alerts.providers.AlertProvider
import com.weatherapp.libre.alerts.providers.EcccProvider
import com.weatherapp.libre.alerts.providers.MeteoAlarmProvider
import java.util.Locale

object AlertProviderRouter {

    val METEOALARM_COUNTRY_CODES: Set<String> = setOf(
        "AL", "AT", "BA", "BE", "BG", "CH", "CY", "CZ", "DE", "DK",
        "EE", "ES", "FI", "FR", "GB", "GR", "HR", "HU", "IE", "IS",
        "IT", "LT", "LU", "LV", "ME", "MK", "MT", "NL", "NO", "PL",
        "PT", "RO", "RS", "SE", "SI", "SK", "UK"
    )

    fun getProviderForCountryCode(countryCode: String?): AlertProvider? {
        if (countryCode == null) return null
        val cleanCode = countryCode.trim().uppercase(Locale.US)
        return when {
            cleanCode == "CA" || cleanCode == "CAN" -> EcccProvider()
            METEOALARM_COUNTRY_CODES.contains(cleanCode) -> MeteoAlarmProvider()
            else -> null
        }
    }
}
