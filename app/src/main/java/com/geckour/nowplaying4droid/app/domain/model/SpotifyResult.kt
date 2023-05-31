package com.geckour.nowplaying4droid.app.domain.model

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
        val releasedAt: String,
    ) : java.io.Serializable
}