package com.geckour.nowplaying4gpm.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifySearchResult(
    val tracks: SpotifyTracks?
) {

    @Serializable
    data class SpotifyTracks(
        @SerialName("href")
        val queryUrl: String,

        val items: List<SpotifyTrack>
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
        }
    }

    @Serializable
    data class SpotifyAlbum(
        val name: String,
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