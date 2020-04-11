package com.geckour.nowplaying4gpm.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SpotifyToken(
    @Json(name = "access_token")
    val accessToken: String,

    @Json(name = "token_type")
    val tokenType: String,

    @Json(name = "expires_in")
    val expiresIn: Long,

    @Json(name = "refresh_token")
    val codeForRefreshToken: String
)