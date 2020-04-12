package com.geckour.nowplaying4gpm.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Image(
    @Json(name = "#text")
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