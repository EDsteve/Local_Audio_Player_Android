package com.localplayer.data

/**
 * Two-level genre tree: every specific genre ("Indie Rock", "Trap", "Bebop")
 * belongs to one top genre ("Rock", "Hip-Hop", "Jazz").
 *
 * Tracks keep their specific genre; the tree is applied when the library is
 * grouped, so playing a top genre plays all of its sub-genres.
 */
object GenreMapper {
    const val OTHER = "Other"

    /** Top genre -> the specific genres filed under it (normalized spelling). */
    private val tree: Map<String, List<String>> = linkedMapOf(
        "Rock" to listOf(
            "rock", "hard rock", "progressive rock", "prog", "grunge", "post grunge",
            "post rock", "punk rock", "garage rock", "psychedelic rock", "psychedelic",
            "classic rock", "soft rock", "alternative", "alternative rock", "indie",
            "indie rock", "punk", "pop punk", "emo", "post punk", "hardcore",
            "hardcore punk", "post hardcore", "shoegaze", "britpop", "new wave",
            "glam rock", "rock and roll", "rockabilly", "surf rock", "surf",
            "southern rock", "stoner rock", "art rock", "krautrock", "math rock",
            "noise rock", "blues rock", "folk rock", "country rock", "pop rock",
            "rock opera"
        ),
        "Pop" to listOf(
            "pop", "dance pop", "synthpop", "k pop", "j pop", "electropop", "teen pop",
            "bubblegum pop", "adult contemporary", "indie pop", "dream pop", "power pop",
            "new romantic", "europop", "schlager", "chanson", "easy listening", "disco",
            "oldies"
        ),
        "Hip-Hop" to listOf(
            "hip hop", "rap", "trap", "gangsta rap", "conscious hip hop",
            "underground hip hop", "grime", "drill", "boom bap", "crunk"
        ),
        "Electronic" to listOf(
            "electronic", "edm", "dance", "house", "deep house", "techno", "trance",
            "psytrance", "dubstep", "drum and bass", "jungle", "electronica", "idm",
            "electro", "breakbeat", "big beat", "uk garage", "hardstyle",
            "happy hardcore", "synthwave", "vaporwave", "trip hop", "industrial",
            "eurodance", "future bass", "minimal", "chiptune"
        ),
        "R&B" to listOf(
            "r and b", "contemporary r and b", "soul", "funk", "neo soul", "motown"
        ),
        "Jazz" to listOf(
            "jazz", "smooth jazz", "bebop", "swing", "big band", "jazz fusion",
            "cool jazz", "free jazz", "acid jazz", "vocal jazz", "dixieland"
        ),
        "Blues" to listOf(
            "blues", "delta blues", "chicago blues", "electric blues"
        ),
        "Classical" to listOf(
            "classical", "opera", "orchestral", "baroque", "chamber music", "symphony",
            "piano", "instrumental", "contemporary classical", "modern classical",
            "neoclassical", "choral", "minimalism"
        ),
        "Country" to listOf(
            "country", "americana", "bluegrass", "outlaw country", "alt country",
            "honky tonk", "western"
        ),
        "Folk" to listOf(
            "folk", "singer songwriter", "acoustic", "indie folk", "traditional folk",
            "folk pop", "neofolk", "freak folk"
        ),
        "Latin" to listOf(
            "latin", "reggaeton", "salsa", "bachata", "bossa nova", "latin pop",
            "cumbia", "mariachi", "flamenco", "tango", "samba", "merengue", "mpb",
            "tropical"
        ),
        "Reggae" to listOf(
            "reggae", "ska", "dub", "dancehall", "roots reggae", "rocksteady"
        ),
        "Metal" to listOf(
            "metal", "metalcore", "death metal", "black metal", "heavy metal",
            "thrash metal", "power metal", "nu metal", "doom metal",
            "progressive metal", "deathcore", "grindcore", "hair metal", "glam metal",
            "sludge"
        ),
        "Ambient" to listOf(
            "ambient", "chill", "downtempo", "lo fi", "chillout", "new age",
            "meditation", "relaxation", "drone"
        ),
        "Soundtrack" to listOf(
            "soundtrack", "score", "film score", "video game music", "anime", "musical"
        ),
        "Gospel" to listOf(
            "gospel", "religious", "christian", "worship", "ccm", "praise"
        ),
        "Children" to listOf(
            "children", "nursery rhymes"
        ),
        "Spoken Word" to listOf(
            "spoken word", "podcast", "audiobook", "comedy", "speech"
        ),
        "World" to listOf(
            "world", "afro", "afrobeat", "afrobeats", "asian", "celtic", "indian",
            "ethnic", "african", "middle eastern", "arabic", "balkan", "klezmer",
            "bollywood"
        )
    )

    /** Specific genre (normalized) -> top genre. */
    private val parents: Map<String, String> =
        tree.flatMap { (top, subs) -> subs.map { it to top } }.toMap()

