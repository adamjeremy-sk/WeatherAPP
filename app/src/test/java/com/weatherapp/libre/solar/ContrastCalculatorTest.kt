package com.weatherapp.libre.solar

import org.junit.Test
import kotlin.math.pow

class ContrastCalculatorTest {

    private fun parseColorHex(hex: String): Int {
        val clean = hex.removePrefix("#")
        val colorLong = clean.toLong(16)
        return if (clean.length == 6) {
            (0xFF000000.toLong() or colorLong).toInt()
        } else {
            colorLong.toInt()
        }
    }

    private fun red(c: Int) = (c shr 16) and 0xFF
    private fun green(c: Int) = (c shr 8) and 0xFF
    private fun blue(c: Int) = c and 0xFF

    private fun luminance(color: Int): Double {
        val r = red(color) / 255.0
        val g = green(color) / 255.0
        val b = blue(color) / 255.0

        val rs = if (r <= 0.04045) r / 12.92 else ((r + 0.055) / 1.055).pow(2.4)
        val gs = if (g <= 0.04045) g / 12.92 else ((g + 0.055) / 1.055).pow(2.4)
        val bs = if (b <= 0.04045) b / 12.92 else ((b + 0.055) / 1.055).pow(2.4)

        return 0.2126 * rs + 0.7152 * gs + 0.0722 * bs
    }

    private fun contrastRatio(c1Hex: String, c2Hex: String): Double {
        val c1 = parseColorHex(c1Hex)
        val c2 = parseColorHex(c2Hex)
        val l1 = luminance(c1)
        val l2 = luminance(c2)
        return if (l1 > l2) (l1 + 0.05) / (l2 + 0.05) else (l2 + 0.05) / (l1 + 0.05)
    }

    data class PaletteData(
        val name: String,
        val surfaceCard: String,
        val textPrimary: String,
        val textSecondary: String,
        val alertExtreme: String,
        val alertSevere: String,
        val alertModerate: String,
        val alertMinor: String
    )

    @Test
    fun calculateAllPalettes() {
        val palettes = listOf(
            PaletteData("LIGHT", "#FFFFFF", "#0F172A", "#475569", "#DC2626", "#C2410C", "#92400E", "#0369A1"),
            PaletteData("DARK", "#1E293B", "#FFFFFF", "#CBD5E1", "#FF6B6B", "#FF9900", "#FFCC00", "#38BDF8"),
            PaletteData("CREMA", "#FAF5EF", "#1C120C", "#5A4B43", "#B91C1C", "#C2410C", "#92400E", "#0369A1"),
            PaletteData("CHARCOAL", "#212121", "#FFFFFF", "#CCCCCC", "#FF6B6B", "#FF9900", "#FFCC00", "#64B5F6"),
            PaletteData("COBALT", "#001F54", "#FFFFFF", "#B8C0E0", "#FF6B6B", "#FF9900", "#FFCC00", "#38BDF8"),
            PaletteData("JADE", "#132E27", "#FFFFFF", "#A7F3D0", "#FF7675", "#FF9900", "#FFCC00", "#2DD4BF"),
            PaletteData("AURORA", "#1B1D28", "#FFFFFF", "#CBD5E1", "#FF75C3", "#FF9900", "#FFCC00", "#818CF8"),
            PaletteData("TERRACOTTA", "#FDF8F5", "#1F110B", "#5C3C32", "#B91C1C", "#C2410C", "#92400E", "#0369A1"),
            PaletteData("FROST", "#E0F2FE", "#031B2E", "#1E40AF", "#B91C1C", "#C2410C", "#92400E", "#034078"),
            PaletteData("FIRE_ENGINE", "#FFFFFF", "#1A1A1A", "#4B5563", "#DC2626", "#C2410C", "#92400E", "#0369A1"),
            PaletteData("ORANGE_GROVE", "#FAF7EF", "#0F240A", "#2D4A21", "#B91C1C", "#C2410C", "#92400E", "#0369A1")
        )

        for (p in palettes) {
            val bg = p.surfaceCard
            val tp = contrastRatio(p.textPrimary, bg)
            val ts = contrastRatio(p.textSecondary, bg)
            val ex = contrastRatio(p.alertExtreme, bg)
            val sv = contrastRatio(p.alertSevere, bg)
            val mo = contrastRatio(p.alertModerate, bg)
            val mi = contrastRatio(p.alertMinor, bg)

            println("=== PALETTE: ${p.name} (surfaceCard: $bg) ===")
            println("textPrimary (${p.textPrimary}): ${String.format("%.2f", tp)}:1 ${if (tp >= 4.5) "PASS" else "FAIL"}")
            println("textSecondary (${p.textSecondary}): ${String.format("%.2f", ts)}:1 ${if (ts >= 4.5) "PASS" else "FAIL"}")
            println("alertExtreme (${p.alertExtreme}): ${String.format("%.2f", ex)}:1 ${if (ex >= 4.5) "PASS" else "FAIL"}")
            println("alertSevere (${p.alertSevere}): ${String.format("%.2f", sv)}:1 ${if (sv >= 4.5) "PASS" else "FAIL"}")
            println("alertModerate (${p.alertModerate}): ${String.format("%.2f", mo)}:1 ${if (mo >= 4.5) "PASS" else "FAIL"}")
            println("alertMinor (${p.alertMinor}): ${String.format("%.2f", mi)}:1 ${if (mi >= 4.5) "PASS" else "FAIL"}")
            println()

            org.junit.Assert.assertTrue("textPrimary contrast fail on ${p.name}", tp >= 4.5)
            org.junit.Assert.assertTrue("textSecondary contrast fail on ${p.name}", ts >= 4.5)
            org.junit.Assert.assertTrue("alertExtreme contrast fail on ${p.name}", ex >= 4.5)
            org.junit.Assert.assertTrue("alertSevere contrast fail on ${p.name}", sv >= 4.5)
            org.junit.Assert.assertTrue("alertModerate contrast fail on ${p.name}", mo >= 4.5)
            org.junit.Assert.assertTrue("alertMinor contrast fail on ${p.name}", mi >= 4.5)
        }
    }
}
