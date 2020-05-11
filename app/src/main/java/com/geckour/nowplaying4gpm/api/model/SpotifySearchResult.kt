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

            val album: SpotifyAlbum
        )
    }

    @Serializable
    data class SpotifyAlbum(
        val images: List<SpotifyImage>
    ) {

        @Serializable
        data class SpotifyImage(
            val url: String,
            val height: Int,
            val width: Int
        )
    }
}