package com.sabin.notes

object PinPresentation {
    fun badge(pinned: Boolean): String? = if (pinned) "Pinned" else null
    fun buttonLabel(pinned: Boolean): String = if (pinned) "Unpin" else "Pin"
}
