package com.geckour.nowplaying4gpm.api.model

import com.google.gson.annotations.SerializedName

data class Results(
        @SerializedName("results")
        val resultItems: List<Album>
)