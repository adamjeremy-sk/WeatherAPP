package com.weatherapp.libre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetricCellTest {

    @Test
    fun testMetricCellModel() {
        val cell = MetricCell(
            type = MetricType.WIND,
            title = "Wind",
            value = "15 km/h",
            secondary = "NW",
            iconResId = 0
        )
        assertEquals(MetricType.WIND, cell.type)
        assertEquals("Wind", cell.title)
        assertEquals("15 km/h", cell.value)
        assertEquals("NW", cell.secondary)
    }

    @Test
    fun testMetricCellModel_secondaryIsNull() {
        val cell = MetricCell(
            type = MetricType.HUMIDITY,
            title = "Humidity",
            value = "80%",
            secondary = null,
            iconResId = 0
        )
        assertNull(cell.secondary)
    }
}
