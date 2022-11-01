package com.geckour.nowplaying4droid.app.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Album(

    @SerialName("image")
    val artworks: List<Image>
)