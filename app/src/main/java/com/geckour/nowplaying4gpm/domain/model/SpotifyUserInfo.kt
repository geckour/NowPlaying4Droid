package com.geckour.nowplaying4gpm.domain.model

import com.geckour.nowplaying4gpm.api.model.SpotifyToken

data class SpotifyUserInfo(
    val token: SpotifyToken,
    val userName: String
)