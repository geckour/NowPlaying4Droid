package com.geckour.nowplaying4droid.app.util

import android.provider.MediaStore
import com.geckour.nowplaying4droid.app.domain.model.TrackDetail
import kotlinx.serialization.json.Json

val json: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

const val contentQuerySelection: String =
    "${MediaStore.Audio.Media.TITLE}=? and ${MediaStore.Audio.Media.ARTIST}=? and ${MediaStore.Audio.Media.ALBUM}=?"

val TrackDetail.TrackCoreElement.contentQueryArgs: Array<String>
    get() = arrayOf(checkNotNull(title), checkNotNull(artist), checkNotNull(album))