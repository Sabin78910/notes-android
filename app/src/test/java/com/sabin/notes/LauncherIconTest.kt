package com.sabin.notes

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIconTest {
    private val res = File("src/main/res")
    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun manifestUsesMipmapIcons() {
        assertTrue(manifest.contains("android:icon=\"@mipmap/ic_launcher\""))
        assertTrue(manifest.contains("android:roundIcon=\"@mipmap/ic_launcher_round\""))
    }

    @Test
    fun placeholderRemoved() = assertFalse(File(res, "drawable/ic_launcher.xml").exists())

    @Test
    fun adaptiveIconsReferenceBackgroundAndForeground() {
        for (name in listOf("ic_launcher", "ic_launcher_round")) {
            val xml = File(res, "mipmap-anydpi-v26/$name.xml").readText()
            assertTrue(xml.contains("<adaptive-icon"))
            assertTrue(xml.contains("@drawable/ic_launcher_background"))
            assertTrue(xml.contains("@drawable/ic_launcher_foreground"))
        }
    }

    @Test
    fun iconUsesStoreColors() {
        val bg = File(res, "drawable/ic_launcher_background.xml").readText()
        val fg = File(res, "drawable/ic_launcher_foreground.xml").readText()
        assertTrue(bg.contains("#F5A623"))
        assertTrue(fg.contains("#FFFFFF"))
        assertTrue(fg.contains("#D6780A"))
    }
}
