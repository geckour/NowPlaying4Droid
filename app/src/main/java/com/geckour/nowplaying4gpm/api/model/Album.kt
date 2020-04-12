package com.geckour.nowplaying4gpm.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Album(

    @Json(name = "image")
    val artworks: List<Image>
)