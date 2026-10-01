package com.weatherapp.libre.alerts

import com.weatherapp.libre.alerts.models.CapAlert
import org.junit.Assert.*
import org.junit.Test

class CapXmlParserTest {

    private val tornadoXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <alert xmlns="urn:oasis:names:tc:emergency:cap:1.2">
          <identifier>urn:oid:2.49.0.1.124.0.2026.CAP-CP-ECCC-TEST-TORNADO-001</identifier>
          <sender>cap-pac@ec.gc.ca</sender>
          <sent>2026-09-30T12:00:00Z</sent>
          <status>Test</status>
          <msgType>Alert</msgType>
          <scope>Public</scope>
          <info>
            <language>en-CA</language>
            <category>Met</category>
            <event>Tornado Warning</event>
            <urgency>Immediate</urgency>
            <severity>Extreme</severity>
            <certainty>Observed</certainty>
            <effective>2026-09-30T12:00:00Z</effective>
            <expires>2026-09-30T16:00:00Z</expires>
            <senderName>Environment and Climate Change Canada</senderName>
            <headline>TEST — Tornado Warning in effect for target region</headline>
            <description>TEST — ECCC meteorologists are tracking a severe thunderstorm producing a tornado.</description>
            <instruction>TEST — Take cover immediately if threatening weather approaches.</instruction>
            <web>https://weather.gc.ca/warnings/</web>
            <area>
              <areaDesc>Test Region</areaDesc>
              <circle>45.4215,-75.6972 50.0</circle>
              <geocode>
                <valueName>CLC</valueName>
                <value>054321</value>
              </geocode>
            </area>
          </info>
          <info>
            <language>fr-CA</language>
            <category>Met</category>
            <event>Alerte de tornade</event>
            <urgency>Immediate</urgency>
            <severity>Extreme</severity>
            <certainty>Observed</certainty>
            <effective>2026-09-30T12:00:00Z</effective>
            <expires>2026-09-30T16:00:00Z</expires>
            <senderName>Environnement et Changement climatique Canada</senderName>
            <headline>TEST — Alerte de tornade en vigueur</headline>
            <description>TEST — Les météorologues d'ECCC surveillent une tornade.</description>
            <instruction>TEST — Mettez-vous immédiatement à l'abri.</instruction>
            <web>https://meteo.gc.ca/warnings/</web>
            <area>
              <areaDesc>Région d'essai</areaDesc>
              <circle>45.4215,-75.6972 50.0</circle>
            </area>
          </info>
        </alert>
    """.trimIndent()

    @Test
    fun testParseTornadoWarningEnglishLocale() {
        val alert = CapXmlParser.parse(tornadoXml, targetLocale = "en-CA")
        assertNotNull(alert)
        assertEquals("urn:oid:2.49.0.1.124.0.2026.CAP-CP-ECCC-TEST-TORNADO-001", alert!!.identifier)
        assertEquals("cap-pac@ec.gc.ca", alert.sender)
        assertEquals("Test", alert.status)
        assertEquals("Alert", alert.msgType)
        assertEquals(1, alert.infoList.size)

        val info = alert.infoList.first()
        assertEquals("en-CA", info.language)
        assertEquals("Tornado Warning", info.event)
        assertEquals("Extreme", info.severity)
        assertEquals("Immediate", info.urgency)
        assertEquals("Observed", info.certainty)
        assertEquals("TEST — Tornado Warning in effect for target region", info.headline)
        assertEquals(1, info.areaList.size)

        val area = info.areaList.first()
        assertEquals("Test Region", area.areaDesc)
        assertEquals(1, area.circles.size)
        assertEquals(45.4215, area.circles.first().centerLat, 0.0001)
        assertEquals(-75.6972, area.circles.first().centerLon, 0.0001)
        assertEquals(50.0, area.circles.first().radiusKm, 0.01)
    }

    @Test
    fun testParseTornadoWarningFrenchLocale() {
        val alert = CapXmlParser.parse(tornadoXml, targetLocale = "fr-CA")
        assertNotNull(alert)
        assertEquals(1, alert!!.infoList.size)

        val info = alert.infoList.first()
        assertEquals("fr-CA", info.language)
        assertEquals("Alerte de tornade", info.event)
        assertEquals("TEST — Alerte de tornade en vigueur", info.headline)
    }

    @Test
    fun testParseUpdateAndExtractReferences() {
        val updateXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <alert xmlns="urn:oasis:names:tc:emergency:cap:1.2">
              <identifier>urn:oid:2.49.0.1.124.0.2026.CAP-CP-ECCC-TEST-TORNADO-001-UPDATE</identifier>
              <sender>cap-pac@ec.gc.ca</sender>
              <sent>2026-09-30T12:45:00Z</sent>
              <status>Test</status>
              <msgType>Update</msgType>
              <scope>Public</scope>
              <references>cap-pac@ec.gc.ca,urn:oid:2.49.0.1.124.0.2026.CAP-CP-ECCC-TEST-TORNADO-001,2026-09-30T12:00:00Z</references>
              <info>
                <language>en-CA</language>
                <category>Met</category>
                <event>Tornado Warning</event>
                <urgency>Immediate</urgency>
                <severity>Extreme</severity>
                <certainty>Observed</certainty>
                <headline>TEST — UPDATED Tornado Warning</headline>
              </info>
            </alert>
        """.trimIndent()

        val alert = CapXmlParser.parse(updateXml, targetLocale = "en-CA")
        assertNotNull(alert)
        assertEquals("Update", alert!!.msgType)
        val refs = alert.getReferencedIdentifiers()
        assertEquals(1, refs.size)
        assertEquals("urn:oid:2.49.0.1.124.0.2026.CAP-CP-ECCC-TEST-TORNADO-001", refs.first())
    }

    @Test
    fun testParseInvalidXmlReturnsNull() {
        val invalidXml = "<alert><identifier>test</identifier><invalid>"
        val alert = CapXmlParser.parse(invalidXml)
        assertNull(alert)
    }
}
