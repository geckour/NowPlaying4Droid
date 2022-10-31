package com.geckour.nowplaying4droid.app.api.model

import kotlinx.serialization.Serializable

@Serializable
data class AlbumWrapper(
    val album: Album
)