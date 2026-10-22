package com.weatherapp.libre.alerts.utils

import java.util.Locale

object CountryCodeMapper {

    private val COUNTRY_NAME_TO_ISO: Map<String, String> = mapOf(
        // MeteoAlarm / Europe
        "albania" to "AL",
        "austria" to "AT",
        "bosnia and herzegovina" to "BA",
        "belgium" to "BE",
        "bulgaria" to "BG",
        "switzerland" to "CH",
        "cyprus" to "CY",
        "czech republic" to "CZ",
        "czechia" to "CZ",
        "germany" to "DE",
        "denmark" to "DK",
        "estonia" to "EE",
        "spain" to "ES",
        "finland" to "FI",
        "france" to "FR",
        "united kingdom" to "GB",
        "great britain" to "GB",
        "uk" to "GB",
        "greece" to "GR",
        "croatia" to "HR",
        "hungary" to "HU",
        "ireland" to "IE",
        "iceland" to "IS",
        "italy" to "IT",
        "lithuania" to "LT",
        "luxembourg" to "LU",
        "latvia" to "LV",
        "montenegro" to "ME",
        "macedonia" to "MK",
        "north macedonia" to "MK",
        "malta" to "MT",
        "netherlands" to "NL",
        "norway" to "NO",
        "poland" to "PL",
        "portugal" to "PT",
        "romania" to "RO",
        "serbia" to "RS",
        "sweden" to "SE",
        "slovenia" to "SI",
        "slovakia" to "SK",

        // Primary Non-EU
        "canada" to "CA",
        "united states" to "US",
        "united states of america" to "US",
        "usa" to "US",
        "australia" to "AU",
        "new zealand" to "NZ"
    )

    fun getIsoCodeForCountryName(countryName: String?): String? {
        if (countryName.isNullOrBlank()) return null
        val cleanName = countryName.trim().lowercase(Locale.US)
        return COUNTRY_NAME_TO_ISO[cleanName]
    }
}
