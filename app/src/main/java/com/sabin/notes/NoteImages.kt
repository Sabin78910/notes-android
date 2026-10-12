package com.sabin.notes

import java.io.File
import java.util.UUID

/** Photo file names: only plain `[A-Za-z0-9_-]` stems with an extension, so a name can never escape the images folder. */
object ImageName {
    private val safe = Regex("[A-Za-z0-9_-]{1,64}\\.[A-Za-z0-9]{1,5}")
    fun isSafe(name: String): Boolean = safe.matches(name)
}

/** Size maths for importing photos: longest edge is capped, aspect ratio kept, never upscaled. */
object ImageScale {
    const val MAX_EDGE = 2048
    data class Size(val width: Int, val height: Int)

    fun fit(width: Int, height: Int, max: Int = MAX_EDGE): Size {
        val longest = maxOf(width, height)
        if (longest <= max) return Size(width, height)
        val f = max.toDouble() / longest
        return Size(maxOf(1, Math.round(width * f).toInt()), maxOf(1, Math.round(height * f).toInt()))
    }

    /** Power-of-two decode sample size that keeps the longest edge at or above [max]. */
    fun sampleSize(width: Int, height: Int, max: Int): Int {
        var s = 1
        while (maxOf(width, height) / (s * 2) >= max) s *= 2
        return s
    }
}

/** The app-private folder holding attached photos. */
class ImageFiles(private val dir: File) {
    init { dir.mkdirs() }

    fun newName(ext: String): String = "${UUID.randomUUID()}.$ext"

    fun file(name: String): File = File(dir, name)

    fun fileOrNull(name: String): File? = if (ImageName.isSafe(name)) file(name) else null

    fun names(): List<String> = dir.list()?.filter(ImageName::isSafe)?.sorted() ?: emptyList()

    /** Deletes every stored photo that no note references (e.g. after Trash is emptied or purged). */
    fun sweep(store: NoteStore) {
        store.orphanedImages(names()).forEach { file(it).delete() }
    }
}
