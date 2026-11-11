package com.localplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenreMapperTest {

    @Test
    fun subgenresAreFiledUnderTheirTopGenre() {
        assertEquals("Rock", GenreMapper.parentOf("Alternative rock"))
        assertEquals("Rock", GenreMapper.parentOf("indie rock"))
        assertEquals("Rock", GenreMapper.parentOf("Post-Punk"))
        assertEquals("Hip-Hop", GenreMapper.parentOf("Trap"))
        assertEquals("Electronic", GenreMapper.parentOf("Drum & Bass"))
        assertEquals("R&B", GenreMapper.parentOf("R&B"))
        assertEquals("R&B", GenreMapper.parentOf("rnb"))
        assertEquals("Folk", GenreMapper.parentOf("Singer-Songwriter"))
    }

    @Test
    fun unlistedGenresUseTheLastKnownGenreInTheirName() {
        assertEquals("Metal", GenreMapper.parentOf("Progressive Death Metal"))
        assertEquals("Hip-Hop", GenreMapper.parentOf("jazz rap"))
        assertEquals("Metal", GenreMapper.parentOf("folk metal"))
        assertEquals("Rock", GenreMapper.parentOf("Swedish indie rock"))
    }

    @Test
    fun knownGenresOnlyMatchWholeWords() {
        // "ost" (soundtrack) must not match inside "post", nor "emo" inside "demo"
        assertEquals("Rock", GenreMapper.parentOf("post-hardcore"))
        assertEquals("Other", GenreMapper.parentOf("demo"))
        assertEquals("Other", GenreMapper.parentOf("Alaska"))
    }

    @Test
    fun unknownGenresGoToOther() {
        assertEquals("Other", GenreMapper.parentOf(null))
        assertEquals("Other", GenreMapper.parentOf(""))
        assertEquals("Other", GenreMapper.parentOf("Icelandic"))
        assertFalse(GenreMapper.isKnown("seen live"))
        assertTrue(GenreMapper.isKnown("punk"))
    }

    @Test
    fun differentSpellingsGetOneName() {
        assertEquals("Indie Rock", GenreMapper.displayName("indie rock"))
        assertEquals("Indie Rock", GenreMapper.displayName("Indie-Rock"))
        assertEquals("Hip-Hop", GenreMapper.displayName("hip hop"))
        assertEquals("Hip-Hop", GenreMapper.displayName("HipHop"))
        assertEquals("R&B", GenreMapper.displayName("rnb"))
        assertEquals("Drum & Bass", GenreMapper.displayName("dnb"))
        assertEquals("Other", GenreMapper.displayName(""))
    }

    @Test
    fun aTopGenreIsItsOwnParentAndName() {
        // This is what puts a track tagged just "Rock" directly in Rock
        GenreMapper.topGenres.forEach { top ->
            assertEquals(top, GenreMapper.parentOf(top))
            assertEquals(top, GenreMapper.displayName(top))
        }
    }

    @Test
    fun fileTagsKeepTheFirstKnownGenre() {
        assertEquals("Indie Rock", GenreMapper.specificGenre("indie rock"))
        assertEquals("Soul", GenreMapper.specificGenre("Unknown; Soul / Funk"))
        assertEquals("Other", GenreMapper.specificGenre("(17)"))
        assertEquals("Other", GenreMapper.specificGenre(null))
    }
}
