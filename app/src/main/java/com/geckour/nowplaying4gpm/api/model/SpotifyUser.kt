package com.geckour.nowplaying4gpm.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyUser(
    @SerialName("display_name")
    val displayName: String,
    @SerialName("id")
    val id: String
)