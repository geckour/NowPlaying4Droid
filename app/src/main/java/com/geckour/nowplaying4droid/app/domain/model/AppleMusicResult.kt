package com.geckour.nowplaying4droid.app.domain.model

import kotlinx.serialization.Serializable

sealed class AppleMusicResult {

    class Success(val data: Data) : AppleMusicResult()
    class Failure(val cause: Throwable) : AppleMusicResult()

    @Serializable
    data class Data(
        val sharingUrl: String,
        val artworkUrl: String?,
        val trackName: String,
        val artistName: String,
        val albumName: String,
        val composerName: String?,
        val releasedAt: String,
    ) : java.io.Serializable
}