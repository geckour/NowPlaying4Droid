package com.geckour.nowplaying4gpm.app.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MastodonInstanceList(
    @SerialName("instances")
    val value: List<MastodonInstance>?
)