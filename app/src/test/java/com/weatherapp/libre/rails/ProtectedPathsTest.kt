// Tests that protected astronomy source files match the SHA-256 manifest.
// Manifest regeneration command:
// cd ~/AndroidStudioProjects/WeatherAPP && { find app/src/main/java/com/weatherapp/libre/lunar app/src/main/java/com/weatherapp/libre/solar -type f; find app/src theme-engine/src -type d -name golden -exec find {} -type f ';'; } | LC_ALL=C sort | xargs sha256sum > config/protected.sha256
package com.weatherapp.libre.rails

import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class ProtectedPathsTest {

    @Test
    fun protectedFilesMatchManifest() {
        val root = RepoRoot.find()
        val manifestFile = File(root, ProtectedManifest.MANIFEST_PATH)
        if (!manifestFile.exists()) {
            fail("config/protected.sha256 is missing")
        }
        val expected = ProtectedManifest.parse(manifestFile.readText())
        val actual = ProtectedManifest.hashTree(root)
        val violations = ProtectedManifest.compare(expected, actual)
        if (violations.isNotEmpty()) {
            val msg = violations.joinToString("\n") +
                "\nA protected path may be edited only in a gate whose prompt carries an UNLOCK line (AGENTS.md rule 23); the manifest is regenerated only with the command in this file's header."
            fail(msg)
        }
    }
}
