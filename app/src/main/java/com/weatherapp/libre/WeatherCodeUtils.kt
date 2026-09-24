package com.weatherapp.libre

object WeatherCodeUtils {

    fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Light Drizzle"
            56, 57 -> "Freezing Drizzle"
            61 -> "Slight Rain"
            63 -> "Moderate Rain"
            65 -> "Heavy Rain"
            66, 67 -> "Freezing Rain"
            71 -> "Slight Snow"
            73 -> "Moderate Snow"
            75 -> "Heavy Snow"
            77 -> "Snow Grains"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with Hail"
            else -> "Varied Conditions"
        }
    }

    fun getWeatherEmoji(code: Int, isDay: Boolean = true): String {
        return when (code) {
            0 -> if (isDay) "☀️" else "🌙"
            1 -> if (isDay) "🌤️" else "🌙"
            2 -> "⛅"
            3 -> "☁️"
            45, 48 -> "🌫️"
            51, 53, 55 -> "🌦️"
            56, 57 -> "🌨️"
            61, 63 -> "🌧️"
            65 -> "🌧️"
            66, 67 -> "🧊"
            71, 73 -> "🌨️"
            75 -> "❄️"
            77 -> "🌨️"
            80, 81, 82 -> "🌦️"
            85, 86 -> "🌨️"
            95, 96, 99 -> "⛈️"
            else -> "🌡️"
        }
    }
}
