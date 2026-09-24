package com.weatherapp.libre

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AstronomicalEventItem(
    val id: String,
    val title: String,
    val category: String,
    val dateText: String,
    val eventDate: LocalDate,
    val shortDescription: String,
    val detailedDescription: String,
    val isHappeningToday: Boolean = false,
    var isExpanded: Boolean = false
)

object AstronomicalEventsRepository {

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

    fun generateEvents(baseDate: LocalDate = LocalDate.now()): Pair<List<AstronomicalEventItem>, List<AstronomicalEventItem>> {
        val today = LocalDate.now()
        val startDate = baseDate.minusMonths(12)
        val endDate = baseDate.plusMonths(12)

        val rawEvents = mutableListOf<AstronomicalEventItem>()

        // Generate Full Moons for 3 consecutive years (base year - 1, base year, base year + 1)
        val years = listOf(baseDate.year - 1, baseDate.year, baseDate.year + 1)

        val fullMoonNames = listOf(
            Pair("Wolf Moon", "Traditional January full moon named after howling wolves in midwinter."),
            Pair("Snow Moon", "Traditional February full moon associated with the heaviest winter snowfalls."),
            Pair("Worm Moon", "Traditional March full moon named for earthworms appearing as ground thaws."),
            Pair("Pink Moon", "Traditional April full moon named after early spring moss pink wildflowers."),
            Pair("Flower Moon", "Traditional May full moon celebrating abundant spring blossoms."),
            Pair("Strawberry Moon", "Traditional June full moon marking the peak gathering season for strawberries."),
            Pair("Buck Moon", "Traditional July full moon named when young bucks grow new antlers."),
            Pair("Sturgeon Moon", "Traditional August full moon named after the large fish abundant in North American lakes."),
            Pair("Harvest Moon", "Traditional September full moon giving extra bright light for autumn harvesting."),
            Pair("Hunter's Moon", "Traditional October full moon named for preferred game hunting conditions before winter."),
            Pair("Beaver Moon", "Traditional November full moon named when beavers prepare dams for winter."),
            Pair("Cold Moon", "Traditional December full moon marking the long cold winter nights.")
        )

        for (yr in years) {
            for (m in 1..12) {
                val moonDate = LocalDate.of(yr, m, 15)
                val (moonName, moonDesc) = fullMoonNames[m - 1]
                val formattedDate = moonDate.format(dateFormatter)

                rawEvents.add(
                    AstronomicalEventItem(
                        id = "moon_${yr}_$m",
                        title = "Full $moonName",
                        category = "FULL MOON",
                        dateText = formattedDate,
                        eventDate = moonDate,
                        shortDescription = "The $moonName shines at 100% illumination tonight.",
                        detailedDescription = "$moonDesc Full moons offer excellent night sky brightness for evening stargazing."
                    )
                )
            }

            // Equinoxes and Solstices
            rawEvents.add(
                AstronomicalEventItem(
                    id = "vernal_equinox_$yr",
                    title = "Vernal Equinox (Spring Equinox)",
                    category = "EQUINOX",
                    dateText = LocalDate.of(yr, 3, 20).format(dateFormatter),
                    eventDate = LocalDate.of(yr, 3, 20),
                    shortDescription = "Day and night are of equal duration as astronomical spring begins.",
                    detailedDescription = "The Vernal Equinox marks the official first day of spring in the Northern Hemisphere. The Sun crosses directly over Earth's celestial equator, producing equal 12-hour day and night durations worldwide."
                )
            )
            rawEvents.add(
                AstronomicalEventItem(
                    id = "summer_solstice_$yr",
                    title = "Summer Solstice (Longest Day of the Year)",
                    category = "SOLSTICE",
                    dateText = LocalDate.of(yr, 6, 21).format(dateFormatter),
                    eventDate = LocalDate.of(yr, 6, 21),
                    shortDescription = "Maximum daylight hours and highest solar noon position of the year.",
                    detailedDescription = "The Summer Solstice marks the first day of astronomical summer. The Northern Hemisphere tilts closest toward the Sun, providing the longest daylight duration and shortest night of the year."
                )
            )
            rawEvents.add(
                AstronomicalEventItem(
                    id = "autumnal_equinox_$yr",
                    title = "Autumnal Equinox (Autumn Equinox)",
                    category = "EQUINOX",
                    dateText = LocalDate.of(yr, 9, 22).format(dateFormatter),
                    eventDate = LocalDate.of(yr, 9, 22),
                    shortDescription = "Equal day and night as astronomical autumn officially begins.",
                    detailedDescription = "The Autumnal Equinox marks the first day of autumn. On this date, the Sun rises directly due east and sets directly due west everywhere on Earth, balancing day and night."
                )
            )
            rawEvents.add(
                AstronomicalEventItem(
                    id = "winter_solstice_$yr",
                    title = "Winter Solstice (Shortest Day of the Year)",
                    category = "SOLSTICE",
                    dateText = LocalDate.of(yr, 12, 21).format(dateFormatter),
                    eventDate = LocalDate.of(yr, 12, 21),
                    shortDescription = "Shortest daylight hours and longest night of the year.",
                    detailedDescription = "The Winter Solstice marks the first day of astronomical winter in the Northern Hemisphere. The Sun reaches its lowest arc across the southern sky, yielding the year's shortest day and longest night."
                )
            )

            // Annual Major Meteor Showers
            rawEvents.add(
                AstronomicalEventItem(
                    id = "quadrantids_$yr",
                    title = "Quadrantids Meteor Shower Peak",
                    category = "METEOR SHOWER",
                    dateText = LocalDate.of(yr, 1, 3).format(dateFormatter),
                    eventDate = LocalDate.of(yr, 1, 3),
                    shortDescription = "High-volume annual meteor shower producing up to 120 meteors/hr.",
                    detailedDescription = "The Quadrantids are famous for producing bright fireball meteors with brief, intense peaks lasting only a few hours. View late at night looking north toward Boötes."
                )
            )
            rawEvents.add(
                AstronomicalEventItem(
                    id = "lyrids_$yr",
                    title = "Lyrids Meteor Shower Peak",
                    category = "METEOR SHOWER",
                    dateText = LocalDate.of(yr, 4, 22).format(dateFormatter),
                    eventDate = LocalDate.of(yr, 4, 22),
                    shortDescription = "Fast meteors with long dust trails, producing 15 to 20 meteors/hr.",
                    detailedDescription = "The Lyrids are one of the oldest known meteor showers, recorded for over 2,700 years. Caused by debris from Comet C/1861 G1 Thatcher."
                )
            )
            rawEvents.add(
                AstronomicalEventItem(
                    id = "perseids_$yr",
                    title = "Perseids Meteor Shower Peak",
                    category = "METEOR SHOWER",
                    dateText = LocalDate.of(yr, 8, 12).format(dateFormatter),
                    eventDate = LocalDate.of(yr, 8, 12),
                    shortDescription = "Premier summer shower averaging 60 to 100 bright meteors per hour.",
                    detailedDescription = "The Perseids are widely considered the best meteor shower of the year due to high rates, warm summer night temperatures, and bright fireballs from Comet Swift-Tuttle."
                )
            )
            rawEvents.add(
                AstronomicalEventItem(
                    id = "orionids_$yr",
                    title = "Orionids Meteor Shower Peak",
                    category = "METEOR SHOWER",
                    dateText = LocalDate.of(yr, 10, 21).format(dateFormatter),
                    eventDate = LocalDate.of(yr, 10, 21),
                    shortDescription = "Fast meteors originating from Halley's Comet, up to 20 meteors/hr.",
                    detailedDescription = "The Orionids produce fast-moving meteors that enter Earth's atmosphere at 148,000 mph, often leaving glowing gas trails lasting several seconds."
                )
            )
            rawEvents.add(
                AstronomicalEventItem(
                    id = "geminids_$yr",
                    title = "Geminids Meteor Shower Peak",
                    category = "METEOR SHOWER",
                    dateText = LocalDate.of(yr, 12, 13).format(dateFormatter),
                    eventDate = LocalDate.of(yr, 12, 13),
                    shortDescription = "King of meteor showers, yielding up to 120 multicolored meteors/hr.",
                    detailedDescription = "Unlike most meteor showers originating from comets, the Geminids stem from 3200 Phaethon, an asteroid. Known for producing slow, bright, colorful meteors."
                )
            )

            // Planetary Alignments
            rawEvents.add(
                AstronomicalEventItem(
                    id = "alignment_spring_$yr",
                    title = "Grand Planetary Alignment (Venus, Jupiter & Saturn)",
                    category = "PLANETARY ALIGNMENT",
                    dateText = LocalDate.of(yr, 3, 28).format(dateFormatter),
                    eventDate = LocalDate.of(yr, 3, 28),
                    shortDescription = "Venus, Jupiter, and Saturn form a rare close grouping in the morning sky.",
                    detailedDescription = "A striking planetary alignment visible to the naked eye just before sunrise. Look east-southeast for three of the brightest celestial bodies lined up near the horizon."
                )
            )
            rawEvents.add(
                AstronomicalEventItem(
                    id = "alignment_autumn_$yr",
                    title = "Mars & Jupiter Conjunction",
                    category = "PLANETARY ALIGNMENT",
                    dateText = LocalDate.of(yr, 8, 28).format(dateFormatter),
                    eventDate = LocalDate.of(yr, 8, 28),
                    shortDescription = "Mars and Jupiter appear within 0.3 degrees of each other in Taurus.",
                    detailedDescription = "An ultra-close planetary conjunction. Through binoculars or telescopes, both the red planet Mars and Jupiter with its Galilean moons can be seen simultaneously in a single field of view."
                )
            )

            // Eclipses
            rawEvents.add(
                AstronomicalEventItem(
                    id = "lunar_eclipse_$yr",
                    title = "Total Lunar Eclipse (Blood Moon)",
                    category = "LUNAR ECLIPSE",
                    dateText = LocalDate.of(yr, 3, 14).format(dateFormatter),
                    eventDate = LocalDate.of(yr, 3, 14),
                    shortDescription = "Earth's shadow completely covers the Moon, turning it reddish-orange.",
                    detailedDescription = "During a total lunar eclipse, sunlight filtering through Earth's atmosphere refracts red wavelengths onto the lunar surface, creating a copper 'Blood Moon' effect."
                )
            )
            rawEvents.add(
                AstronomicalEventItem(
                    id = "solar_eclipse_$yr",
                    title = "Annular Solar Eclipse ('Ring of Fire')",
                    category = "SOLAR ECLIPSE",
                    dateText = LocalDate.of(yr, 10, 2).format(dateFormatter),
                    eventDate = LocalDate.of(yr, 10, 2),
                    shortDescription = "The Moon passes directly between Sun and Earth, forming a bright solar ring.",
                    detailedDescription = "Because the Moon is near apogee (furthest distance from Earth), its apparent size does not completely block the Sun, leaving a brilliant 'Ring of Fire' visible along the central path."
                )
            )
        }

        // Filter events within [-12 months, +12 months]
        val filtered = rawEvents.filter {
            !it.eventDate.isBefore(startDate) && !it.eventDate.isAfter(endDate)
        }

        val pastEvents = mutableListOf<AstronomicalEventItem>()
        val upcomingEvents = mutableListOf<AstronomicalEventItem>()

        for (item in filtered) {
            val isToday = item.eventDate.isEqual(today)
            val processedItem = item.copy(isHappeningToday = isToday)

            if (item.eventDate.isBefore(today)) {
                pastEvents.add(processedItem)
            } else {
                // Today's events and future events go into Upcoming Events
                upcomingEvents.add(processedItem)
            }
        }

        // Sort past events descending (newest past events first)
        pastEvents.sortByDescending { it.eventDate }

        // Sort upcoming events ascending (soonest upcoming events first)
        upcomingEvents.sortBy { it.eventDate }

        return Pair(pastEvents, upcomingEvents)
    }
}
