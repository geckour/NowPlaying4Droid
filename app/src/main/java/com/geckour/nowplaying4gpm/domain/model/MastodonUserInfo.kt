package com.geckour.nowplaying4gpm.domain.model

import com.squareup.moshi.JsonClass
import com.sys1yagi.mastodon4j.api.entity.auth.AccessToken

@JsonClass(generateAdapter = true)
data class MastodonUserInfo(
    val accessToken: AccessToken,
    val instanceName: String,
    val userName: String
)