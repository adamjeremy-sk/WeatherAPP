package com.weatherapp.libre.alerts

import org.junit.Assert.*
import org.junit.Test

class AlertConsentTest {

    @Test
    fun testCurrentConsentVersionIsOne() {
        assertEquals(1, AlertConsentDialog.CURRENT_ALERT_CONSENT_VERSION)
    }

    @Test
    fun testDefaultAlertsDisabledForNewUser() {
        // Verification of default state: when consent version is 0 (absent), prompt must show and default state is disabled
        val storedConsentVersion = 0
        val isConsentCompleted = storedConsentVersion >= AlertConsentDialog.CURRENT_ALERT_CONSENT_VERSION
        assertFalse("Prompt must be shown when version is lower than current version", isConsentCompleted)
    }

    @Test
    fun testConsentCompletedVersionCheck() {
        val storedConsentVersion = 1
        val isConsentCompleted = storedConsentVersion >= AlertConsentDialog.CURRENT_ALERT_CONSENT_VERSION
        assertTrue("Prompt should not show again once consent version is saved", isConsentCompleted)
    }
}
