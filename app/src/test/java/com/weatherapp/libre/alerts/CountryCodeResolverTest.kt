package com.weatherapp.libre.alerts

import com.weatherapp.libre.alerts.utils.CountryCodeMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountryCodeResolverTest {

    @Test
    fun testCountryCodeMapperMapsKnownCountries() {
        assertEquals("DE", CountryCodeMapper.getIsoCodeForCountryName("Germany"))
        assertEquals("FR", CountryCodeMapper.getIsoCodeForCountryName("France"))
        assertEquals("IT", CountryCodeMapper.getIsoCodeForCountryName("Italy"))
        assertEquals("ES", CountryCodeMapper.getIsoCodeForCountryName("Spain"))
        assertEquals("CA", CountryCodeMapper.getIsoCodeForCountryName("Canada"))
        assertEquals("US", CountryCodeMapper.getIsoCodeForCountryName("United States"))
        assertEquals("AU", CountryCodeMapper.getIsoCodeForCountryName("Australia"))
        assertEquals("NZ", CountryCodeMapper.getIsoCodeForCountryName("New Zealand"))
        assertEquals("GB", CountryCodeMapper.getIsoCodeForCountryName("United Kingdom"))
    }

    @Test
    fun testCountryCodeMapperHandlesUnknownOrNull() {
        assertNull(CountryCodeMapper.getIsoCodeForCountryName(null))
        assertNull(CountryCodeMapper.getIsoCodeForCountryName(""))
        assertNull(CountryCodeMapper.getIsoCodeForCountryName("Unknown Atlantis"))
    }
}
