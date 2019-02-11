package com.geckour.nowplaying4gpm.domain.model

import com.sys1yagi.mastodon4j.api.entity.auth.AccessToken

data class MastodonUserInfo(
    val accessToken: AccessToken,
    val instanceName: String,
    val userName: String
)