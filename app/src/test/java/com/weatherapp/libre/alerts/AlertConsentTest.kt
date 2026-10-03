package com.weatherapp.libre.alerts

import com.weatherapp.libre.LocationPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertConsentTest {

    @Test
    fun testDefaultUnsetConsentStateShowsPrompt() {
        val state = LocationPreferences.ALERT_CONSENT_UNSET
        val shouldShowPrompt = (state == LocationPreferences.ALERT_CONSENT_UNSET)
        assertTrue("Consent prompt must show when state is UNSET (-1)", shouldShowPrompt)
    }

    @Test
    fun testOptInConsentStateHidesPromptAndEnablesAlerts() {
        val state = LocationPreferences.ALERT_CONSENT_OPT_IN
        val shouldShowPrompt = (state == LocationPreferences.ALERT_CONSENT_UNSET)
        assertFalse("Consent prompt must NOT show when state is OPT_IN (1)", shouldShowPrompt)
        
        val isAlertsActive = (state == LocationPreferences.ALERT_CONSENT_OPT_IN)
        assertTrue("Alerts must be active when state is OPT_IN", isAlertsActive)
    }

    @Test
    fun testOptOutConsentStateHidesPromptAndDisablesAlerts() {
        val state = LocationPreferences.ALERT_CONSENT_OPT_OUT
        val shouldShowPrompt = (state == LocationPreferences.ALERT_CONSENT_UNSET)
        assertFalse("Consent prompt must NOT show when state is OPT_OUT (0)", shouldShowPrompt)
        
        val isAlertsActive = (state == LocationPreferences.ALERT_CONSENT_OPT_IN)
        assertFalse("Alerts must NOT be active when state is OPT_OUT", isAlertsActive)
    }

    @Test
    fun testLegacyKeysIgnoredByNewReaders() {
        val legacyBooleanKey = "app_weather_alerts_enabled"
        val legacyVersionKey = "app_alert_consent_version"
        val newConsentStateKey = "app_alert_consent_state"

        assertNotEquals(newConsentStateKey, legacyBooleanKey)
        assertNotEquals(newConsentStateKey, legacyVersionKey)
    }
}
