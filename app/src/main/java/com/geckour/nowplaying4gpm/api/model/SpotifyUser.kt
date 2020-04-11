package com.geckour.nowplaying4gpm.api.model

import com.squareup.moshi.Json

data class SpotifyUser(
    @Json(name = "display_name")
    val displayName: String,
    @Json(name = "id")
    val id: String
)