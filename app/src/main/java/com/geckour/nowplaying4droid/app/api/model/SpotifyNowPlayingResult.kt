package com.geckour.nowplaying4droid.app.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyNowPlayingResult(
    val item: SpotifyTrack
) {

    @Serializable
    data class SpotifyTrack(
        val id: String,

        @SerialName("external_urls")
        val urls: Map<String, String>,

        val name: String,

        val album: SpotifyAlbum,

        val artists: List<SpotifyArtist>,
    ) {

        val artistString = artists.joinToString { it.name }

        @Serializable
        data class SpotifyAlbum(
            val name: String,
            @SerialName("release_date")
            val releaseDate: String,
            val images: List<SpotifyImage>
        ) {

            @Serializable
            data class SpotifyImage(
                val url: String,
                val height: Int,
                val width: Int
            )
        }

        @Serializable
        data class SpotifyArtist(
            val name: String,
        )
    }
}