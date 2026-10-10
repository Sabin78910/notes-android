package com.sabin.notes

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizationTest {
    private val res = File("src/main/res")
    private val src = File("src/main/java/com/sabin/notes")
    private val entry = Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)

    private fun strings(path: String): Map<String, String> =
        entry.findAll(File(res, path).readText()).associate { it.groupValues[1] to it.groupValues[2] }

    @Test
    fun everyDefaultStringHasNepaliTranslation() {
        val default = strings("values/strings.xml")
        val ne = strings("values-ne/strings.xml")
        assertTrue(default.isNotEmpty())
        assertEquals(emptySet<String>(), default.keys - ne.keys)
        assertEquals(emptySet<String>(), ne.keys - default.keys)
        for ((k, v) in ne) assertTrue("$k is blank", v.isNotBlank())
    }

    @Test
    fun translationsKeepFormatPlaceholders() {
        val ne = strings("values-ne/strings.xml")
        val placeholder = Regex("""%\d\$[sd]""")
        for ((k, v) in strings("values/strings.xml")) {
            assertEquals("placeholders of $k", placeholder.findAll(v).map { it.value }.toSet(), placeholder.findAll(ne.getValue(k)).map { it.value }.toSet())
        }
    }

    @Test
    fun composeScreensHaveNoHardcodedText() {
        val literal = Regex("""(Text|title|label|placeholder)\s*(=|\()\s*\{?\s*(Text\()?"[A-Za-z]""")
        for (f in listOf("MainActivity.kt", "OnboardingScreen.kt")) {
            val bad = File(src, f).readLines().filter { literal.containsMatchIn(it) && !it.contains("stringResource") }
            assertEquals("$f has hardcoded UI text: $bad", emptyList<String>(), bad)
        }
    }
}
