package com.geckour.nowplaying4gpm.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MastodonInstanceList(
    @Json(name = "instances")
    val value: List<MastodonInstance>?
)