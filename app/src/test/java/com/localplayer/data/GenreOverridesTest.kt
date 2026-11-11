package com.localplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenreOverridesTest {

    @Test
    fun trackGenreWinsOverArtistGenre() {
        val overrides = mapOf(
            GenreOverrides.artistKey("Dusted") to "Indie Rock",
            GenreOverrides.trackKey("track-1") to "Folk"
        )
        assertEquals("Folk", GenreOverrides.manualGenre(overrides, "track-1", "Dusted"))
        assertEquals("Indie Rock", GenreOverrides.manualGenre(overrides, "track-2", "Dusted"))
    }

    @Test
    fun artistGenreIgnoresCaseAndSpacing() {
        val overrides = mapOf(GenreOverrides.artistKey("Dusted") to "Indie Rock")
        assertEquals("Indie Rock", GenreOverrides.manualGenre(overrides, "track-3", " dusted "))
    }

    @Test
    fun nothingSetMeansNoManualGenre() {
        val overrides = mapOf(GenreOverrides.artistKey("Dusted") to "Indie Rock")
        assertNull(GenreOverrides.manualGenre(overrides, "track-4", "Charlie Mars"))
    }

    @Test
    fun everyChoiceIsFiledUnderItsTopGenre() {
        // Picking a genre in the dialog must land the track where the dialog says
        GenreMapper.choices.forEach { choice ->
            assertEquals(choice.name, choice.topGenre, GenreMapper.parentOf(choice.name))
            assertEquals(choice.name, GenreMapper.displayName(choice.name))
        }
        assertTrue(GenreMapper.choices.any { it.name == "Indie Rock" && it.topGenre == "Rock" })
        assertFalse(GenreMapper.choices.any { it.name == "Other" })
    }
}
