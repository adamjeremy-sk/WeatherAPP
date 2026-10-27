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
    @SerializedName("properties") val properties: MeteoGateBasicProperties? = null,
    @SerializedName("links") val links: List<MeteoGateLink>? = null
)

@Keep
data class MeteoGateBasicProperties(
    @SerializedName("countryCode") val countryCode: String? = null,
    @SerializedName("hubTime") val hubTime: String? = null
)

@Keep
data class MeteoGateLink(
    @SerializedName("rel") val rel: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("href") val href: String? = null
)

@Keep
data class MeteoGateDetailedCap(
    @SerializedName("identifier") val identifier: String? = null,
    @SerializedName("sender") val sender: String? = null,
    @SerializedName("sent") val sent: String? = null,
    @SerializedName("msgType") val msgType: String? = null,
    @SerializedName("references") val references: String? = null,
    @SerializedName("info") val info: List<MeteoGateCapInfo>? = null
)

@Keep
data class MeteoGateCapInfo(
    @SerializedName("language") val language: String? = null,
    @SerializedName("event") val event: String? = null,
    @SerializedName("headline") val headline: String? = null,
    @SerializedName("severity") val severity: String? = null,
    @SerializedName("onset") val onset: String? = null,
    @SerializedName("expires") val expires: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("instruction") val instruction: String? = null,
    @SerializedName("area") val area: List<MeteoGateCapArea>? = null,
    @SerializedName("parameter") val parameter: List<MeteoGateCapParameter>? = null
)

@Keep
data class MeteoGateCapArea(
    @SerializedName("areaDesc") val areaDesc: String? = null,
    @SerializedName("polygon") val polygon: List<String>? = null
)

@Keep
data class MeteoGateCapParameter(
    @SerializedName("valueName") val valueName: String? = null,
    @SerializedName("value") val value: String? = null
)
