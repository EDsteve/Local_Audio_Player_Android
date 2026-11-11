package com.localplayer.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistParserTest {

    @Test
    fun spellingVariantsAreTheSameArtist() {
        assertTrue(ArtistParser.isSameArtist("Seals and Crofts", "Seals & Crofts"))
        assertTrue(ArtistParser.isSameArtist("Sharon Jones & The Dap-Kings", "Sharon Jones and the Dap-Kings"))
        assertTrue(ArtistParser.isSameArtist("The Chocolate Watchband", "The Chocolate Watch Band"))
        assertTrue(ArtistParser.isSameArtist("Sergio Mendes & Brasil '66", "Sérgio Mendes & Brasil '66"))
        assertTrue(ArtistParser.isSameArtist("Beatles", "The Beatles"))
        assertTrue(ArtistParser.isSameArtist("U2", "u2"))
    }

    @Test
    fun differentLettersAreADifferentArtist() {
        assertFalse(ArtistParser.isSameArtist("Tony Ann", "Tony An"))
        assertFalse(ArtistParser.isSameArtist("Woods", "Wood"))
        assertFalse(ArtistParser.isSameArtist("Them", "Them Terribles"))
    }
}
