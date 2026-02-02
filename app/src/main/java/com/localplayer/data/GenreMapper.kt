package com.localplayer.data

object GenreMapper {
    private val mapping: Map<String, String> = mapOf(
        "rock" to "Rock",
        "alternative" to "Rock",
        "metal" to "Rock",
        "punk" to "Rock",
        "indie" to "Rock",
        "hard rock" to "Rock",
        "progressive" to "Rock",
        "grunge" to "Rock",

        "pop" to "Pop",
        "dance pop" to "Pop",
        "synthpop" to "Pop",
        "k-pop" to "Pop",
        "j-pop" to "Pop",

        "hip hop" to "Hip-Hop",
        "rap" to "Hip-Hop",
        "trap" to "Hip-Hop",

        "electronic" to "Electronic",
        "edm" to "Electronic",
        "house" to "Electronic",
        "techno" to "Electronic",
        "trance" to "Electronic",
        "dubstep" to "Electronic",

        "r&b" to "R&B",
        "soul" to "R&B",
        "funk" to "R&B",

        "jazz" to "Jazz",
        "blues" to "Blues",

        "classical" to "Classical",
        "opera" to "Classical",
        "orchestral" to "Classical",

        "country" to "Country",
        "folk" to "Folk",

        "latin" to "Latin",
        "reggaeton" to "Latin",
        "salsa" to "Latin",
        "bachata" to "Latin",
        "bossa nova" to "Latin",

        "reggae" to "Reggae",
        "ska" to "Reggae",

        "metalcore" to "Metal",
        "death metal" to "Metal",
        "black metal" to "Metal",

        "ambient" to "Ambient",
        "chill" to "Ambient",
        "downtempo" to "Ambient",
        "lofi" to "Ambient",
        "lo-fi" to "Ambient",

        "soundtrack" to "Soundtrack",
        "score" to "Soundtrack",

        "gospel" to "Gospel",
        "religious" to "Gospel",

        "children" to "Children",
        "kids" to "Children",

        "spoken word" to "Spoken Word",
        "podcast" to "Spoken Word",

        "world" to "World",
        "afro" to "World",
        "afrobeats" to "World",
        "asian" to "World",
        "celtic" to "World",
        "indian" to "World"
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
