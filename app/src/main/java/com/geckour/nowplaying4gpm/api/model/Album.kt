package com.geckour.nowplaying4gpm.api.model

import com.squareup.moshi.Json

data class Album(
    @Json(name = "mbid")
    val id: String,

    @Json(name = "name")
    val title: String,

    val artist: String,

    @Json(name = "image")
    val artworks: List<Image>
)