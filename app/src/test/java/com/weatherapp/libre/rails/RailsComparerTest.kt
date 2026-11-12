// Unit tests verifying that ProtectedManifest and SizeBaseline correctly detect mismatches and regressions.
package com.weatherapp.libre.rails

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RailsComparerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun protectedIdenticalMapsHaveNoViolations() {
        val map = mapOf("a/x.kt" to "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
        val violations = ProtectedManifest.compare(map, map)
        assertEquals(emptyList<String>(), violations)
    }

    @Test
    fun protectedChangedHashIsReported() {
        val expected = mapOf("a/x.kt" to "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
        val actual = mapOf("a/x.kt" to "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        val violations = ProtectedManifest.compare(expected, actual)
        assertEquals(listOf("CHANGED a/x.kt"), violations)
    }

    @Test
    fun protectedMissingAndUnexpectedAreReported() {
        val expected = mapOf(
            "b.kt" to "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            "c.kt" to "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        )
        val actual = mapOf(
            "c.kt" to "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            "d.kt" to "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        )
        val violations = ProtectedManifest.compare(expected, actual)
        assertEquals(listOf("MISSING b.kt", "UNEXPECTED d.kt"), violations)
    }

    @Test
    fun manifestParseAcceptsSha256sumFormat() {
        val text = """
            # note

            ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad  a/b.kt
            E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855 *c/d.kt
        """.trimIndent()
        val parsed = ProtectedManifest.parse(text)
        val expected = mapOf(
            "a/b.kt" to "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            "c/d.kt" to "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        )
        assertEquals(expected, parsed)
    }

    @Test
    fun manifestParseRejectsMalformedLine() {
        val ex1 = assertThrows(IllegalArgumentException::class.java) {
            ProtectedManifest.parse("zz  x.kt")
        }
        assertTrue("Expected message containing 'line 1', got: ${ex1.message}", ex1.message?.contains("line 1") == true)

        val validLine = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad  a/b.kt"
        val duplicateText = "$validLine\n$validLine"
        val ex2 = assertThrows(IllegalArgumentException::class.java) {
            ProtectedManifest.parse(duplicateText)
        }
        assertTrue("Expected message containing 'line 2', got: ${ex2.message}", ex2.message?.contains("line 2") == true)
    }

    @Test
    fun hashTreeFindsLunarSolarAndGoldenOnly() {
        val root = tempFolder.newFolder("repo")
        fun createFile(relPath: String, content: String) {
            val f = File(root, relPath)
            f.parentFile?.mkdirs()
            f.writeText(content)
        }
        createFile("app/src/main/java/com/weatherapp/libre/lunar/A.kt", "abc")
        createFile("app/src/main/java/com/weatherapp/libre/solar/B.kt", "")
        createFile("app/src/test/java/p/golden/g.csv", "abc")
        createFile("theme-engine/src/main/golden/h.txt", "abc")
        createFile("app/src/main/java/com/weatherapp/libre/other/C.kt", "abc")

        val tree = ProtectedManifest.hashTree(root)
        val expectedKeys = setOf(
            "app/src/main/java/com/weatherapp/libre/lunar/A.kt",
            "app/src/main/java/com/weatherapp/libre/solar/B.kt",
            "app/src/test/java/p/golden/g.csv",
            "theme-engine/src/main/golden/h.txt"
        )
        assertEquals(expectedKeys, tree.keys)
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", tree["app/src/main/java/com/weatherapp/libre/lunar/A.kt"])
    }

    @Test
    fun sizeGrowthIsReportedShrinkIsNot() {
        val baseline = mapOf("a.kt" to 600)

        val grew = SizeBaseline.compare(baseline, mapOf("a.kt" to 601))
        assertEquals(listOf("GREW a.kt 601 > 600"), grew)

        val equal = SizeBaseline.compare(baseline, mapOf("a.kt" to 600))
        assertEquals(emptyList<String>(), equal)

        val shrunk = SizeBaseline.compare(baseline, mapOf("a.kt" to 550))
        assertEquals(emptyList<String>(), shrunk)
    }

    @Test
    fun sizeNewOffenderAndStaleEntryAreReported() {
        val baseline1 = mapOf("old.kt" to 520)
        val over1 = mapOf("new.kt" to 510)
        val res1 = SizeBaseline.compare(baseline1, over1)
        assertEquals(listOf("NEW new.kt 510", "STALE old.kt"), res1)

        val baseline2 = mapOf("a.kt" to 600)
        val over2 = emptyMap<String, Int>()
        val res2 = SizeBaseline.compare(baseline2, over2)
        assertEquals(listOf("STALE a.kt"), res2)
    }

    @Test
    fun measureUsesNewlineCountAndLimits() {
        val root = tempFolder.newFolder("repo_size")
        fun createLines(relPath: String, linesCount: Int, endingNewline: Boolean = true) {
            val f = File(root, relPath)
            f.parentFile?.mkdirs()
            val sb = StringBuilder()
            for (i in 1..linesCount) {
                sb.append("line $i\n")
            }
            if (!endingNewline) {
                sb.append("no final newline")
            }
            f.writeText(sb.toString())
        }

        createLines("app/src/main/java/p/Big.kt", 501, true)
        createLines("app/src/main/java/p/Edge.kt", 500, true)
        createLines("app/src/main/java/p/NoNewline.kt", 500, false)
        createLines("app/src/test/java/p/BigTest.kt", 801, true)
        createLines("app/src/test/java/p/MidTest.kt", 800, true)
        createLines("app/src/main/java/p/Notes.txt", 900, true)
        createLines("app/build/generated/Gen.kt", 900, true)

        val measured = SizeBaseline.measure(root)
        val expected = mapOf(
            "app/src/main/java/p/Big.kt" to 501,
            "app/src/test/java/p/BigTest.kt" to 801
        )
        assertEquals(expected, measured)
    }
}
