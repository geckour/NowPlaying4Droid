package com.geckour.nowplaying4gpm.util

import android.provider.MediaStore

fun getContentQuerySelection(title: String?, artist: String?, album: String?): String =
        "${MediaStore.Audio.Media.TITLE}='${title?.escapeSql()}' and ${MediaStore.Audio.Media.ARTIST}='${artist?.escapeSql()}' and ${MediaStore.Audio.Media.ALBUM}='${album?.escapeSql()}'"