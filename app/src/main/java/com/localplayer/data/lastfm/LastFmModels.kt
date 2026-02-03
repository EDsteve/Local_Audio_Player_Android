package com.localplayer.data.lastfm

import com.google.gson.annotations.SerializedName

/**
 * Data classes for Last.fm API responses
 */

data class ArtistInfoResponse(
    @SerializedName("artist")
    val artist: ArtistInfo?
)

data class ArtistInfo(
    @SerializedName("name")
    val name: String?,
    @SerializedName("tags")
    val tags: TagContainer?
)

data class TagContainer(
    @SerializedName("tag")
    val tag: List<Tag>?
)

data class Tag(
    @SerializedName("name")
    val name: String?,
    @SerializedName("url")
    val url: String?
)

/**
 * Error response from Last.fm API
 */
data class LastFmError(
    @SerializedName("error")
    val error: Int?,
    @SerializedName("message")
    val message: String?
)
