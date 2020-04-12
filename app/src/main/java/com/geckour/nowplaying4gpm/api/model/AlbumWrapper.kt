package com.geckour.nowplaying4gpm.api.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AlbumWrapper(
    val album: Album
)