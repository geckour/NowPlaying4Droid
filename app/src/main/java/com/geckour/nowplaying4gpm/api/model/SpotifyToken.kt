package com.geckour.nowplaying4gpm.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyToken(
    @SerialName("access_token")
    val accessToken: String,

    @SerialName("token_type")
    val tokenType: String,

    @SerialName("expires_in")
    val expiresIn: Long,

    @SerialName("refresh_token")
    val refreshToken: String? = null,

    @SerialName("scope")
    val scope: String = ""
) {

    fun getExpiredAt(currentTime: Long = System.currentTimeMillis()): Long? =
        refreshToken?.let { currentTime + expiresIn * 1000 }
}