package com.weatherapp.libre

import com.google.gson.annotations.SerializedName
import java.time.Instant
import java.time.LocalDate

enum class UnitSystem {
    METRIC,
    US_CUSTOMARY
}

data class LocationItem(
    val id: Long = 0L,
    val name: String,
    val admin1: String? = null,
    val country: String? = null,
    val latitude: Double,
    val longitude: Double
) {
    val displayName: String
        get() {
            return when {
                !admin1.isNullOrBlank() -> "$name, $admin1"
                !country.isNullOrBlank() -> "$name, $country"
                else -> name
            }
        }

    val regionAndCountryText: String
        get() {
            val parts = mutableListOf<String>()
            if (!admin1.isNullOrBlank()) parts.add(admin1)
            if (!country.isNullOrBlank()) parts.add(country)
            return if (parts.isNotEmpty()) parts.joinToString(", ") else "Lat: $latitude, Lon: $longitude"
        }
}

data class GeocodingResponse(
    val results: List<GeocodingResult>?
)

data class GeocodingResult(
    val id: Long,
    val name: String,
    val admin1: String?,
    val country: String?,
    val latitude: Double,
    val longitude: Double
)

data class AirQualityResponse(
    val current: AirQualityCurrent?
)

data class AirQualityCurrent(
    @SerializedName("us_aqi")
    val usAqi: Int?
)

data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String?,
    val current: CurrentData?,
    val daily: DailyData?
)

data class CurrentData(
    @SerializedName("temperature_2m")
    var temperature: Double?,
    @SerializedName("relative_humidity_2m")
    var relativeHumidity: Double?,
    @SerializedName("apparent_temperature")
    var apparentTemperature: Double?,
    @SerializedName("weather_code")
    var weatherCode: Int?,
    @SerializedName("is_day")
    var isDay: Int?,
    @SerializedName("wind_speed_10m")
    var windSpeed: Double?,
    @SerializedName("wind_direction_10m")
    var windDirection: Double?,
    @SerializedName("wind_gusts_10m")
    var windGusts: Double? = null,
    @SerializedName("visibility")
    var visibilityMeters: Double?,
    @SerializedName("uv_index")
    var uvIndex: Double?,
    @SerializedName("cloud_cover")
    var cloudCoverPct: Double?,
    var airQualityIndex: Int? = null
)

data class DailyData(
    var time: List<String>?,
    @SerializedName("weather_code")
    var weatherCode: List<Int>?,
    @SerializedName("temperature_2m_max")
    var temperatureMax: List<Double>?,
    @SerializedName("temperature_2m_min")
    var temperatureMin: List<Double>?,
    var sunrise: List<String>?,
    var sunset: List<String>?,
    @SerializedName("uv_index_max")
    var uvIndexMax: List<Double>?,
    @SerializedName("wind_speed_10m_max")
    var windSpeedMax: List<Double>?,
    @SerializedName("wind_gusts_10m_max")
    var windGustsMax: List<Double?>? = null
)

data class SolarDaySummary(
    val sunrise: Instant? = null,
    val sunset: Instant? = null,
    val sunriseAzimuthDegrees: Int? = null,
    val sunsetAzimuthDegrees: Int? = null,
    val morningGoldenHourStart: Instant? = null,
    val morningGoldenHourEnd: Instant? = null,
    val eveningGoldenHourStart: Instant? = null,
    val eveningGoldenHourEnd: Instant? = null,
    val morningBlueHourStart: Instant? = null,
    val morningBlueHourEnd: Instant? = null,
    val eveningBlueHourStart: Instant? = null,
    val eveningBlueHourEnd: Instant? = null,
    val solarNoonElevationDegrees: Int? = null,
    val daylightDurationMinutes: Long? = null
)

data class DayForecast(
    val dateString: String,
    val dayOfWeek: String,
    val formattedDate: String,
    val weatherCode: Int,
    val weatherDescription: String,
    val iconEmoji: String,
    val tempMax: Double,
    val tempMin: Double,
    val windSpeedMaxKmH: Double,
    val windGustsMaxKmH: Double? = null,
    val uvIndexMax: Double,
    val humidityPct: Int,
    val visibilityMeters: Double,
    val cloudCoverPct: Int,
    val airQualityIndex: Int?,
    val solarSummary: SolarDaySummary? = null,
    var isExpanded: Boolean = false
) {
    val date: LocalDate
        get() = try {
            LocalDate.parse(dateString)
        } catch (e: Exception) {
            LocalDate.now()
        }
}
