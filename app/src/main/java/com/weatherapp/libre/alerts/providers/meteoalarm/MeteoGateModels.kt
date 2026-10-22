package com.weatherapp.libre.alerts.providers.meteoalarm

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class MeteoGateFeatureCollection(
    @SerializedName("type") val type: String? = null,
    @SerializedName("features") val features: List<MeteoGateFeature>? = null
)

@Keep
data class MeteoGateFeature(
    @SerializedName("type") val type: String? = null,
    @SerializedName("id") val id: String? = null,
    @SerializedName("properties") val properties: MeteoGateProperties? = null
)

@Keep
data class MeteoGateProperties(
    @SerializedName("event") val event: String? = null,
    @SerializedName("phenomenon") val phenomenon: String? = null,
    @SerializedName("awareness_level") val awarenessLevel: String? = null,
    @SerializedName("awareness_type") val awarenessType: String? = null,
    @SerializedName("severity") val severity: String? = null,
    @SerializedName("onset") val onset: String? = null,
    @SerializedName("expires") val expires: String? = null,
    @SerializedName("headline") val headline: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("instruction") val instruction: String? = null,
    @SerializedName("areaDesc") val areaDesc: String? = null,
    @SerializedName("region_name") val regionName: String? = null,
    @SerializedName("country") val country: String? = null
)
