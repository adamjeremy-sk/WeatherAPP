// Tests that source files do not grow beyond established line count baseline limits.
package com.weatherapp.libre.rails

import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class SizeRatchetTest {

    @Test
    fun noFileGrowsPastItsBaseline() {
        val root = RepoRoot.find()
        val baselineFile = File(root, SizeBaseline.BASELINE_PATH)
        if (!baselineFile.exists()) {
            fail("config/size-baseline.txt is missing")
        }
        val baseline = SizeBaseline.parse(baselineFile.readText())
        val over = SizeBaseline.measure(root)
        val violations = SizeBaseline.compare(baseline, over)
        if (violations.isNotEmpty()) {
            val msg = violations.joinToString("\n") +
                "\nSplit the file (AGENTS.md rule 16); never raise a number in config/size-baseline.txt."
            fail(msg)
        }
    }
}
