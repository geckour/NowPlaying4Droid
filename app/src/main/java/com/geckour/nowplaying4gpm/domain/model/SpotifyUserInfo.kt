package com.geckour.nowplaying4gpm.domain.model

import com.geckour.nowplaying4gpm.api.model.SpotifyToken
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SpotifyUserInfo(
    val token: SpotifyToken,
    val userName: String
)