package com.geckour.nowplaying4gpm.app.domain.model

import com.geckour.nowplaying4gpm.app.api.model.SpotifyToken
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyUserInfo(
    @SerialName("token")
    val token: SpotifyToken,

    @SerialName("user_name")
    val userName: String,

    @SerialName("refresh_token_expired_at")
    val refreshTokenExpiredAt: Long? = null
)