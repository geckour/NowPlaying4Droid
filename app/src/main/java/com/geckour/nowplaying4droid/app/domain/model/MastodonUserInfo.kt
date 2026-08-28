package com.geckour.nowplaying4droid.app.domain.model

import kotlinx.serialization.Serializable
import social.bigbone.api.entity.Token

@Serializable
data class MastodonUserInfo(
    val accessToken: Token,
    val instanceName: String,
    val username: String
)