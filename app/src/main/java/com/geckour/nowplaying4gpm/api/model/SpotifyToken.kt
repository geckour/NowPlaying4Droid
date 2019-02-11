package com.geckour.nowplaying4gpm.api.model

import com.google.gson.annotations.SerializedName

data class SpotifyToken(
    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("token_type")
    val tokenType: String,

    @SerializedName("expires_in")
    val expiresIn: Int
)