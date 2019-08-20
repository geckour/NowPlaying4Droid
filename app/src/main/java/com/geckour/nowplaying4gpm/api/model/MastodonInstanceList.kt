package com.geckour.nowplaying4gpm.api.model

import com.squareup.moshi.Json

data class MastodonInstanceList(
    @Json(name = "instances")
    val value: List<MastodonInstance>?
)