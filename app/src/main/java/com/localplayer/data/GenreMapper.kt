package com.localplayer.data

object GenreMapper {
    private val mapping: Map<String, String> = mapOf(
        // Rock - only explicit rock genres, not singer-songwriter
        "rock" to "Rock",
        "hard rock" to "Rock",
        "progressive rock" to "Rock",
        "grunge" to "Rock",
        "post rock" to "Rock",
        "punk rock" to "Rock",
        "garage rock" to "Rock",
        "psychedelic rock" to "Rock",
        "classic rock" to "Rock",
        "soft rock" to "Rock",
        
        // Alternative/Indie - separate from Rock for better categorization
        "alternative" to "Rock",
        "punk" to "Rock",
        "emo" to "Rock",
        "post punk" to "Rock",
        
        // Indie - can go to Rock but prioritize Folk for acoustic
        "indie rock" to "Rock",
        
        // Pop
        "pop" to "Pop",
        "dance pop" to "Pop",
        "synthpop" to "Pop",
        "k-pop" to "Pop",
        "j-pop" to "Pop",
        "electropop" to "Pop",
        "teen pop" to "Pop",
        "bubblegum pop" to "Pop",
        "adult contemporary" to "Pop",

        // Hip-Hop
        "hip hop" to "Hip-Hop",
        "rap" to "Hip-Hop",
        "trap" to "Hip-Hop",
        "hip-hop" to "Hip-Hop",
        "hiphop" to "Hip-Hop",
        "gangsta rap" to "Hip-Hop",
        "conscious hip hop" to "Hip-Hop",

        // Electronic
        "electronic" to "Electronic",
        "edm" to "Electronic",
        "house" to "Electronic",
        "techno" to "Electronic",
        "trance" to "Electronic",
        "dubstep" to "Electronic",
        "drum and bass" to "Electronic",
        "dnb" to "Electronic",
        "electronica" to "Electronic",
        "idm" to "Electronic",

        // R&B / Soul
        "r&b" to "R&B",
        "rnb" to "R&B",
        "soul" to "R&B",
        "funk" to "R&B",
        "neo soul" to "R&B",
        "motown" to "R&B",
        "rhythm and blues" to "R&B",

        // Jazz
        "jazz" to "Jazz",
        "smooth jazz" to "Jazz",
        "bebop" to "Jazz",
        "swing" to "Jazz",
        "big band" to "Jazz",

        // Blues
        "blues" to "Blues",
        "blues rock" to "Blues",
        "delta blues" to "Blues",

        // Classical
        "classical" to "Classical",
        "opera" to "Classical",
        "orchestral" to "Classical",
        "baroque" to "Classical",
        "chamber music" to "Classical",
        "symphony" to "Classical",
        "romantic" to "Classical",
        "piano" to "Classical",
        "instrumental" to "Classical",

        // Country
        "country" to "Country",
        "country rock" to "Country",
        "americana" to "Country",
        "bluegrass" to "Country",
        "outlaw country" to "Country",

        // Folk - IMPORTANT: singer-songwriter goes here
        "folk" to "Folk",
        "singer songwriter" to "Folk",
        "singer-songwriter" to "Folk",
        "acoustic" to "Folk",
        "indie folk" to "Folk",
        "folk rock" to "Folk",
        "traditional folk" to "Folk",
        "folk pop" to "Folk",

        // Latin
        "latin" to "Latin",
        "reggaeton" to "Latin",
        "salsa" to "Latin",
        "bachata" to "Latin",
        "bossa nova" to "Latin",
        "latin pop" to "Latin",
        "cumbia" to "Latin",
        "mariachi" to "Latin",
        "flamenco" to "Latin",

        // Reggae
        "reggae" to "Reggae",
        "ska" to "Reggae",
        "dub" to "Reggae",
        "dancehall" to "Reggae",

        // Metal
        "metal" to "Metal",
        "metalcore" to "Metal",
        "death metal" to "Metal",
        "black metal" to "Metal",
        "heavy metal" to "Metal",
        "thrash metal" to "Metal",
        "power metal" to "Metal",
        "nu metal" to "Metal",
        "doom metal" to "Metal",
        "progressive metal" to "Metal",

        // Ambient
        "ambient" to "Ambient",
        "chill" to "Ambient",
        "downtempo" to "Ambient",
        "lofi" to "Ambient",
        "lo-fi" to "Ambient",
        "chillout" to "Ambient",
        "new age" to "Ambient",
        "meditation" to "Ambient",
        "relaxation" to "Ambient",

        // Soundtrack
        "soundtrack" to "Soundtrack",
        "score" to "Soundtrack",
        "film score" to "Soundtrack",
        "video game music" to "Soundtrack",
        "anime" to "Soundtrack",
        "ost" to "Soundtrack",

        // Gospel
        "gospel" to "Gospel",
        "religious" to "Gospel",
        "christian" to "Gospel",
        "worship" to "Gospel",
        "ccm" to "Gospel",
        "praise" to "Gospel",

        // Children
        "children" to "Children",
        "kids" to "Children",
        "nursery rhymes" to "Children",
        "children's music" to "Children",

        // Spoken Word
        "spoken word" to "Spoken Word",
        "podcast" to "Spoken Word",
        "audiobook" to "Spoken Word",
        "comedy" to "Spoken Word",

        // World
        "world" to "World",
        "afro" to "World",
        "afrobeats" to "World",
        "asian" to "World",
        "celtic" to "World",
        "indian" to "World",
        "world music" to "World",
        "ethnic" to "World",
        "african" to "World",
        "middle eastern" to "World"
    )

    val topGenres = listOf(
        "Rock",
        "Pop",
        "Hip-Hop",
        "Electronic",
        "R&B",
        "Jazz",
        "Blues",
        "Classical",
        "Country",
        "Folk",
        "Latin",
        "Reggae",
        "Metal",
        "Ambient",
        "Soundtrack",
        "Gospel",
        "Children",
        "Spoken Word",
        "World",
        "Other"
    )

    fun mapToTopGenre(raw: String?): String {
        if (raw.isNullOrBlank()) return "Other"
        val cleaned = raw.lowercase()
            .replace("&", "and")
            .replace("-", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        val parts = cleaned.split(Regex("[,/;|]")).map { it.trim() }.filter { it.isNotBlank() }
        val candidates = if (parts.isEmpty()) listOf(cleaned) else parts
        candidates.forEach { candidate ->
            mapping[candidate]?.let { return it }
            mapping.entries.firstOrNull { candidate.contains(it.key) }?.value?.let { return it }
        }
        return "Other"
    }
}
