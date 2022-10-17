package com.geckour.nowplaying4gpm.app.api.model

import kotlinx.serialization.Serializable

@Serializable
data class MastodonInstance(
    val name: String?
)