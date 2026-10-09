package com.sabin.notes

import kotlin.math.pow

/** Eight label colours; backgrounds/text are ARGB ints chosen to meet WCAG AA (4.5:1) in both themes. */
enum class NoteColor(val label: String, val lightBg: Long, val darkBg: Long) {
    RED("Red", 0xFFFFCDD2, 0xFF5C1A1A),
    ORANGE("Orange", 0xFFFFE0B2, 0xFF5A3300),
    YELLOW("Yellow", 0xFFFFF59D, 0xFF4F4600),
    GREEN("Green", 0xFFC8E6C9, 0xFF1B4D1F),
    TEAL("Teal", 0xFFB2DFDB, 0xFF004D47),
    BLUE("Blue", 0xFFBBDEFB, 0xFF12385F),
    PURPLE("Purple", 0xFFE1BEE7, 0xFF43204D),
    GREY("Grey", 0xFFE0E0E0, 0xFF3A3A3A);

    fun background(dark: Boolean): Long = if (dark) darkBg else lightBg
    fun text(dark: Boolean): Long = if (dark) 0xFFFFFFFF else 0xFF000000

    fun contrast(dark: Boolean): Double = contrastRatio(background(dark), text(dark))

    companion object {
        fun fromName(name: String?): NoteColor? = entries.firstOrNull { it.name == name }

        private fun luminance(argb: Long): Double {
            fun ch(shift: Int): Double {
                val c = ((argb shr shift) and 0xFF) / 255.0
                return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
            }
            return 0.2126 * ch(16) + 0.7152 * ch(8) + 0.0722 * ch(0)
        }

        fun contrastRatio(a: Long, b: Long): Double {
            val (hi, lo) = luminance(a).let { x -> luminance(b).let { y -> maxOf(x, y) to minOf(x, y) } }
            return (hi + 0.05) / (lo + 0.05)
        }
    }
}
