package com.weatherapp.libre

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.time.Instant
import java.time.LocalDate

@Keep
enum class UnitSystem {
    METRIC,
    US_CUSTOMARY
}

@Keep
data class LocationItem(
    val id: Long = 0L,
    val name: String,
    val admin1: String? = null,
    val country: String? = null,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double = 0.0
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

@Keep
data class GeocodingResponse(
    val results: List<GeocodingResult>?
)

@Keep
data class GeocodingResult(
    val id: Long,
    val name: String,
    val admin1: String?,
    val country: String?,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = 0.0
)

@Keep
data class AirQualityResponse(
    val current: AirQualityCurrent?
)

@Keep
data class AirQualityCurrent(
    @SerializedName("us_aqi")
    val usAqi: Int?
)

@Keep
data class WeatherResponse(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("elevation")
    val elevation: Double? = 0.0,
    @SerializedName("timezone")
    val timezone: String?,
    @SerializedName("current")
    val current: CurrentData?,
    @SerializedName("daily")
    val daily: DailyData?,
    @SerializedName("hourly")
    var hourly: HourlyData? = null
)

@Keep
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

@Keep
data class DailyData(
    @SerializedName("time")
    var time: List<String>?,
    @SerializedName("weather_code")
    var weatherCode: List<Int>?,
    @SerializedName("temperature_2m_max")
    var temperatureMax: List<Double>?,
    @SerializedName("temperature_2m_min")
    var temperatureMin: List<Double>?,
    @SerializedName("sunrise")
    var sunrise: List<String>?,
    @SerializedName("sunset")
    var sunset: List<String>?,
    @SerializedName("uv_index_max")
    var uvIndexMax: List<Double>?,
    @SerializedName("wind_speed_10m_max")
    var windSpeedMax: List<Double>?,
    @SerializedName("wind_gusts_10m_max")
    var windGustsMax: List<Double?>? = null
)

@Keep
data class HourlyData(
    @SerializedName("time")
    var time: List<String>? = null,
    @SerializedName("temperature_2m")
    var temperature2m: List<Double?>? = null,
    @SerializedName("apparent_temperature")
    var apparentTemperature: List<Double?>? = null,
    @SerializedName("precipitation_probability")
    var precipitationProbability: List<Int?>? = null,
    @SerializedName("precipitation")
    var precipitation: List<Double?>? = null,
    @SerializedName("weather_code")
    var weatherCode: List<Int?>? = null,
    @SerializedName("wind_speed_10m")
    var windSpeed10m: List<Double?>? = null,
    @SerializedName("wind_gusts_10m")
    var windGusts10m: List<Double?>? = null,
    @SerializedName("wind_direction_10m")
    var windDirection10m: List<Double?>? = null,
    @SerializedName("relative_humidity_2m")
    var relativeHumidity2m: List<Int?>? = null,
    @SerializedName("dew_point_2m")
    var dewPoint2m: List<Double?>? = null,
    @SerializedName("cloud_cover")
    var cloudCover: List<Int?>? = null,
    @SerializedName("visibility")
    var visibility: List<Double?>? = null,
    @SerializedName("uv_index")
    var uvIndex: List<Double?>? = null
)

data class HourlyForecast(
    val time: Instant,
    val temperature: Double? = null,
    val apparentTemperature: Double? = null,
    val precipitationProbability: Int? = null,
    val precipitation: Double? = null,
    val weatherCode: Int? = null,
    val windSpeed: Double? = null,
    val windGusts: Double? = null,
    val windDirection: Double? = null,
    val humidity: Int? = null,
    val dewPoint: Double? = null,
    val cloudCover: Int? = null,
    val visibility: Double? = null,
    val uvIndex: Double? = null,
    var isExpanded: Boolean = false
)

sealed class HourlyListItem {
    data class HourRow(
        val forecast: HourlyForecast,
        val isCurrentHour: Boolean = false
    ) : HourlyListItem()

    data class DayDivider(
        val dateLabel: String
    ) : HourlyListItem()

    data class HorizonFooter(
        val message: String = "Hourly data ends here for this model"
    ) : HourlyListItem()
}

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
