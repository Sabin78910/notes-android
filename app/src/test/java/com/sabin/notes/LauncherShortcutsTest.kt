package com.sabin.notes

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherShortcutsTest {
    private val res = File("src/main/res")
    private val shortcuts = File(res, "xml/shortcuts.xml").readText()
    private val extraValue = Regex("""<extra[^>]*android:name="${Regex.escape(LaunchAction.EXTRA)}"[^>]*android:value="([^"]+)"""")

    @Test
    fun manifestLinksShortcutsOnLauncherActivity() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android.app.shortcuts"))
        assertTrue(manifest.contains("@xml/shortcuts"))
    }

    @Test
    fun everyShortcutExtraRoundTripsThroughFromName() {
        val names = extraValue.findAll(shortcuts).map { it.groupValues[1] }.toList()
        assertEquals(LaunchAction.entries.map { it.wireName }.toSet(), names.toSet())
        assertEquals(Regex("<shortcut\\b").findAll(shortcuts).count(), names.size)
        for (n in names) assertEquals(n, LaunchAction.fromName(n)?.wireName)
    }

    @Test
    fun shortcutsTargetMainActivityAndUseStringLabels() {
        assertEquals(2, Regex("""android:targetClass="com\.sabin\.notes\.MainActivity"""").findAll(shortcuts).count())
        for (attr in listOf("shortcutShortLabel", "shortcutLongLabel", "icon")) {
            assertEquals(attr, 2, Regex("""android:$attr="@""").findAll(shortcuts).count())
        }
    }

    @Test
    fun fromNameIsSafeForUnknownOrNull() {
        assertNull(LaunchAction.fromName(null))
        assertNull(LaunchAction.fromName("bogus"))
        assertNull(LaunchAction.fromName(""))
    }
}
