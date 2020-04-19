package com.geckour.nowplaying4gpm.api.model

import kotlinx.serialization.Serializable

@Serializable
data class MastodonInstance(
    val name: String?
)