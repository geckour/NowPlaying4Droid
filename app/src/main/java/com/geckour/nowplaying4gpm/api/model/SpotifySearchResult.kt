package com.geckour.nowplaying4gpm.api.model

import com.squareup.moshi.Json

data class SpotifySearchResult(
    val tracks: SpotifyTracks?
) {
    data class SpotifyTracks(
        @Json(name = "href")
        val queryUrl: String,

        val items: List<SpotifyTrack>
    ) {
        data class SpotifyTrack(
            val id: String,

            @Json(name = "external_urls")
            val urls: Map<String, String>
        )
    }
}