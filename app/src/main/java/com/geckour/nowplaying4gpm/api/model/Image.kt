package com.geckour.nowplaying4gpm.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Image(
    @SerialName("#text")
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