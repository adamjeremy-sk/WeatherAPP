package com.weatherapp.libre

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackNavigationTest {

    @Test
    fun testShouldInterceptBack_matrix() {
        // Menu depth > 0 must intercept regardless of drawer or subtab
        assertTrue(
            "Menu depth > 0 on CURRENT with drawer closed must intercept",
            MainActivity.shouldInterceptBack(menuDepth = 1, drawerOpen = false, subTab = InlineSubTab.CURRENT)
        )
        assertTrue(
            "Menu depth > 0 on HOURLY with drawer open must intercept",
            MainActivity.shouldInterceptBack(menuDepth = 2, drawerOpen = true, subTab = InlineSubTab.HOURLY)
        )

        // Zero depth with drawer open must intercept
        assertTrue(
            "Zero depth with drawer open on CURRENT must intercept",
            MainActivity.shouldInterceptBack(menuDepth = 0, drawerOpen = true, subTab = InlineSubTab.CURRENT)
        )
        assertTrue(
            "Zero depth with drawer open on HOURLY must intercept",
            MainActivity.shouldInterceptBack(menuDepth = 0, drawerOpen = true, subTab = InlineSubTab.HOURLY)
        )

        // Zero depth with drawer closed
        assertFalse(
            "Zero depth, drawer closed on CURRENT must NOT intercept (system default exit)",
            MainActivity.shouldInterceptBack(menuDepth = 0, drawerOpen = false, subTab = InlineSubTab.CURRENT)
        )
        assertTrue(
            "Zero depth, drawer closed on HOURLY must intercept",
            MainActivity.shouldInterceptBack(menuDepth = 0, drawerOpen = false, subTab = InlineSubTab.HOURLY)
        )
        assertTrue(
            "Zero depth, drawer closed on NATURAL_LIGHT must intercept",
            MainActivity.shouldInterceptBack(menuDepth = 0, drawerOpen = false, subTab = InlineSubTab.NATURAL_LIGHT)
        )
    }
}
