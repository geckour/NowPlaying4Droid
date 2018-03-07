package com.geckour.nowplaying4gpm.api.model

import com.google.gson.annotations.SerializedName

data class Album(
        @SerializedName("mbid")
        val id: String,

        @SerializedName("name")
        val title: String,

        val artist: String,

        @SerializedName("image")
        val artworks: List<Image>
)