    /** Different spellings of the same genre (normalized) -> the one we keep. */
    private val aliases: Map<String, String> = mapOf(
        "hiphop" to "hip hop",
        "rnb" to "r and b",
        "rhythm and blues" to "r and b",
        "dnb" to "drum and bass",
        "drum n bass" to "drum and bass",
        "lofi" to "lo fi",
        "kpop" to "k pop",
        "jpop" to "j pop",
        "synth pop" to "synthpop",
        "electro pop" to "electropop",
        "alt rock" to "alternative rock",
        "prog rock" to "progressive rock",
        "prog metal" to "progressive metal",
        "rock n roll" to "rock and roll",
        "kids" to "children",
        "childrens music" to "children",
        "children's music" to "children",
        "world music" to "world",
        "ost" to "soundtrack",
        "soundtracks" to "soundtrack",
        "musicals" to "musical"
    )

    /** Names that plain Title Case would get wrong (normalized -> shown). */
    private val displayNames: Map<String, String> = mapOf(
        "hip hop" to "Hip-Hop",
        "r and b" to "R&B",
        "contemporary r and b" to "Contemporary R&B",
        "lo fi" to "Lo-Fi",
        "k pop" to "K-Pop",
        "j pop" to "J-Pop",
        "edm" to "EDM",
        "idm" to "IDM",
        "ccm" to "CCM",
        "mpb" to "MPB",
        "uk garage" to "UK Garage",
        "singer songwriter" to "Singer-Songwriter",
        "drum and bass" to "Drum & Bass",
        "rock and roll" to "Rock & Roll",
        "post rock" to "Post-Rock",
        "post punk" to "Post-Punk",
        "post hardcore" to "Post-Hardcore",
        "post grunge" to "Post-Grunge",
        "trip hop" to "Trip-Hop",
        "alt country" to "Alt-Country"
    )

    private val lowercaseWords = setOf("and", "of", "the")

    val topGenres: List<String> = tree.keys.toList() + OTHER

    /** A genre the user can pick by hand. */
    data class Choice(val name: String, val topGenre: String) {
        val isTopGenre: Boolean get() = name == topGenre
    }

    /** Every genre in the tree: the top genres first, then their sub-genres. */
    val choices: List<Choice> by lazy {
        val tops = tree.keys.map { Choice(it, it) }
        val subs = tree.flatMap { (top, subgenres) ->
            subgenres.map { Choice(displayName(it), top) }
        }.filter { !it.isTopGenre }.distinct().sortedBy { it.name.lowercase() }
        tops + subs
    }

    /** Lowercase, punctuation-free spelling used to compare genre names. */
    private fun normalize(raw: String): String {
        val cleaned = raw.lowercase()
            .replace("'n'", " and ")
            .replace("&", " and ")
            .replace(Regex("[-_]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return aliases[cleaned] ?: cleaned
    }

    private fun findParent(key: String): String? {
        parents[key]?.let { return it }
        // No exact entry: look for known genres inside the name as whole words
        // ("progressive death metal" contains "death metal"). The last one wins,
        // because the last word of a genre name is usually the genre itself
        // ("jazz rap" is rap, "folk metal" is metal).
        val padded = " $key "
        var best: String? = null
        var bestEnd = -1
        var bestLength = -1
        for (known in parents.keys) {
            val start = padded.lastIndexOf(" $known ")
            if (start < 0) continue
            val end = start + known.length
            if (end > bestEnd || (end == bestEnd && known.length > bestLength)) {
                best = known
                bestEnd = end
                bestLength = known.length
            }
        }
        return best?.let { parents[it] }
    }

    /** True if the genre can be placed under one of the top genres. */
    fun isKnown(genre: String?): Boolean =
        !genre.isNullOrBlank() && findParent(normalize(genre)) != null

    /** Top genre a genre belongs to; [OTHER] if it is not in the tree. */
    fun parentOf(genre: String?): String {
        if (genre.isNullOrBlank()) return OTHER
        return findParent(normalize(genre)) ?: OTHER
    }

    /** One consistent spelling for a genre: "indie rock" / "Indie-Rock" -> "Indie Rock". */
    fun displayName(genre: String?): String {
        if (genre.isNullOrBlank()) return OTHER
        val key = normalize(genre)
        if (key.isEmpty()) return OTHER
        displayNames[key]?.let { return it }
        return key.split(" ").mapIndexed { index, word ->
            if (index > 0 && word in lowercaseWords) word
            else word.replaceFirstChar { it.uppercase() }
        }.joinToString(" ")
    }

    /**
     * Genre to store for a raw file tag, which may list several ("Indie Rock; Pop").
     * Returns the first one found in the tree, or [OTHER] if none is.
     */
    fun specificGenre(raw: String?): String {
        if (raw.isNullOrBlank()) return OTHER
        val match = raw.split(Regex("[,/;|]"))
            .map { it.trim() }
            .firstOrNull { isKnown(it) }
        return match?.let { displayName(it) } ?: OTHER
    }
}
