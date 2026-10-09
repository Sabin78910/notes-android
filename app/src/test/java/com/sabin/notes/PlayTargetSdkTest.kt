package com.sabin.notes

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayTargetSdkTest {
    private val script = File("build.gradle.kts").readText()

    @Test
    fun compileSdkIs36() = assertTrue(Regex("""compileSdk\s*=\s*36\b""").containsMatchIn(script))

    @Test
    fun targetSdkIs36() = assertTrue(Regex("""targetSdk\s*=\s*36\b""").containsMatchIn(script))
}
