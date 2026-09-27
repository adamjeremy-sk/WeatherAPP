package com.weatherapp.libre

import android.content.Context
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object LunarInfoRepository {

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    data class LunarPhaseEvent(
        val date: LocalDate,
        val type: String
    )

    private var cachedEvents: List<AstronomicalEventItem>? = null
    private var cachedPhases: List<LunarPhaseEvent>? = null

    fun loadLunarInfo(context: Context) {
        if (cachedEvents != null && cachedPhases != null) return

        val eventsList = mutableListOf<AstronomicalEventItem>()
        val phasesList = mutableListOf<LunarPhaseEvent>()

        try {
            val inputStream = context.resources.openRawResource(R.raw.lunarinfo)
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val tagName = parser.name
                    when (tagName) {
                        "phase" -> {
                            val dateStr = parser.getAttributeValue(null, "date")
                            val typeStr = parser.getAttributeValue(null, "type") ?: "Moon Phase"
                            if (!dateStr.isNullOrEmpty()) {
                                val date = LocalDate.parse(dateStr, isoFormatter)
                                phasesList.add(LunarPhaseEvent(date, typeStr))

                                val category = "LUNAR PHASE"
                                val emoji = when (typeStr) {
                                    "New Moon" -> "🌑"
                                    "First Quarter" -> "🌓"
                                    "Full Moon" -> "🌕"
                                    "Last Quarter" -> "🌗"
                                    else -> "🌙"
                                }
                                val formattedDate = date.format(dateFormatter)
                                eventsList.add(
                                    AstronomicalEventItem(
                                        id = "phase_${dateStr}_$typeStr",
                                        title = "$typeStr $emoji",
                                        category = category,
                                        dateText = formattedDate,
                                        eventDate = date,
                                        shortDescription = "$typeStr occurring on $formattedDate.",
                                        detailedDescription = "Official astronomical $typeStr milestone from lunar telemetry data for $formattedDate."
                                    )
                                )
                            }
                        }
                        "eclipse" -> {
                            val dateStr = parser.getAttributeValue(null, "date")
                            val typeStr = parser.getAttributeValue(null, "type") ?: "Lunar Eclipse"
                            if (!dateStr.isNullOrEmpty()) {
                                val date = LocalDate.parse(dateStr, isoFormatter)
                                val formattedDate = date.format(dateFormatter)
                                eventsList.add(
                                    AstronomicalEventItem(
                                        id = "eclipse_$dateStr",
                                        title = "🌘 $typeStr",
                                        category = "LUNAR ECLIPSE",
                                        dateText = formattedDate,
                                        eventDate = date,
                                        shortDescription = "$typeStr occurring on $formattedDate.",
                                        detailedDescription = "Astronomical $typeStr recorded in lunar telemetry data for $formattedDate."
                                    )
                                )
                            }
                        }
                        "specialMoon" -> {
                            val dateStr = parser.getAttributeValue(null, "date")
                            val nameStr = parser.getAttributeValue(null, "name") ?: "Special Moon"
                            if (!dateStr.isNullOrEmpty()) {
                                val date = LocalDate.parse(dateStr, isoFormatter)
                                val formattedDate = date.format(dateFormatter)
                                eventsList.add(
                                    AstronomicalEventItem(
                                        id = "special_$dateStr",
                                        title = "🌕 $nameStr",
                                        category = "SPECIAL MOON",
                                        dateText = formattedDate,
                                        eventDate = date,
                                        shortDescription = "$nameStr occurring on $formattedDate.",
                                        detailedDescription = "$nameStr astronomical event recorded in lunar telemetry for $formattedDate."
                                    )
                                )
                            }
                        }
                        "meteorShower" -> {
                            val dateStr = parser.getAttributeValue(null, "date")
                            val nameStr = parser.getAttributeValue(null, "name") ?: "Meteor Shower"
                            if (!dateStr.isNullOrEmpty()) {
                                val date = LocalDate.parse(dateStr, isoFormatter)
                                val formattedDate = date.format(dateFormatter)
                                eventsList.add(
                                    AstronomicalEventItem(
                                        id = "meteor_$dateStr",
                                        title = "☄️ $nameStr",
                                        category = "METEOR SHOWER",
                                        dateText = formattedDate,
                                        eventDate = date,
                                        shortDescription = "$nameStr on $formattedDate.",
                                        detailedDescription = "Peak activity for $nameStr recorded in astronomical observations for $formattedDate."
                                    )
                                )
                            }
                        }
                        "alignment" -> {
                            val dateStr = parser.getAttributeValue(null, "date")
                            val eventStr = parser.getAttributeValue(null, "event") ?: "Planetary Alignment"
                            if (!dateStr.isNullOrEmpty()) {
                                val date = LocalDate.parse(dateStr, isoFormatter)
                                val formattedDate = date.format(dateFormatter)
                                eventsList.add(
                                    AstronomicalEventItem(
                                        id = "alignment_$dateStr",
                                        title = "🪐 $eventStr",
                                        category = "PLANETARY ALIGNMENT",
                                        dateText = formattedDate,
                                        eventDate = date,
                                        shortDescription = "$eventStr on $formattedDate.",
                                        detailedDescription = "Astronomical planetary alignment/conjunction: $eventStr."
                                    )
                                )
                            }
                        }
                        "season" -> {
                            val dateStr = parser.getAttributeValue(null, "date")
                            val nameStr = parser.getAttributeValue(null, "name") ?: "Equinox/Solstice"
                            if (!dateStr.isNullOrEmpty()) {
                                val date = LocalDate.parse(dateStr, isoFormatter)
                                val formattedDate = date.format(dateFormatter)
                                eventsList.add(
                                    AstronomicalEventItem(
                                        id = "season_$dateStr",
                                        title = "☀️ $nameStr",
                                        category = "SOLSTICE & EQUINOX",
                                        dateText = formattedDate,
                                        eventDate = date,
                                        shortDescription = "$nameStr on $formattedDate.",
                                        detailedDescription = "Astronomical seasonal milestone: $nameStr."
                                    )
                                )
                            }
                        }
                        "asteroidPass" -> {
                            val dateStr = parser.getAttributeValue(null, "date")
                            val eventStr = parser.getAttributeValue(null, "event") ?: "Asteroid Approach"
                            if (!dateStr.isNullOrEmpty()) {
                                val date = LocalDate.parse(dateStr, isoFormatter)
                                val formattedDate = date.format(dateFormatter)
                                eventsList.add(
                                    AstronomicalEventItem(
                                        id = "asteroid_$dateStr",
                                        title = "☄️ $eventStr",
                                        category = "ASTEROID APPROACH",
                                        dateText = formattedDate,
                                        eventDate = date,
                                        shortDescription = "$eventStr on $formattedDate.",
                                        detailedDescription = "Near-Earth Asteroid (NEA) tracking telemetry: $eventStr."
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cachedEvents = eventsList.sortedBy { it.eventDate }
        cachedPhases = phasesList.sortedBy { it.date }
    }

    fun getMoonPhaseForDate(context: Context, targetDate: LocalDate): LunarPhaseResult {
        loadLunarInfo(context)
        val phases = cachedPhases ?: emptyList()

        if (phases.isEmpty()) {
            return LunarPhaseResult(
                name = "Full Moon",
                emoji = "🌕",
                percentage = 50
            )
        }

        // Check if targetDate matches an exact phase milestone in lunarinfo.xml
        val exactMatch = phases.find { it.date.isEqual(targetDate) }
        if (exactMatch != null) {
            val (emoji, pct) = when (exactMatch.type) {
                "New Moon" -> Pair("🌑", 0)
                "First Quarter" -> Pair("🌓", 25)
                "Full Moon" -> Pair("🌕", 50)
                "Last Quarter" -> Pair("🌗", 75)
                else -> Pair("🌕", 50)
            }
            return LunarPhaseResult(
                name = exactMatch.type,
                emoji = emoji,
                percentage = pct
            )
        }

        // Find nearest previous New Moon milestone date in lunarinfo.xml
        val prevNewMoons = phases.filter { it.type == "New Moon" && !it.date.isAfter(targetDate) }
        val nextNewMoons = phases.filter { it.type == "New Moon" && it.date.isAfter(targetDate) }

        val lastNewMoon = prevNewMoons.lastOrNull()?.date ?: targetDate.minusDays(15)
        val nextNewMoon = nextNewMoons.firstOrNull()?.date ?: lastNewMoon.plusDays(29)

        val daysElapsed = java.time.temporal.ChronoUnit.DAYS.between(lastNewMoon, targetDate).toDouble()
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(lastNewMoon, nextNewMoon).toDouble().coerceAtLeast(28.0)

        val fraction = (daysElapsed / totalDays).coerceIn(0.0, 1.0)
        val cyclePct = (fraction * 100).toInt()

        val (phaseName, emoji) = when {
            fraction < 0.06 -> Pair("New Moon", "🌑")
            fraction < 0.22 -> Pair("Waxing Crescent", "🌒")
            fraction < 0.28 -> Pair("First Quarter", "🌓")
            fraction < 0.44 -> Pair("Waxing Gibbous", "🌔")
            fraction < 0.56 -> Pair("Full Moon", "🌕")
            fraction < 0.72 -> Pair("Waning Gibbous", "🌖")
            fraction < 0.78 -> Pair("Last Quarter", "🌗")
            fraction < 0.94 -> Pair("Waning Crescent", "🌘")
            else -> Pair("New Moon", "🌑")
        }

        return LunarPhaseResult(
            name = phaseName,
            emoji = emoji,
            percentage = cyclePct
        )
    }

    fun generateEventsFromLunarInfo(context: Context, today: LocalDate = LocalDate.now()): Pair<List<AstronomicalEventItem>, List<AstronomicalEventItem>> {
        loadLunarInfo(context)
        val allEvents = cachedEvents ?: emptyList()

        val pastEvents = mutableListOf<AstronomicalEventItem>()
        val upcomingEvents = mutableListOf<AstronomicalEventItem>()

        for (item in allEvents) {
            val isToday = item.eventDate.isEqual(today)
            val cloned = item.copy(isHappeningToday = isToday)

            if (item.eventDate.isBefore(today)) {
                pastEvents.add(cloned)
            } else {
                upcomingEvents.add(cloned)
            }
        }

        return Pair(pastEvents.reversed(), upcomingEvents)
    }
}
