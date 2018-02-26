package com.geckour.nowplaying4gpm.api.model

import com.google.gson.annotations.SerializedName

data class Album(
        @SerializedName("collectionName")
        val name: String,

        @SerializedName("artworkUrl100")
        val artworkUrl: String
)