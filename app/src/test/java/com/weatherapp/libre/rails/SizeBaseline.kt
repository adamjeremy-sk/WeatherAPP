// Helper for parsing, measuring, and comparing file size baseline ratchets.
package com.weatherapp.libre.rails

import java.io.File

object SizeBaseline {
    const val BASELINE_PATH = "config/size-baseline.txt"
    const val MAIN_LIMIT = 500
    const val TEST_LIMIT = 800

    fun parse(text: String): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        val lines = text.lines()
        for ((index, rawLine) in lines.withIndex()) {
            val lineNumber = index + 1
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue
            }
            val spaceIndex = rawLine.indexOf(' ')
            if (spaceIndex <= 0 || spaceIndex == rawLine.length - 1) {
                throw IllegalArgumentException("Malformed baseline entry at line $lineNumber: $rawLine")
            }
            val countStr = rawLine.substring(0, spaceIndex)
            val pathStr = rawLine.substring(spaceIndex + 1).trim()
            val count = countStr.toIntOrNull()
            if (count == null || count < 0 || pathStr.isEmpty()) {
                throw IllegalArgumentException("Malformed baseline entry at line $lineNumber: $rawLine")
            }
            val normalizedPath = pathStr.replace('\\', '/')
            if (result.containsKey(normalizedPath)) {
                throw IllegalArgumentException("Duplicate path at line $lineNumber: $normalizedPath")
            }
            result[normalizedPath] = count
        }
        return result
    }

    fun measure(root: File): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        val modules = root.listFiles()?.filter { child ->
            child.isDirectory && !child.name.startsWith(".") && child.name != "build" && File(child, "src").isDirectory
        } ?: emptyList()

        val ktFiles = mutableListOf<File>()
        for (module in modules) {
            val srcDir = File(module, "src")
            srcDir.walkTopDown().filter { it.isFile && it.name.endsWith(".kt") }.forEach { ktFiles.add(it) }
        }

        for (file in ktFiles.sortedBy { it.relativeTo(root).path.replace('\\', '/') }) {
            val relPath = file.relativeTo(root).path.replace('\\', '/')
            val limit = when {
                relPath.contains("/src/main/") -> MAIN_LIMIT
                relPath.contains("/src/test/") || relPath.contains("/src/androidTest/") -> TEST_LIMIT
                else -> null
            } ?: continue

            val linesCount = countNewlines(file)
            if (linesCount > limit) {
                result[relPath] = linesCount
            }
        }
        return result
    }

    fun compare(baseline: Map<String, Int>, over: Map<String, Int>): List<String> {
        val violations = mutableListOf<String>()
        val allPaths = (baseline.keys + over.keys).toSet().sorted()
        for (path in allPaths) {
            val overLines = over[path]
            val baseLines = baseline[path]
            if (overLines != null) {
                if (baseLines != null) {
                    if (overLines > baseLines) {
                        violations.add("GREW $path $overLines > $baseLines")
                    }
                } else {
                    violations.add("NEW $path $overLines")
                }
            } else if (baseLines != null) {
                violations.add("STALE $path")
            }
        }
        return violations
    }

    private fun countNewlines(file: File): Int {
        var count = 0
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                for (i in 0 until bytesRead) {
                    if (buffer[i] == '\n'.code.toByte()) {
                        count++
                    }
                }
            }
        }
        return count
    }
}
