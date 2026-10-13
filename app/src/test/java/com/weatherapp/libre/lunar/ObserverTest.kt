package com.weatherapp.libre.lunar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ObserverTest {

    @Test
    fun testValidConstruction() {
        val observer = Observer(45.0, -75.0)
        assertNotNull(observer)
        assertEquals(45.0, observer.latitudeDegrees, 1e-12)
        assertEquals(-75.0, observer.longitudeDegrees, 1e-12)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidLatitude() {
        Observer(91.0, -75.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidLongitude() {
        Observer(45.0, 181.0)
    }

    @Test
    fun testBoundaryValues() {
        val obs1 = Observer(-90.0, -180.0)
        assertNotNull(obs1)

        val obs2 = Observer(90.0, 180.0)
        assertNotNull(obs2)
    }

    @Test
    fun testHorizonDipExplicit() {
        val observer = Observer(45.0, -75.0, horizonDipDegrees = 0.5)
        assertEquals(0.5, observer.effectiveHorizonDipDegrees, 1e-12)
    }

    @Test
    fun testHorizonDipCalculated() {
        val observer = Observer(45.0, -75.0, elevationMeters = 100.0)
        assertEquals(0.293, observer.effectiveHorizonDipDegrees, 0.001)
    }

    @Test
    fun testBelowSeaLevel() {
        val observer = Observer(45.0, -75.0, elevationMeters = -50.0)
        assertEquals(0.0, observer.effectiveHorizonDipDegrees, 1e-12)
    }
}
