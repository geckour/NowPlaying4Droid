package com.geckour.nowplaying4gpm.api.model

import com.google.gson.annotations.SerializedName

data class Image(
        @SerializedName("#text")
        val url: String,

        val size: String
) {
    enum class Size(val rawStr: String) {
        SMALL("small"),
        MEDIUM("medium"),
        LARGE("large"),
        EX_LARGE("extralarge"),
        MEGA("mega")
    }
}