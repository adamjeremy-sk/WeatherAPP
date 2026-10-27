package com.weatherapp.libre.alerts

import android.util.Log
import com.weatherapp.libre.alerts.models.CapAlert
import com.weatherapp.libre.alerts.models.CapArea
import com.weatherapp.libre.alerts.models.CapCircle
import com.weatherapp.libre.alerts.models.CapGeocode
import com.weatherapp.libre.alerts.models.CapInfo
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.StringReader
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.Locale

object CapXmlParser {

    private fun createXmlPullParser(): XmlPullParser {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            factory.newPullParser()
        } catch (e: Exception) {
            try {
                Class.forName("org.kxml2.io.KXmlParser")
                    .getDeclaredConstructor()
                    .newInstance() as XmlPullParser
            } catch (e2: Throwable) {
                throw RuntimeException("Failed to create XmlPullParser", e2)
            }
        }
    }

    fun parse(xmlString: String, targetLocale: String = "en-CA", isTestFixture: Boolean = false): CapAlert? {
        return parse(StringReader(xmlString), targetLocale, isTestFixture)
    }

    fun parse(inputStream: InputStream, targetLocale: String = "en-CA", isTestFixture: Boolean = false): CapAlert? {
        val parser = createXmlPullParser()
        parser.setInput(inputStream, "UTF-8")
        return parseInternal(parser, targetLocale, isTestFixture)
    }

    fun parse(reader: java.io.Reader, targetLocale: String = "en-CA", isTestFixture: Boolean = false): CapAlert? {
        val parser = createXmlPullParser()
        parser.setInput(reader)
        return parseInternal(parser, targetLocale, isTestFixture)
    }

    private fun parseInternal(parser: XmlPullParser, targetLocale: String, isTestFixture: Boolean): CapAlert? {
        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name.equals("alert", ignoreCase = true)) {
                    return readAlert(parser, targetLocale, isTestFixture)
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("CapXmlParser", "Error parsing CAP XML document", e)
        }
        return null
    }

    private fun readAlert(parser: XmlPullParser, targetLocale: String, isTestFixture: Boolean): CapAlert? {
        var identifier = ""
        var sender = ""
        var sent: Instant? = null
        var status = "Actual"
        var msgType = "Alert"
        var scope = "Public"
        var references: String? = null
        val infoList = mutableListOf<CapInfo>()

        parser.require(XmlPullParser.START_TAG, null, "alert")

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue

            when (parser.name.lowercase(Locale.ROOT)) {
                "identifier" -> identifier = readText(parser)
                "sender" -> sender = readText(parser)
                "sent" -> sent = parseInstant(readText(parser))
                "status" -> status = readText(parser)
                "msgtype" -> msgType = readText(parser)
                "scope" -> scope = readText(parser)
                "references" -> references = readText(parser)
                "info" -> {
                    val info = readInfo(parser)
                    if (info != null) infoList.add(info)
                }
                else -> skip(parser)
            }
        }

        if (identifier.isBlank() || sent == null) return null

        // Filter info list for user's locale or fallback to en-CA
        val selectedInfoList = filterInfoForLocale(infoList, targetLocale)

        return CapAlert(
            identifier = identifier,
            sender = sender,
            sent = sent,
            status = status,
            msgType = msgType,
            scope = scope,
            references = references,
            infoList = selectedInfoList,
            isTestFixture = isTestFixture || status.equals("Test", ignoreCase = true)
        )
    }

    private fun filterInfoForLocale(allInfo: List<CapInfo>, targetLocale: String): List<CapInfo> {
        if (allInfo.isEmpty()) return emptyList()

        val normalizedTarget = if (targetLocale.lowercase(Locale.ROOT).startsWith("fr")) "fr-ca" else "en-ca"
        val matching = allInfo.filter { it.language.lowercase(Locale.ROOT).startsWith(normalizedTarget.substring(0, 2)) }

        return if (matching.isNotEmpty()) matching else {
            val enFallback = allInfo.filter { it.language.lowercase(Locale.ROOT).startsWith("en") }
            if (enFallback.isNotEmpty()) enFallback else listOf(allInfo.first())
        }
    }

    private fun readInfo(parser: XmlPullParser): CapInfo? {
        parser.require(XmlPullParser.START_TAG, null, "info")

        var language = "en-CA"
        var category = "Met"
        var event = "Weather Alert"
        var urgency = "Immediate"
        var severity = "Severe"
        var certainty = "Observed"
        var effective: Instant? = null
        var onset: Instant? = null
        var expires: Instant? = null
        var senderName: String? = null
        var headline: String? = null
        var description: String? = null
        var instruction: String? = null
        var web: String? = null
        val areaList = mutableListOf<CapArea>()

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue

            when (parser.name.lowercase(Locale.ROOT)) {
                "language" -> language = readText(parser)
                "category" -> category = readText(parser)
                "event" -> event = readText(parser)
                "urgency" -> urgency = readText(parser)
                "severity" -> severity = readText(parser)
                "certainty" -> certainty = readText(parser)
                "effective" -> effective = parseInstant(readText(parser))
                "onset" -> onset = parseInstant(readText(parser))
                "expires" -> expires = parseInstant(readText(parser))
                "sendername" -> senderName = readText(parser)
                "headline" -> headline = readText(parser)
                "description" -> description = readText(parser)
                "instruction" -> instruction = readText(parser)
                "web" -> web = readText(parser)
                "area" -> {
                    val area = readArea(parser)
                    if (area != null) areaList.add(area)
                }
                else -> skip(parser)
            }
        }

        return CapInfo(
            language = language,
            category = category,
            event = event,
            urgency = urgency,
            severity = severity,
            certainty = certainty,
            effective = effective,
            onset = onset,
            expires = expires,
            senderName = senderName,
            headline = headline,
            description = description,
            instruction = instruction,
            web = web,
            areaList = areaList
        )
    }

    private fun readArea(parser: XmlPullParser): CapArea? {
        parser.require(XmlPullParser.START_TAG, null, "area")

        var areaDesc = ""
        val polygons = mutableListOf<List<Pair<Double, Double>>>()
        val circles = mutableListOf<CapCircle>()
        val geocodes = mutableListOf<CapGeocode>()

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue

            when (parser.name.lowercase(Locale.ROOT)) {
                "areadesc" -> areaDesc = readText(parser)
                "polygon" -> {
                    val polyStr = readText(parser)
                    val points = parsePolygon(polyStr)
                    if (points.isNotEmpty()) polygons.add(points)
                }
                "circle" -> {
                    val circleStr = readText(parser)
                    val circle = parseCircle(circleStr)
                    if (circle != null) circles.add(circle)
                }
                "geocode" -> {
                    val geocode = readGeocode(parser)
                    if (geocode != null) geocodes.add(geocode)
                }
                else -> skip(parser)
            }
        }

        return CapArea(
            areaDesc = areaDesc,
            polygons = polygons,
            circles = circles,
            geocodes = geocodes
        )
    }

    private fun readGeocode(parser: XmlPullParser): CapGeocode? {
        parser.require(XmlPullParser.START_TAG, null, "geocode")

        var valueName = ""
        var value = ""

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue

            when (parser.name.lowercase(Locale.ROOT)) {
                "valuename" -> valueName = readText(parser)
                "value" -> value = readText(parser)
                else -> skip(parser)
            }
        }

        return if (valueName.isNotBlank() && value.isNotBlank()) {
            CapGeocode(valueName, value)
        } else null
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text.trim()
            parser.nextTag()
        }
        return result
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    fun parseInstant(text: String): Instant? {
        if (text.isBlank()) return null
        return try {
            Instant.parse(text)
        } catch (e: DateTimeParseException) {
            try {
                OffsetDateTime.parse(text).toInstant()
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun parsePolygon(text: String): List<Pair<Double, Double>> {
        val pairs = text.trim().split(" ", "\n", "\t").filter { it.isNotBlank() }
        val points = mutableListOf<Pair<Double, Double>>()
        for (pairStr in pairs) {
            val parts = pairStr.split(",")
            if (parts.size == 2) {
                val lat = parts[0].toDoubleOrNull()
                val lon = parts[1].toDoubleOrNull()
                if (lat != null && lon != null) {
                    points.add(Pair(lat, lon))
                }
            }
        }
        return points
    }

    private fun parseCircle(text: String): CapCircle? {
        // Format: "lat,lon radiusKm" or "lat,lon,radiusKm"
        val trimmed = text.trim()
        val spaceParts = trimmed.split(" ")
        if (spaceParts.size == 2) {
            val coords = spaceParts[0].split(",")
            val radius = spaceParts[1].toDoubleOrNull()
            if (coords.size == 2 && radius != null) {
                val lat = coords[0].toDoubleOrNull()
                val lon = coords[1].toDoubleOrNull()
                if (lat != null && lon != null) {
                    return CapCircle(lat, lon, radius)
                }
            }
        } else {
            val parts = trimmed.split(",")
            if (parts.size == 3) {
                val lat = parts[0].toDoubleOrNull()
                val lon = parts[1].toDoubleOrNull()
                val radius = parts[2].toDoubleOrNull()
                if (lat != null && lon != null && radius != null) {
                    return CapCircle(lat, lon, radius)
                }
            }
        }
        return null
    }
}
