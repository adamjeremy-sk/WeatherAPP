package com.weatherapp.libre.alerts

import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.models.CapAlert
import com.weatherapp.libre.alerts.models.CapArea
import com.weatherapp.libre.alerts.models.CapCircle
import com.weatherapp.libre.alerts.models.CapGeocode
import com.weatherapp.libre.alerts.models.CapInfo
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class AlertMatcherTest {

    private val ottawa = LocationItem(id = 1L, name = "Ottawa", admin1 = "Ontario", country = "Canada", latitude = 45.4215, longitude = -75.6972)
    private val vancouver = LocationItem(id = 2L, name = "Vancouver", admin1 = "BC", country = "Canada", latitude = 49.2827, longitude = -123.1207)

    @Test
    fun testCircleRadiusMatchingTrue() {
        val alert = CapAlert(
            identifier = "urn:oid:test:1",
            sender = "cap-pac@ec.gc.ca",
            sent = Instant.now(),
            status = "Actual",
            msgType = "Alert",
            infoList = listOf(
                CapInfo(
                    areaList = listOf(
                        CapArea(
                            areaDesc = "Ottawa Region",
                            circles = listOf(CapCircle(45.4215, -75.6972, 50.0))
                        )
                    )
                )
            )
        )

        val resultOttawa = AlertMatcher.matchesLocation(alert, ottawa)
        assertTrue(resultOttawa.isMatched)
        assertEquals(AlertMatcher.MatchRule.CIRCLE, resultOttawa.rule)

        val resultVancouver = AlertMatcher.matchesLocation(alert, vancouver)
        assertFalse(resultVancouver.isMatched)
        assertEquals(AlertMatcher.MatchRule.NONE, resultVancouver.rule)
    }

    @Test
    fun testPointInPolygonMatchingTrueAndFalse() {
        // Square polygon around Ottawa: lat [45.0, 46.0], lon [-76.0, -75.0]
        val polygon = listOf(
            Pair(45.0, -76.0),
            Pair(46.0, -76.0),
            Pair(46.0, -75.0),
            Pair(45.0, -75.0)
        )

        val alert = CapAlert(
            identifier = "urn:oid:test:2",
            sender = "cap-pac@ec.gc.ca",
            sent = Instant.now(),
            status = "Actual",
            msgType = "Alert",
            infoList = listOf(
                CapInfo(
                    areaList = listOf(
                        CapArea(
                            areaDesc = "Ottawa Polygon Area",
                            polygons = listOf(polygon)
                        )
                    )
                )
            )
        )

        val resultOttawa = AlertMatcher.matchesLocation(alert, ottawa)
        assertTrue(resultOttawa.isMatched)
        assertEquals(AlertMatcher.MatchRule.POLYGON, resultOttawa.rule)

        val resultVancouver = AlertMatcher.matchesLocation(alert, vancouver)
        assertFalse(resultVancouver.isMatched)
        assertEquals(AlertMatcher.MatchRule.NONE, resultVancouver.rule)
    }

    @Test
    fun testPublicCanadaScopeMatching() {
        val alert = CapAlert(
            identifier = "urn:oid:test:3",
            sender = "cap-pac@ec.gc.ca",
            sent = Instant.now(),
            status = "Actual",
            msgType = "Alert",
            scope = "Public",
            infoList = listOf(
                CapInfo(
                    areaList = emptyList() // No specific area info -> applies to all Canada
                )
            )
        )

        val resultOttawa = AlertMatcher.matchesLocation(alert, ottawa)
        assertTrue(resultOttawa.isMatched)
        assertEquals(AlertMatcher.MatchRule.PUBLIC_CANADA, resultOttawa.rule)

        val resultVancouver = AlertMatcher.matchesLocation(alert, vancouver)
        assertTrue(resultVancouver.isMatched)
        assertEquals(AlertMatcher.MatchRule.PUBLIC_CANADA, resultVancouver.rule)
    }

    @Test
    fun testGeocodeMatching() {
        val alert = CapAlert(
            identifier = "urn:oid:test:4",
            sender = "cap-pac@ec.gc.ca",
            sent = Instant.now(),
            status = "Actual",
            msgType = "Alert",
            infoList = listOf(
                CapInfo(
                    areaList = listOf(
                        CapArea(
                            areaDesc = "Ottawa, Ontario",
                            geocodes = listOf(CapGeocode("CLC", "054321"))
                        )
                    )
                )
            )
        )

        val resultOttawa = AlertMatcher.matchesLocation(alert, ottawa)
        assertTrue(resultOttawa.isMatched)
        assertEquals(AlertMatcher.MatchRule.GEOCODE, resultOttawa.rule)

        val resultVancouver = AlertMatcher.matchesLocation(alert, vancouver)
        assertFalse(resultVancouver.isMatched)
    }
}
