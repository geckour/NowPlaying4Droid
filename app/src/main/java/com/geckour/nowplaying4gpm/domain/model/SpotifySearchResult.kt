package com.geckour.nowplaying4gpm.domain.model

import kotlinx.serialization.Serializable

sealed class SpotifySearchResult(val query: String?) {
    class Success(query: String, val data: Data) : SpotifySearchResult(query)
    class Failure(query: String?, val cause: Throwable) : SpotifySearchResult(query)

    @Serializable
    data class Data(
        val sharingUrl: String,
        val artworkUrl: String?,
        val trackName: String,
        val artistName: String,
        val albumName: String,
    ) : java.io.Serializable
}