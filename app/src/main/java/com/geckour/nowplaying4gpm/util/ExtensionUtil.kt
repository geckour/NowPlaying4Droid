package com.geckour.nowplaying4gpm.util

import android.app.AlertDialog
import android.content.DialogInterface
import android.view.View
import com.geckour.nowplaying4gpm.R
import java.text.MessageFormat

fun String.getSharingText(title: String, artist: String, album: String): String {
    val pattern = this
            .replace("{", "'{'")
            .replace("}", "'}'")
            .replace("TI", "{0}")
            .replace("AR", "{1}")
            .replace("AL", "{2}")
    return MessageFormat.format(pattern, title, artist, album)
}

fun AlertDialog.Builder.generate(
        title: String,
        message: String,
        view: View,
        callback: (dialog: DialogInterface, which: Int) -> Unit = { _, _ -> }): AlertDialog {
    setTitle(title)
    setMessage(message)
    setView(view)
    setPositiveButton(R.string.dialog_button_ok) { dialog, which -> callback(dialog, which) }

    return create()
}