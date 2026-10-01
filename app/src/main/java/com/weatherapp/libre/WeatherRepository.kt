package com.weatherapp.libre

import com.google.gson.Gson
import com.weatherapp.libre.solar.Observer
import com.weatherapp.libre.solar.SolarLightingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class CachedWeatherEntry(
    val response: WeatherResponse,
    val timestampMillis: Long
)

class WeatherRepository {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val forecastCache = ConcurrentHashMap<String, CachedWeatherEntry>()

    fun clearMemoryAndDiskCache(locationPreferences: LocationPreferences?) {
        forecastCache.clear()
        locationPreferences?.clearPersistentCache()
    }

    fun hasValidCache(
        latitude: Double,
        longitude: Double,
        model: ForecastModel,
        ttlMinutes: Int,
        locationPreferences: LocationPreferences?
    ): Boolean {
        val modelKey = model.openMeteoKey
        val cacheKey = String.format(Locale.US, "cache_%.4f_%.4f_%s", latitude, longitude, modelKey)
        val now = System.currentTimeMillis()
        val ttlMillis = ttlMinutes.toLong() * 60 * 1000L

        val cached = forecastCache[cacheKey]
        if (cached != null && (now - cached.timestampMillis) < ttlMillis) {
            return true
        }

        if (locationPreferences != null) {
            val diskTime = locationPreferences.getPersistentCacheTime(cacheKey)
            if (diskTime > 0L && (now - diskTime) < ttlMillis) {
                val diskJson = locationPreferences.getPersistentCacheJson(cacheKey)
                if (!diskJson.isNullOrEmpty()) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Fetches weather forecast from Open-Meteo API.
     * Serves from memory or persistent disk cache if enabled and fetched less than ttlMinutes ago.
     */
    suspend fun getWeatherForecast(
        latitude: Double,
        longitude: Double,
        model: ForecastModel = ForecastModel.GEM,
        forceRefresh: Boolean = false,
        isCacheEnabled: Boolean = true,
        ttlMinutes: Int = 120,
        locationPreferences: LocationPreferences? = null
    ): Result<WeatherResponse> = withContext(Dispatchers.IO) {
        val modelKey = model.openMeteoKey
        val cacheKey = String.format(Locale.US, "cache_%.4f_%.4f_%s", latitude, longitude, modelKey)
        val now = System.currentTimeMillis()
        val ttlMillis = ttlMinutes.toLong() * 60 * 1000L

        if (!forceRefresh && isCacheEnabled) {
            val cached = forecastCache[cacheKey]
            if (cached != null && (now - cached.timestampMillis) < ttlMillis) {
                return@withContext Result.success(cached.response)
            }

            if (locationPreferences != null) {
                val diskTime = locationPreferences.getPersistentCacheTime(cacheKey)
                if (diskTime > 0L && (now - diskTime) < ttlMillis) {
                    val diskJson = locationPreferences.getPersistentCacheJson(cacheKey)
                    if (!diskJson.isNullOrEmpty()) {
                        try {
                            val response = gson.fromJson(diskJson, WeatherResponse::class.java)
                            if (response != null) {
                                forecastCache[cacheKey] = CachedWeatherEntry(response, diskTime)
                                return@withContext Result.success(response)
                            }
                        } catch (e: Exception) {
                            // Disk JSON parse error fallback
                        }
                    }
                }
            }
        }

        try {
            coroutineScope {
                val forecastDeferred = async { fetchForecastData(latitude, longitude, model) }
                val extraDeferred = async { fetchBestMatchMetrics(latitude, longitude) }
                val aqiDeferred = async { fetchAirQualityData(latitude, longitude) }

                val forecastResult = forecastDeferred.await()
                if (forecastResult.isFailure) {
                    return@coroutineScope forecastResult
                }

                val weatherResponse = forecastResult.getOrThrow()
                val extraResponse = extraDeferred.await().getOrNull()
                val aqiResult = aqiDeferred.await().getOrNull()

                if (weatherResponse.current != null && extraResponse?.current != null) {
                    val curr = weatherResponse.current
                    val extraCurr = extraResponse.current

                    if (aqiResult != null) curr.airQualityIndex = aqiResult
                    if (curr.temperature == null) curr.temperature = extraCurr.temperature
                    if (curr.relativeHumidity == null) curr.relativeHumidity = extraCurr.relativeHumidity
                    if (curr.apparentTemperature == null) curr.apparentTemperature = extraCurr.apparentTemperature
                    if (curr.weatherCode == null) curr.weatherCode = extraCurr.weatherCode
                    if (curr.windSpeed == null) curr.windSpeed = extraCurr.windSpeed
                    if (curr.windDirection == null) curr.windDirection = extraCurr.windDirection
                    if (curr.windGusts == null) curr.windGusts = extraCurr.windGusts
                    if (curr.uvIndex == null) curr.uvIndex = extraCurr.uvIndex
                    if (curr.visibilityMeters == null) curr.visibilityMeters = extraCurr.visibilityMeters
                    if (curr.cloudCoverPct == null) curr.cloudCoverPct = extraCurr.cloudCoverPct
                }

                if (weatherResponse.daily != null && extraResponse?.daily != null) {
                    val d = weatherResponse.daily
                    val exD = extraResponse.daily

                    val maxCount = (exD.time?.size ?: 8).coerceAtLeast(d.time?.size ?: 0)
                    val mergedTimes = MutableList<String>(maxCount) { i -> d.time?.getOrNull(i) ?: exD.time?.getOrNull(i) ?: "" }
                    val mergedCodes = MutableList<Int>(maxCount) { i -> d.weatherCode?.getOrNull(i) ?: exD.weatherCode?.getOrNull(i) ?: 0 }
                    val mergedMax = MutableList<Double>(maxCount) { i -> d.temperatureMax?.getOrNull(i) ?: exD.temperatureMax?.getOrNull(i) ?: 0.0 }
                    val mergedMin = MutableList<Double>(maxCount) { i -> d.temperatureMin?.getOrNull(i) ?: exD.temperatureMin?.getOrNull(i) ?: 0.0 }
                    val mergedUv = MutableList<Double>(maxCount) { i -> d.uvIndexMax?.getOrNull(i) ?: exD.uvIndexMax?.getOrNull(i) ?: 0.0 }
                    val mergedWind = MutableList<Double>(maxCount) { i -> d.windSpeedMax?.getOrNull(i) ?: exD.windSpeedMax?.getOrNull(i) ?: 0.0 }
                    val mergedGusts = MutableList<Double?>(maxCount) { i -> d.windGustsMax?.getOrNull(i) ?: exD.windGustsMax?.getOrNull(i) }
                    val mergedSunrise = MutableList<String>(maxCount) { i -> d.sunrise?.getOrNull(i) ?: exD.sunrise?.getOrNull(i) ?: "" }
                    val mergedSunset = MutableList<String>(maxCount) { i -> d.sunset?.getOrNull(i) ?: exD.sunset?.getOrNull(i) ?: "" }

                    d.time = mergedTimes
                    d.weatherCode = mergedCodes
                    d.temperatureMax = mergedMax
                    d.temperatureMin = mergedMin
                    d.uvIndexMax = mergedUv
                    d.windSpeedMax = mergedWind
                    d.windGustsMax = mergedGusts
                    d.sunrise = mergedSunrise
                    d.sunset = mergedSunset
                }

                forecastCache[cacheKey] = CachedWeatherEntry(weatherResponse, now)
                if (locationPreferences != null) {
                    try {
                        val json = gson.toJson(weatherResponse)
                        locationPreferences.savePersistentCache(cacheKey, json, now)
                    } catch (e: Exception) {
                        // Persistent cache write exception
                    }
                }

                Result.success(weatherResponse)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchForecastData(latitude: Double, longitude: Double, model: ForecastModel): Result<WeatherResponse> {
        return try {
            val baseUrl = model.baseUrl
            val modelApiString = model.openMeteoKey

            val url = "$baseUrl?" +
                    "latitude=$latitude" +
                    "&longitude=$longitude" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,is_day,wind_speed_10m,wind_direction_10m,wind_gusts_10m,visibility,uv_index,cloud_cover" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,wind_speed_10m_max,wind_gusts_10m_max" +
                    "&forecast_days=8" +
                    "&models=$modelApiString" +
                    "&timezone=auto"

            android.util.Log.d("WeatherAPI_Routing", "Requesting Model: ${model.displayName} | Endpoint: $baseUrl | URL: $url")

            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 400 && model == ForecastModel.ECMWF) {
                        android.util.Log.w("WeatherAPI_Routing", "ECMWF returned HTTP 400, falling back to GFS")
                        return fetchForecastData(latitude, longitude, ForecastModel.GFS)
                    }
                    return Result.failure(IOException("HTTP Error: ${response.code}"))
                }
                val bodyString = response.body?.string()
                    ?: return Result.failure(IOException("Empty response body"))

                val weatherResponse = gson.fromJson(bodyString, WeatherResponse::class.java)
                Result.success(weatherResponse)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchBestMatchMetrics(latitude: Double, longitude: Double): Result<WeatherResponse> {
        return try {
            val url = "https://api.open-meteo.com/v1/forecast?" +
                    "latitude=$latitude" +
                    "&longitude=$longitude" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,is_day,wind_speed_10m,wind_direction_10m,wind_gusts_10m,visibility,uv_index,cloud_cover" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,wind_speed_10m_max,wind_gusts_10m_max" +
                    "&forecast_days=8" +
                    "&timezone=auto"

            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IOException("HTTP Error: ${response.code}"))
                }
                val bodyString = response.body?.string() ?: return Result.failure(IOException("Empty extra body"))
                val weatherResponse = gson.fromJson(bodyString, WeatherResponse::class.java)
                Result.success(weatherResponse)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchAirQualityData(latitude: Double, longitude: Double): Result<Int?> {
        return try {
            val url = "https://air-quality-api.open-meteo.com/v1/air-quality?" +
                    "latitude=$latitude&longitude=$longitude&current=us_aqi"

            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IOException("HTTP Error: ${response.code}"))
                }
                val bodyString = response.body?.string() ?: return Result.failure(IOException("Empty AQI body"))
                val aqiResponse = gson.fromJson(bodyString, AirQualityResponse::class.java)
                Result.success(aqiResponse.current?.usAqi)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Searches location using Open-Meteo Geocoding API.
     */
    suspend fun searchLocations(query: String): Result<List<LocationItem>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext Result.success(emptyList())

        try {
            val encodedQuery = java.net.URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encodedQuery&count=10&language=en&format=json"

            val request = Request.Builder().url(url).get().build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use Result.failure(IOException("HTTP Error: ${response.code}"))
                }
                val bodyString = response.body?.string()
                    ?: return@use Result.failure(IOException("Empty response body"))

                val geocodingResponse = gson.fromJson(bodyString, GeocodingResponse::class.java)
                val results = geocodingResponse.results?.map {
                    LocationItem(
                        id = it.id,
                        name = it.name,
                        admin1 = it.admin1,
                        country = it.country,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        elevationMeters = it.elevation ?: 0.0
                    )
                } ?: emptyList()

                Result.success(results)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateSolarSummary(latitude: Double, longitude: Double, elevationMeters: Double, date: LocalDate, zoneId: ZoneId): SolarDaySummary {
        val observer = Observer(latitude, longitude, elevationMeters)
        val lighting = SolarLightingEngine.calculateSolarLightingPhases(date, zoneId, observer)

        val srInstant = lighting.sunrise?.instantUtc
        val ssInstant = lighting.sunset?.instantUtc
        val srAz = lighting.sunrise?.azimuthDegrees?.toInt()
        val ssAz = lighting.sunset?.azimuthDegrees?.toInt()

        val durationMins = if (srInstant != null && ssInstant != null) {
            ChronoUnit.MINUTES.between(srInstant, ssInstant).coerceAtLeast(0L)
        } else null

        return SolarDaySummary(
            sunrise = srInstant,
            sunset = ssInstant,
            sunriseAzimuthDegrees = srAz,
            sunsetAzimuthDegrees = ssAz,
            morningGoldenHourStart = lighting.morningGoldenHour?.startInstantUtc,
            morningGoldenHourEnd = lighting.morningGoldenHour?.endInstantUtc,
            eveningGoldenHourStart = lighting.eveningGoldenHour?.startInstantUtc,
            eveningGoldenHourEnd = lighting.eveningGoldenHour?.endInstantUtc,
            morningBlueHourStart = lighting.morningBlueHour?.startInstantUtc,
            morningBlueHourEnd = lighting.morningBlueHour?.endInstantUtc,
            eveningBlueHourStart = lighting.eveningBlueHour?.startInstantUtc,
            eveningBlueHourEnd = lighting.eveningBlueHour?.endInstantUtc,
            solarNoonElevationDegrees = lighting.solarNoon?.apparentAltitudeDegrees?.toInt(),
            daylightDurationMinutes = durationMins
        )
    }

    /**
     * Processes specifically Today's forecast for cardTodayDetails.
     */
    fun processTodayForecast(
        response: WeatherResponse,
        latitude: Double,
        longitude: Double
    ): DayForecast? {
        val daily = response.daily ?: return null
        val times = daily.time ?: return null
        if (times.isEmpty()) return null

        val current = response.current
        val zoneId = try {
            if (!response.timezone.isNullOrEmpty()) ZoneId.of(response.timezone)
            else ZoneId.systemDefault()
        } catch (e: Exception) {
            ZoneId.systemDefault()
        }

        val dateStr = times[0]
        val date = try {
            LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } catch (e: Exception) {
            LocalDate.now(zoneId)
        }

        val code = daily.weatherCode?.getOrNull(0) ?: current?.weatherCode ?: 0
        val maxTemp = daily.temperatureMax?.getOrNull(0) ?: current?.temperature ?: 0.0
        val minTemp = daily.temperatureMin?.getOrNull(0) ?: current?.temperature ?: 0.0
        val uvMax = daily.uvIndexMax?.getOrNull(0) ?: current?.uvIndex ?: 0.0
        val windMax = daily.windSpeedMax?.getOrNull(0) ?: current?.windSpeed ?: 0.0
        val windGustsMax = daily.windGustsMax?.getOrNull(0) ?: current?.windGusts

        val humidity = current?.relativeHumidity?.toInt() ?: 60
        val visibility = current?.visibilityMeters ?: 10000.0
        val cloudCover = current?.cloudCoverPct?.toInt() ?: 0
        val aqi = current?.airQualityIndex

        val solarSummary = calculateSolarSummary(latitude, longitude, response.elevation ?: 0.0, date, zoneId)

        val desc = WeatherCodeUtils.getWeatherDescription(code)
        val emoji = WeatherCodeUtils.getWeatherEmoji(code, isDay = true)

        return DayForecast(
            dateString = dateStr,
            dayOfWeek = "Today",
            formattedDate = date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())),
            weatherCode = code,
            weatherDescription = desc,
            iconEmoji = emoji,
            tempMax = maxTemp,
            tempMin = minTemp,
            windSpeedMaxKmH = windMax,
            windGustsMaxKmH = windGustsMax,
            uvIndexMax = uvMax,
            humidityPct = humidity,
            visibilityMeters = visibility,
            cloudCoverPct = cloudCover,
            airQualityIndex = aqi,
            solarSummary = solarSummary
        )
    }

    /**
     * Converts raw WeatherResponse into list of DayForecast items for the forecast list (excluding Today).
     * Tomorrow (the first item in the list) is expanded by default.
     */
    fun processDailyForecast(
        response: WeatherResponse,
        latitude: Double,
        longitude: Double
    ): List<DayForecast> {
        val daily = response.daily ?: return emptyList()
        val times = daily.time ?: return emptyList()
        val codes = daily.weatherCode ?: emptyList()
        val maxTemps = daily.temperatureMax ?: emptyList()
        val minTemps = daily.temperatureMin ?: emptyList()
        val uvMaxList = daily.uvIndexMax ?: emptyList()
        val windMaxList = daily.windSpeedMax ?: emptyList()
        val windGustsList = daily.windGustsMax ?: emptyList()

        val current = response.current

        val zoneId = try {
            if (!response.timezone.isNullOrEmpty()) ZoneId.of(response.timezone)
            else ZoneId.systemDefault()
        } catch (e: Exception) {
            ZoneId.systemDefault()
        }

        val inputDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
        val displayDateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

        val list = mutableListOf<DayForecast>()
        val count = times.size.coerceAtMost(8)
        val today = LocalDate.now(zoneId)

        for (i in 0 until count) {
            val dateStr = times[i]
            val date = try {
                LocalDate.parse(dateStr, inputDateFormatter)
            } catch (e: Exception) {
                today.plusDays(i.toLong())
            }

            if (date.isEqual(today) || i == 0) {
                continue
            }

            val dayOfWeekStr = date.format(dayOfWeekFormatter)
            val formattedDateStr = date.format(displayDateFormatter)

            val code = codes.getOrNull(i) ?: 0
            val maxTemp = maxTemps.getOrNull(i) ?: 0.0
            val minTemp = minTemps.getOrNull(i) ?: 0.0

            val uvMax = uvMaxList.getOrNull(i) ?: current?.uvIndex ?: 0.0
            val windMax = windMaxList.getOrNull(i) ?: current?.windSpeed ?: 0.0
            val windGustsMax = windGustsList.getOrNull(i) ?: current?.windGusts

            val humidity = current?.relativeHumidity?.toInt() ?: 60
            val visibility = current?.visibilityMeters ?: 10000.0
            val cloudCover = current?.cloudCoverPct?.toInt() ?: 0
            val aqi = current?.airQualityIndex

            val solarSummary = calculateSolarSummary(latitude, longitude, response.elevation ?: 0.0, date, zoneId)

            val desc = WeatherCodeUtils.getWeatherDescription(code)
            val emoji = WeatherCodeUtils.getWeatherEmoji(code, isDay = true)

            val isExpandedByDefault = (list.isEmpty())

            list.add(
                DayForecast(
                    dateString = dateStr,
                    dayOfWeek = dayOfWeekStr,
                    formattedDate = formattedDateStr,
                    weatherCode = code,
                    weatherDescription = desc,
                    iconEmoji = emoji,
                    tempMax = maxTemp,
                    tempMin = minTemp,
                    windSpeedMaxKmH = windMax,
                    windGustsMaxKmH = windGustsMax,
                    uvIndexMax = uvMax,
                    humidityPct = humidity,
                    visibilityMeters = visibility,
                    cloudCoverPct = cloudCover,
                    airQualityIndex = aqi,
                    solarSummary = solarSummary,
                    isExpanded = isExpandedByDefault
                )
            )
        }

        return list
    }
}
