package com.geckour.nowplaying4gpm.app.domain.model

import kotlinx.serialization.Serializable

sealed class SpotifyResult {
    class Success(val data: Data) : SpotifyResult()
    class Failure(val cause: Throwable) : SpotifyResult()

    @Serializable
    data class Data(
        val sharingUrl: String,
        val artworkUrl: String?,
        val trackName: String,
        val artistName: String,
        val albumName: String,
    ) : java.io.Serializable
}