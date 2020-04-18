package com.geckour.nowplaying4gpm.domain.model

import com.geckour.nowplaying4gpm.api.model.SpotifyToken
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SpotifyUserInfo(
    @Json(name = "token")
    val token: SpotifyToken,

    @Json(name = "user_name")
    val userName: String
)