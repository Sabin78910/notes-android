package com.sabin.notes

object PinPresentation {
    fun badge(pinned: Boolean): String? = if (pinned) "Pinned" else null
    fun buttonLabel(pinned: Boolean): String = if (pinned) "Unpin" else "Pin"

    private const val MAX_SUMMARY = 40

    private fun summary(text: String): String {
        val line = text.trim().lineSequence().first()
        return if (line.length > MAX_SUMMARY) line.take(MAX_SUMMARY) + "…" else line
    }

    fun pinDescription(pinned: Boolean, text: String): String = "${buttonLabel(pinned)} note: ${summary(text)}"
    fun editDescription(text: String): String = "Edit note: ${summary(text)}"
    fun deleteDescription(text: String): String = "Delete note: ${summary(text)}"
}
