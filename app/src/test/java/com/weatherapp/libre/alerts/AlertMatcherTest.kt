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

    // --- Epsilon Boundary Regression (1e-9) ---
    //
    // These tests pin the inclusive boundary semantics of isPointInPolygon.
    // The production ray-casting test was changed from a strict `<` to a
    // `<=` with a 1e-9 epsilon so that a point sitting exactly on a polygon
    // edge (or within floating-point ULP noise of it) is counted as INSIDE,
    // matching the inclusive (<=) semantics of the circle rule. Without the
    // epsilon, floating-point rounding can make the intersection X land a
    // hair below the point's lat and flip the boundary point to OUTSIDE.

    @Test
    fun testEpsilonPointExactlyOnHorizontalEdgeIsInside() {
        // Square polygon: lat [45.0, 46.0], lon [-76.0, -75.0]
        val polygon = listOf(
            Pair(45.0, -76.0),
            Pair(46.0, -76.0),
            Pair(46.0, -75.0),
            Pair(45.0, -75.0)
        )

        // Point exactly on the top horizontal edge: lat == 46.0, lon == -75.5.
        // The 1e-9 epsilon in `lat <= ... + 1e-9` ensures this boundary point
        // is counted as inside despite potential floating point ULP noise.
        val onEdge = AlertMatcher.isPointInPolygon(46.0, -75.5, polygon)
        assertTrue(
            "A point exactly on a polygon edge must be counted as inside (1e-9 epsilon)",
            onEdge
        )
    }

    @Test
    fun testEpsilonPointExactlyOnVertexIsInside() {
        // Square polygon: lat [45.0, 46.0], lon [-76.0, -75.0]
        val polygon = listOf(
            Pair(45.0, -76.0),
            Pair(46.0, -76.0),
            Pair(46.0, -75.0),
            Pair(45.0, -75.0)
        )

        // Point exactly on vertex B (lat == 46.0, lon == -76.0).
        // Mathematically on the boundary; the 1e-9 epsilon keeps it inside.
        val onVertex = AlertMatcher.isPointInPolygon(46.0, -76.0, polygon)
        assertTrue(
            "A point exactly on a polygon vertex must be counted as inside (1e-9 epsilon)",
            onVertex
        )
    }

    @Test
    fun testEpsilonPointClearlyOutsideIsOutside() {
        // Square polygon: lat [45.0, 46.0], lon [-76.0, -75.0]
        val polygon = listOf(
            Pair(45.0, -76.0),
            Pair(46.0, -76.0),
            Pair(46.0, -75.0),
            Pair(45.0, -75.0)
        )

        // Point clearly outside (far to the east of the polygon).
        val outside = AlertMatcher.isPointInPolygon(45.5, -74.0, polygon)
        assertFalse(
            "A point clearly outside the polygon must remain outside",
            outside
        )
    }

    @Test
    fun testEpsilonPointJustInsideEdgeStaysInside() {
        // Square polygon: lat [45.0, 46.0], lon [-76.0, -75.0]
        val polygon = listOf(
            Pair(45.0, -76.0),
            Pair(46.0, -76.0),
            Pair(46.0, -75.0),
            Pair(45.0, -75.0)
        )

        // Point a hair inside the right edge (lon slightly less than -75.0).
        // Guards against the epsilon over-correcting and pushing interior
        // points out; it must remain inside.
        val justInside = AlertMatcher.isPointInPolygon(45.5, -75.0000001, polygon)
        assertTrue(
            "A point just inside the polygon edge must remain inside",
            justInside
        )
    }
}
