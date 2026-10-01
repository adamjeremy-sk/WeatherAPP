package com.weatherapp.libre

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object HourlyMapper {

    fun parseTime(timeStr: String): Instant? {
        return try {
            if (timeStr.endsWith("Z") || timeStr.contains("+") || (timeStr.contains("-") && timeStr.lastIndexOf("-") > 10)) {
                Instant.parse(timeStr)
            } else {
                LocalDateTime.parse(timeStr).toInstant(ZoneOffset.UTC)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun mapRawHourly(hourlyData: HourlyData?): List<HourlyForecast> {
        val times = hourlyData?.time ?: return emptyList()
        val temps = hourlyData.temperature2m
        val apparentTemps = hourlyData.apparentTemperature
        val precipProbs = hourlyData.precipitationProbability
        val precips = hourlyData.precipitation
        val weatherCodes = hourlyData.weatherCode
        val windSpeeds = hourlyData.windSpeed10m
        val windGusts = hourlyData.windGusts10m
        val windDirs = hourlyData.windDirection10m
        val humidities = hourlyData.relativeHumidity2m
        val dewPoints = hourlyData.dewPoint2m
        val cloudCovers = hourlyData.cloudCover
        val visibilities = hourlyData.visibility
        val uvIndices = hourlyData.uvIndex

        val result = mutableListOf<HourlyForecast>()
        for (i in times.indices) {
            val timeStr = times[i]
            val instant = parseTime(timeStr) ?: continue

            result.add(
                HourlyForecast(
                    time = instant,
                    temperature = temps?.getOrNull(i),
                    apparentTemperature = apparentTemps?.getOrNull(i),
                    precipitationProbability = precipProbs?.getOrNull(i),
                    precipitation = precips?.getOrNull(i),
                    weatherCode = weatherCodes?.getOrNull(i),
                    windSpeed = windSpeeds?.getOrNull(i),
                    windGusts = windGusts?.getOrNull(i),
                    windDirection = windDirs?.getOrNull(i),
                    humidity = humidities?.getOrNull(i),
                    dewPoint = dewPoints?.getOrNull(i),
                    cloudCover = cloudCovers?.getOrNull(i),
                    visibility = visibilities?.getOrNull(i),
                    uvIndex = uvIndices?.getOrNull(i)
                )
            )
        }
        return result
    }

    fun build36HourList(
        forecasts: List<HourlyForecast>,
        now: Instant,
        zoneId: ZoneId
    ): List<HourlyListItem> {
        if (forecasts.isEmpty()) return emptyList()

        val anchorHour = now.atZone(ZoneOffset.UTC).truncatedTo(ChronoUnit.HOURS).toInstant()

        val startIndex = forecasts.indexOfFirst { it.time >= anchorHour }
        if (startIndex == -1) return emptyList()

        val sliced = forecasts.subList(startIndex, (startIndex + 36).coerceAtMost(forecasts.size))
        if (sliced.isEmpty()) return emptyList()

        val items = mutableListOf<HourlyListItem>()
        val dividerFormatter = DateTimeFormatter.ofPattern("EEEE • MMM d", Locale.getDefault())

        val firstDate = sliced[0].time.atZone(zoneId).toLocalDate()
        items.add(HourlyListItem.DayDivider(dateLabel = firstDate.format(dividerFormatter)))

        for (i in sliced.indices) {
            val item = sliced[i]
            val localZdt = item.time.atZone(zoneId)

            if (i > 0) {
                val prevLocalZdt = sliced[i - 1].time.atZone(zoneId)
                if (localZdt.toLocalDate() != prevLocalZdt.toLocalDate()) {
                    val label = localZdt.format(dividerFormatter)
                    items.add(HourlyListItem.DayDivider(dateLabel = label))
                }
            }

            items.add(
                HourlyListItem.HourRow(
                    forecast = item,
                    isCurrentHour = (i == 0)
                )
            )
        }

        if (sliced.size < 36) {
            items.add(HourlyListItem.HorizonFooter())
        }

        return items
    }
}
