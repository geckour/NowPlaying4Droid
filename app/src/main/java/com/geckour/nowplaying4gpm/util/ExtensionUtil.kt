package com.geckour.nowplaying4gpm.util

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.view.View
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.activity.SettingsActivity
import kotlinx.coroutines.experimental.Job
import timber.log.Timber

fun String.getSharingText(title: String, artist: String, album: String): String =
        this.splitIncludeDelimiter("''", "'", "TI", "AR", "AL", "\\\\n")
                .let { splitList ->
                    val escapes = splitList.mapIndexed { i, s -> Pair(i, s) }.filter { it.second == "'" }.apply { if (lastIndex < 0) return@let splitList }
                    return@let ArrayList<String>().apply {
                        for (i in 0 until escapes.lastIndex step 2) {
                            this.addAll(splitList.subList(
                                    if (i == 0) 0 else escapes[i - 1].first + 1,
                                    escapes[i].first))

                            this.add(splitList.subList(
                                    escapes[i].first,
                                    escapes[i + 1].first + 1).joinToString(""))
                        }

                        this.addAll(splitList.subList(
                                if (escapes[escapes.lastIndex].first + 1 < splitList.lastIndex) escapes[escapes.lastIndex].first + 1 else splitList.lastIndex,
                                splitList.size))
                    }
                }.joinToString("") {
                    return@joinToString Regex("^'(.+)'$").let { regex ->
                        if (it.matches(regex)) it.replace(regex, "$1")
                        else when (it) {
                            "'" -> ""
                            "''" -> "'"
                            "TI" -> title
                            "AR" -> artist
                            "AL" -> album
                            "\\n" -> "\n"
                            else -> it
                        }
                    }
                }

fun String.splitIncludeDelimiter(vararg delimiters: String) =
        delimiters.joinToString("|").let { pattern -> this.split(Regex("(?<=$pattern)|(?=$pattern)")) }

fun String.escapeSql(): String = replace("'", "''")

fun AlertDialog.Builder.generate(
        title: String,
        message: String,
        view: View,
        callback: (dialog: DialogInterface, which: Int) -> Unit = { _, _ -> }): AlertDialog {
    setTitle(title)
    setMessage(message)
    setView(view)
    setPositiveButton(R.string.dialog_button_ok) { dialog, which -> callback(dialog, which) }
    setNegativeButton(R.string.dialog_button_ng) { dialog, _ -> dialog.dismiss() }

    return create()
}

fun List<Job>.cancelAll() = forEach { it.cancel() }

fun Context.checkStoragePermission(onAlreadyGrant: ((context: Context) -> Unit)? = null, onGrant: (context: Context) -> Unit = {}) {
    if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        onAlreadyGrant?.invoke(this)
                ?: SettingsActivity.getIntent(this).apply { this@checkStoragePermission.startActivity(this) }
    } else onGrant(this)
}