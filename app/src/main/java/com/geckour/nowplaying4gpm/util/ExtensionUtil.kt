package com.geckour.nowplaying4gpm.util

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.view.View
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.activity.SettingsActivity
import com.geckour.nowplaying4gpm.domain.model.TrackCoreElement
import kotlinx.coroutines.experimental.Job
import timber.log.Timber
import kotlin.math.absoluteValue

fun String.getSharingText(trackCoreElement: TrackCoreElement): String =
        this.splitIncludeDelimiter("''", "'", "TI", "AR", "AL", "\\\\n")
                .let { splitList ->
                    val escapes = splitList.mapIndexed { i, s -> Pair(i, s) }
                            .filter { it.second == "'" }
                            .apply { if (lastIndex < 0) return@let splitList }

                    return@let ArrayList<String>().apply {
                        for (i in 0 until escapes.lastIndex step 2) {
                            this.addAll(
                                    splitList.subList(
                                            if (i == 0) 0 else escapes[i - 1].first + 1,
                                            escapes[i].first))

                            this.add(
                                    splitList.subList(
                                            escapes[i].first,
                                            escapes[i + 1].first + 1
                                    ).joinToString(""))
                        }

                        this.addAll(
                                splitList.subList(
                                        if (escapes[escapes.lastIndex].first + 1 < splitList.lastIndex)
                                            escapes[escapes.lastIndex].first + 1
                                        else splitList.lastIndex,
                                        splitList.size
                                ))
                    }
                }.joinToString("") {
                    return@joinToString Regex("^'(.+)'$").let { regex ->
                        if (it.matches(regex)) it.replace(regex, "$1")
                        else when (it) {
                            "'" -> ""
                            "''" -> "'"
                            "TI" -> trackCoreElement.title ?: ""
                            "AR" -> trackCoreElement.artist ?: ""
                            "AL" -> trackCoreElement.album ?: ""
                            "\\n" -> "\n"
                            else -> it
                        }
                    }
                }

fun String.splitIncludeDelimiter(vararg delimiters: String) =
        delimiters.joinToString("|")
                .let { pattern ->
                    this.split(Regex("(?<=$pattern)|(?=$pattern)"))
                }

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

fun Context.checkStoragePermission(onNotGranted: ((context: Context) -> Unit)? = null, onGranted: (context: Context) -> Unit = {}) {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        onGranted(this)
    else {
        onNotGranted?.invoke(this)
                ?: this@checkStoragePermission.startActivity(SettingsActivity.getIntent(this).apply { flags = flags or Intent.FLAG_ACTIVITY_NEW_TASK })
    }
}

fun Bitmap.similarity(bitmap: Bitmap): Float {
    if (this.width != bitmap.width || this.height != bitmap.height) return 0f

    var count = 0
    for (x in 0 until this.width) {
        for (y in 0 until this.height) {
            if (this.getPixel(x, y).colorSimilarity(bitmap.getPixel(x, y)) > 0.9) count++
        }
    }

    return (count.toFloat() / (this.width * this.height)).apply { Timber.d("similarity: $this") }
}

fun Int.colorSimilarity(colorInt: Int): Float {
    val src = Color.valueOf(this)
    val dst = Color.valueOf(colorInt)
    return ((255 - (src.red() - dst.red()).absoluteValue) / 255 + (255 - (src.green() - dst.green()).absoluteValue) / 255 + (255 - (src.blue() - dst.blue()).absoluteValue) / 255) / 3
}