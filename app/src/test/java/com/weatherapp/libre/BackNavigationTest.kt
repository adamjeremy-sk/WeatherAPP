package com.weatherapp.libre

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackNavigationTest {

    @Test
    fun testShouldInterceptBack_matrix() {
        // Matrix: drawer open with each subtab -> must intercept (true)
        assertTrue(
            "Drawer open on CURRENT must intercept back",
            MainActivity.shouldInterceptBack(drawerOpen = true, subTab = InlineSubTab.CURRENT)
        )
        assertTrue(
            "Drawer open on HOURLY must intercept back",
            MainActivity.shouldInterceptBack(drawerOpen = true, subTab = InlineSubTab.HOURLY)
        )
        assertTrue(
            "Drawer open on NATURAL_LIGHT must intercept back",
            MainActivity.shouldInterceptBack(drawerOpen = true, subTab = InlineSubTab.NATURAL_LIGHT)
        )

        // Matrix: drawer closed with each subtab
        assertFalse(
            "Drawer closed on CURRENT must NOT intercept back (system default exit)",
            MainActivity.shouldInterceptBack(drawerOpen = false, subTab = InlineSubTab.CURRENT)
        )
        assertTrue(
            "Drawer closed on HOURLY must intercept back (return to today's weather)",
            MainActivity.shouldInterceptBack(drawerOpen = false, subTab = InlineSubTab.HOURLY)
        )
        assertTrue(
            "Drawer closed on NATURAL_LIGHT must intercept back (return to today's weather)",
            MainActivity.shouldInterceptBack(drawerOpen = false, subTab = InlineSubTab.NATURAL_LIGHT)
        )
    }
}
