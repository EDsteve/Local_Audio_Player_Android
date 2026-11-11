package com.localplayer.ui.screens

/** "1 track", "2 tracks": a count with its noun in the right form. */
internal fun countLabel(count: Int, noun: String): String =
    if (count == 1) "1 $noun" else "$count ${noun}s"
