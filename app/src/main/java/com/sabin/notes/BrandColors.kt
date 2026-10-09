package com.sabin.notes

/** Brand fallback palette (seed amber #F59E0B) used when dynamic colour is unavailable (pre-Android 12). */
object BrandColors {
    const val SEED = 0xFFF59E0B
    const val CARD_CORNER_DP = 26

    class Scheme(
        val primary: Long, val onPrimary: Long,
        val primaryContainer: Long, val onPrimaryContainer: Long,
        val secondary: Long, val onSecondary: Long,
        val secondaryContainer: Long, val onSecondaryContainer: Long,
        val background: Long, val onBackground: Long,
        val surfaceVariant: Long, val onSurfaceVariant: Long
    )

    val light = Scheme(
        primary = 0xFF8A5100, onPrimary = 0xFFFFFFFF,
        primaryContainer = SEED, onPrimaryContainer = 0xFF2B1700,
        secondary = 0xFF755A2F, onSecondary = 0xFFFFFFFF,
        secondaryContainer = 0xFFFFDDAE, onSecondaryContainer = 0xFF281900,
        background = 0xFFFFF8F2, onBackground = 0xFF1F1B16,
        surfaceVariant = 0xFFF0E0CF, onSurfaceVariant = 0xFF4F4539
    )

    val dark = Scheme(
        primary = 0xFFFFB955, onPrimary = 0xFF492900,
        primaryContainer = 0xFF693C00, onPrimaryContainer = 0xFFFFDDB8,
        secondary = 0xFFE4C18D, onSecondary = 0xFF422C05,
        secondaryContainer = 0xFF5B421A, onSecondaryContainer = 0xFFFFDDAE,
        background = 0xFF17130B, onBackground = 0xFFEBE1D9,
        surfaceVariant = 0xFF4F4539, onSurfaceVariant = 0xFFD3C4B4
    )
}
