package com.geckour.nowplaying4gpm.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SpotifySearchResult(
    val tracks: SpotifyTracks?
) {
    @JsonClass(generateAdapter = true)
    data class SpotifyTracks(
        @Json(name = "href")
        val queryUrl: String,

        val items: List<SpotifyTrack>
    ) {
        @JsonClass(generateAdapter = true)
        data class SpotifyTrack(
            val id: String,

            @Json(name = "external_urls")
            val urls: Map<String, String>
        )
    }
}