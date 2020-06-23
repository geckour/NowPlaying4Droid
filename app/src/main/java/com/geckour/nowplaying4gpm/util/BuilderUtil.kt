package com.geckour.nowplaying4gpm.util

import android.provider.MediaStore
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

val json: Json = Json(JsonConfiguration.Stable.copy(ignoreUnknownKeys = true, isLenient = true))

const val contentQuerySelection: String =
    "${MediaStore.Audio.Media.TITLE}=? and ${MediaStore.Audio.Media.ARTIST}=? and ${MediaStore.Audio.Media.ALBUM}=?"

val TrackInfo.TrackCoreElement.contentQueryArgs: Array<String>
    get() = arrayOf(requireNotNull(title), requireNotNull(artist), requireNotNull(album))