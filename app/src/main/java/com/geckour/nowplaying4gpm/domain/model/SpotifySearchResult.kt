package com.geckour.nowplaying4gpm.domain.model

sealed class SpotifySearchResult(val query: String?) {
    class Success(query: String, val data: Data) : SpotifySearchResult(query)
    class Failure(query: String?, val cause: Throwable) : SpotifySearchResult(query)

    data class Data(
        val sharingUrl: String,
        val artworkUrl: String?
    )
}