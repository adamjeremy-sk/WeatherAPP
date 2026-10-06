package com.weatherapp.libre

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabLongPressTest {

    @Test
    fun testShouldReloadAfterTabEdit_equalIndexes_returnsTrue() {
        assertTrue(
            "Equal indexes must return true for refresh",
            MainActivity.shouldReloadAfterTabEdit(pressedIndex = 1, activeIndex = 1)
        )
    }

    @Test
    fun testShouldReloadAfterTabEdit_differingIndexes_returnsFalse() {
        assertFalse(
            "Differing indexes must return false for refresh",
            MainActivity.shouldReloadAfterTabEdit(pressedIndex = 0, activeIndex = 1)
        )
        assertFalse(
            "Differing indexes must return false for refresh",
            MainActivity.shouldReloadAfterTabEdit(pressedIndex = 2, activeIndex = 1)
        )
    }

    @Test
    fun testShouldReloadAfterTabEdit_boundaryIndexes_zeroAndCountMinusOne() {
        assertTrue(
            "Index 0 equal active 0 must return true",
            MainActivity.shouldReloadAfterTabEdit(pressedIndex = 0, activeIndex = 0)
        )
        assertFalse(
            "Index 0 differing from active 3 must return false",
            MainActivity.shouldReloadAfterTabEdit(pressedIndex = 0, activeIndex = 3)
        )

        val maxIndex = 3
        assertTrue(
            "Boundary index maxIndex equal active maxIndex must return true",
            MainActivity.shouldReloadAfterTabEdit(pressedIndex = maxIndex, activeIndex = maxIndex)
        )
        assertFalse(
            "Boundary index maxIndex differing from active 0 must return false",
            MainActivity.shouldReloadAfterTabEdit(pressedIndex = maxIndex, activeIndex = 0)
        )
    }
}
