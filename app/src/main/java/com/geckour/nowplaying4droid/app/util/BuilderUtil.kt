package com.geckour.nowplaying4droid.app.util

import android.provider.MediaStore
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
val json: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

const val contentQuerySelection: String =
    "${
        MediaStore.Audio.Media.TITLE
    }=? and ${
        MediaStore.Audio.Media.ARTIST
    }=? and ${
        MediaStore.Audio.Media.ALBUM
    }=?"