package com.weatherapp.libre.alerts.utils

import android.content.Context
import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.LocationPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object CountryCodeResolver {

    suspend fun resolveAndCacheCountryCode(
        context: Context,
        location: LocationItem,
        prefs: LocationPreferences
    ): String? = withContext(Dispatchers.IO) {
        if (!location.countryCode.isNullOrBlank()) {
            return@withContext location.countryCode.uppercase(Locale.US)
        }

        var resolvedCode: String? = null

        val countryText = location.country?.trim()
        if (countryText != null && countryText.length == 2 && countryText.all { it.isLetter() }) {
            resolvedCode = countryText.uppercase(Locale.US)
        } else {
            resolvedCode = CountryCodeMapper.getIsoCodeForCountryName(countryText)
        }

        if (!resolvedCode.isNullOrBlank()) {
            val updatedLocation = location.copy(countryCode = resolvedCode)
            val tabCount = prefs.getTabCount()
            for (i in 0 until tabCount) {
                val tabLoc = prefs.getTabLocation(i)
                if (tabLoc.latitude == location.latitude && tabLoc.longitude == location.longitude) {
                    prefs.saveTabLocation(i, updatedLocation)
                    break
                }
            }
            return@withContext resolvedCode
        }

        return@withContext null
    }
}
