package com.geckour.nowplaying4gpm.api.model

import com.google.gson.annotations.SerializedName

data class SpotifySearchResult(
        val tracks: SpotifyTracks?
) {
    data class SpotifyTracks(
            @SerializedName("href")
            val queryUrl: String,

            val items: List<SpotifyTrack>
    ) {
        data class SpotifyTrack(
                val id: String,

                @SerializedName("external_urls")
                val urls: HashMap<String, String>
        )
    }
}