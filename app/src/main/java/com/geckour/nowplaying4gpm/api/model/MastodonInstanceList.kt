package com.geckour.nowplaying4gpm.api.model

import com.google.gson.annotations.SerializedName

data class MastodonInstanceList(
    @SerializedName("instances") val value: List<MastodonInstance>
)