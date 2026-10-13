package com.weatherapp.libre.solar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SolarObserverTest {

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
    fun testHorizonDipCalculation() {
        val observer = Observer(45.0, -75.0, elevationMeters = 1000.0)
        assertEquals(1.015, observer.horizonDipDegrees, 0.01)
    }

    @Test
    fun testBelowSeaLevel() {
        val observer = Observer(45.0, -75.0, elevationMeters = -50.0)
        assertEquals(0.0, observer.horizonDipDegrees, 1e-12)
    }
}
