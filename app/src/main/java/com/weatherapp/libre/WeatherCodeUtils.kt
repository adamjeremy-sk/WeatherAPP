package com.weatherapp.libre

object WeatherCodeUtils {

    fun conditionIconRes(code: Int): Int {
        return when (code) {
            0 -> R.drawable.ic_condition_clear_sky
            1 -> R.drawable.ic_condition_mainly_clear
            2 -> R.drawable.ic_condition_partly_cloudy
            3 -> R.drawable.ic_condition_overcast
            45 -> R.drawable.ic_condition_fog
            48 -> R.drawable.ic_condition_rime_fog
            51 -> R.drawable.ic_condition_drizzle_light
            53 -> R.drawable.ic_condition_drizzle_moderate
            55 -> R.drawable.ic_condition_drizzle_dense
            56 -> R.drawable.ic_condition_freezing_drizzle_light
            57 -> R.drawable.ic_condition_freezing_drizzle_dense
            61 -> R.drawable.ic_condition_rain_slight
            63 -> R.drawable.ic_condition_rain_moderate
            65 -> R.drawable.ic_condition_rain_heavy
            66 -> R.drawable.ic_condition_freezing_rain_light
            67 -> R.drawable.ic_condition_freezing_rain_heavy
            71 -> R.drawable.ic_condition_snow_slight
            73 -> R.drawable.ic_condition_snow_moderate
            75 -> R.drawable.ic_condition_snow_heavy
            77 -> R.drawable.ic_condition_snow_grains
            80 -> R.drawable.ic_condition_rain_showers_slight
            81 -> R.drawable.ic_condition_rain_showers_moderate
            82 -> R.drawable.ic_condition_rain_showers_violent
            85 -> R.drawable.ic_condition_snow_showers_slight
            86 -> R.drawable.ic_condition_snow_showers_heavy
            95 -> R.drawable.ic_condition_thunderstorm
            96 -> R.drawable.ic_condition_thunderstorm_hail_slight
            99 -> R.drawable.ic_condition_thunderstorm_hail_heavy
            else -> R.drawable.ic_condition_unknown
        }
    }

    fun conditionStringRes(code: Int): Int {
        return when (code) {
            0 -> R.string.condition_clear_sky
            1 -> R.string.condition_mainly_clear
            2 -> R.string.condition_partly_cloudy
            3 -> R.string.condition_overcast
            45, 48 -> R.string.condition_foggy
            51, 53, 55 -> R.string.condition_light_drizzle
            56, 57 -> R.string.condition_freezing_drizzle
            61 -> R.string.condition_slight_rain
            63 -> R.string.condition_moderate_rain
            65 -> R.string.condition_heavy_rain
            66, 67 -> R.string.condition_freezing_rain
            71 -> R.string.condition_slight_snow
            73 -> R.string.condition_moderate_snow
            75 -> R.string.condition_heavy_snow
            77 -> R.string.condition_snow_grains
            80, 81, 82 -> R.string.condition_rain_showers
            85, 86 -> R.string.condition_snow_showers
            95 -> R.string.condition_thunderstorm
            96, 99 -> R.string.condition_thunderstorm_hail
            else -> R.string.condition_varied
        }
    }

    fun getWeatherDescription(context: android.content.Context, code: Int): String {
        return context.getString(conditionStringRes(code))
    }

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

    @Deprecated("Replaced with vector condition icons via conditionIconRes(code)")
    fun getWeatherEmoji(code: Int, isDay: Boolean = true): String {
        return ""
    }
}
