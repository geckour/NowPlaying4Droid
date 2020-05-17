package com.geckour.nowplaying4gpm.domain.model

import com.geckour.nowplaying4gpm.util.MastodonAccessTokenSerializer
import com.sys1yagi.mastodon4j.api.entity.auth.AccessToken
import kotlinx.serialization.Serializable

@Serializable
data class MastodonUserInfo(
    @Serializable(with = MastodonAccessTokenSerializer::class)
    val accessToken: AccessToken,
    val instanceName: String,
    val userName: String
)