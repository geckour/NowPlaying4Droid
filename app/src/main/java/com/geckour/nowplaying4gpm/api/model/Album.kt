package com.geckour.nowplaying4gpm.api.model

import com.squareup.moshi.Json

data class Album(

    @Json(name = "image")
    val artworks: List<Image>
)