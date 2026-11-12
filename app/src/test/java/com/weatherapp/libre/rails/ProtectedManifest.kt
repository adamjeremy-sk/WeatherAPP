// Helper for parsing, hashing, and verifying protected file paths against a SHA-256 manifest.
package com.weatherapp.libre.rails

import java.io.File
import java.security.MessageDigest

object ProtectedManifest {
    const val MANIFEST_PATH = "config/protected.sha256"

    fun parse(text: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val lines = text.lines()
        val hexRegex = Regex("^[0-9a-fA-F]{64}$")
        for ((index, rawLine) in lines.withIndex()) {
            val lineNumber = index + 1
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue
            }
            if (rawLine.length < 66) {
                throw IllegalArgumentException("Malformed manifest entry at line $lineNumber: $rawLine")
            }
            val hashPart = rawLine.substring(0, 64)
            if (!hexRegex.matches(hashPart)) {
                throw IllegalArgumentException("Malformed manifest entry at line $lineNumber: $rawLine")
            }
            val rest = rawLine.substring(64)
            if (!rest.startsWith(" ") && !rest.startsWith("\t")) {
                throw IllegalArgumentException("Malformed manifest entry at line $lineNumber: $rawLine")
            }
            var pathPart = rest.trimStart()
            if (pathPart.startsWith("*")) {
                pathPart = pathPart.substring(1).trimStart()
            }
            if (pathPart.isEmpty()) {
                throw IllegalArgumentException("Malformed manifest entry at line $lineNumber: $rawLine")
            }
            val pathNormalized = pathPart.replace('\\', '/')
            val hashLower = hashPart.lowercase()
            if (result.containsKey(pathNormalized)) {
                throw IllegalArgumentException("Duplicate path at line $lineNumber: $pathNormalized")
            }
            result[pathNormalized] = hashLower
        }
        return result
    }

    fun hashTree(root: File): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val modules = root.listFiles()?.filter { child ->
            child.isDirectory && !child.name.startsWith(".") && child.name != "build" && File(child, "src").isDirectory
        } ?: emptyList()

        val protectedFiles = mutableSetOf<File>()

        for (module in modules) {
            val srcDir = File(module, "src")
            val lunarDir = File(srcDir, "main/java/com/weatherapp/libre/lunar")
            if (lunarDir.isDirectory) {
                lunarDir.walkTopDown().filter { it.isFile }.forEach { protectedFiles.add(it) }
            }
            val solarDir = File(srcDir, "main/java/com/weatherapp/libre/solar")
            if (solarDir.isDirectory) {
                solarDir.walkTopDown().filter { it.isFile }.forEach { protectedFiles.add(it) }
            }
            findGoldenFiles(srcDir, protectedFiles)
        }

        for (file in protectedFiles.sortedBy { it.relativeTo(root).path.replace('\\', '/') }) {
            val relPath = file.relativeTo(root).path.replace('\\', '/')
            result[relPath] = sha256Hex(file)
        }

        return result
    }

    fun compare(expected: Map<String, String>, actual: Map<String, String>): List<String> {
        val violations = mutableListOf<String>()
        val allPaths = (expected.keys + actual.keys).toSet().sorted()
        for (path in allPaths) {
            val expHash = expected[path]
            val actHash = actual[path]
            if (expHash != null && actHash != null) {
                if (expHash != actHash) {
                    violations.add("CHANGED $path")
                }
            } else if (expHash != null) {
                violations.add("MISSING $path")
            } else if (actHash != null) {
                violations.add("UNEXPECTED $path")
            }
        }
        return violations
    }

    private fun findGoldenFiles(dir: File, outFiles: MutableSet<File>) {
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                if (child.name == "golden") {
                    child.walkTopDown().filter { it.isFile }.forEach { outFiles.add(it) }
                } else {
                    findGoldenFiles(child, outFiles)
                }
            }
        }
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = file.readBytes()
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
