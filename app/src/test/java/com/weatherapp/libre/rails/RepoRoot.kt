// Utility for locating the repository root directory in unit tests.
package com.weatherapp.libre.rails

import java.io.File

object RepoRoot {
    fun find(start: File = File(System.getProperty("user.dir") ?: ".")): File {
        var current: File? = start.canonicalFile
        while (current != null) {
            if (File(current, "settings.gradle.kts").exists()) {
                return current.absoluteFile.normalize()
            }
            current = current.parentFile
        }
        throw IllegalStateException("Repository root containing settings.gradle.kts was not found starting from ${start.absolutePath}")
    }
}
