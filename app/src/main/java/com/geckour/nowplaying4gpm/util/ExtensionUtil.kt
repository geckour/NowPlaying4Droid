package com.geckour.nowplaying4gpm.util

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.preference.PreferenceManager
import android.support.annotation.ColorInt
import android.support.v4.content.ContextCompat
import android.support.v7.graphics.Palette
import android.view.View
import com.crashlytics.android.Crashlytics
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.domain.model.TrackCoreElement
import com.geckour.nowplaying4gpm.ui.SettingsActivity
import com.google.gson.Gson
import io.fabric.sdk.android.Fabric
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import timber.log.Timber
import java.lang.reflect.Type
import kotlin.math.absoluteValue

enum class PaletteColor {
    LIGHT_VIBRANT {
        override val hierarchyList: List<PaletteColor>
            get() = listOf(
                    LIGHT_VIBRANT,
                    VIBRANT,
                    DARK_VIBRANT,
                    LIGHT_MUTED,
                    MUTED,
                    DARK_MUTED
            )
    },
    VIBRANT {
        override val hierarchyList: List<PaletteColor>
            get() = listOf(
                    VIBRANT,
                    LIGHT_VIBRANT,
                    DARK_VIBRANT,
                    MUTED,
                    LIGHT_MUTED,
                    DARK_MUTED
            )
    },
    DARK_VIBRANT {
        override val hierarchyList: List<PaletteColor>
            get() = listOf(
                    DARK_VIBRANT,
                    VIBRANT,
                    LIGHT_VIBRANT,
                    DARK_MUTED,
                    MUTED,
                    LIGHT_MUTED
            )
    },
    LIGHT_MUTED {
        override val hierarchyList: List<PaletteColor>
            get() = listOf(
                    LIGHT_MUTED,
                    MUTED,
                    DARK_MUTED,
                    LIGHT_VIBRANT,
                    VIBRANT,
                    DARK_VIBRANT
            )
    },
    MUTED {
        override val hierarchyList: List<PaletteColor>
            get() = listOf(
                    MUTED,
                    LIGHT_MUTED,
                    DARK_MUTED,
                    VIBRANT,
                    LIGHT_VIBRANT,
                    DARK_VIBRANT
            )
    },
    DARK_MUTED {
        override val hierarchyList: List<PaletteColor>
            get() = listOf(
                    DARK_MUTED,
                    MUTED,
                    LIGHT_MUTED,
                    DARK_VIBRANT,
                    VIBRANT,
                    LIGHT_VIBRANT
            )
    };

    abstract val hierarchyList: List<PaletteColor>

    fun getSummaryResId(): Int =
            when (this) {
                LIGHT_VIBRANT -> R.string.palette_light_vibrant
                VIBRANT -> R.string.palette_vibrant
                DARK_VIBRANT -> R.string.palette_dark_vibrant
                LIGHT_MUTED -> R.string.palette_light_muted
                MUTED -> R.string.palette_muted
                DARK_MUTED -> R.string.palette_dark_muted
            }

    companion object {
        fun getFromIndex(index: Int): PaletteColor =
                PaletteColor.values().getOrNull(index) ?: LIGHT_VIBRANT
    }
}

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

fun Context.checkStoragePermission(onNotGranted: ((context: Context) -> Unit)? = null,
                                   onGranted: (context: Context) -> Unit = {}) {
    if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED) {
        onGranted(this)
    } else {
        onNotGranted?.invoke(this)
                ?: this@checkStoragePermission.startActivity(
                        SettingsActivity.getIntent(this).apply {
                            flags = flags or Intent.FLAG_ACTIVITY_NEW_TASK
                        })
    }
}

fun Bitmap.similarity(bitmap: Bitmap): Deferred<Float?> =
        async {
            if (this@similarity.isRecycled) {
                Timber.e(IllegalStateException("Bitmap is recycled"))
                return@async null
            }

            val origin = this@similarity.copy(this@similarity.config, false)
            val other =
                    if (origin.width != bitmap.width || origin.height != bitmap.height)
                        Bitmap.createScaledBitmap(bitmap, origin.width, origin.height, false)
                    else bitmap

            var count = 0
            for (x in 0 until origin.width) {
                for (y in 0 until origin.height) {
                    if (origin.getPixel(x, y).colorSimilarity(other.getPixel(x, y)) > 0.9) count++
                }
            }

            return@async (count.toFloat() / (origin.width * origin.height))
        }

fun String.getUri(): Uri? =
        try {
            Uri.parse(this)
        } catch (e: Exception) {
            Timber.e(e)
            null
        }

fun Int.colorSimilarity(colorInt: Int): Float =
        1f - ((Color.red(this) - Color.red(colorInt)).absoluteValue
                + (Color.green(this) - Color.green(colorInt)).absoluteValue
                + (Color.blue(this) - Color.blue(colorInt)).absoluteValue).toFloat() / (255 * 3)

private fun Palette.getColorFromPaletteColor(paletteColor: PaletteColor): Int =
        when (paletteColor) {
            PaletteColor.LIGHT_VIBRANT -> getLightVibrantColor(Color.TRANSPARENT)
            PaletteColor.VIBRANT -> getVibrantColor(Color.TRANSPARENT)
            PaletteColor.DARK_VIBRANT -> getDarkVibrantColor(Color.TRANSPARENT)
            PaletteColor.LIGHT_MUTED -> getLightMutedColor(Color.TRANSPARENT)
            PaletteColor.MUTED -> getMutedColor(Color.TRANSPARENT)
            PaletteColor.DARK_MUTED -> getDarkMutedColor(Color.TRANSPARENT)
        }

@ColorInt
fun Palette.getOptimizedColor(context: Context): Int {
    val paletteColorHierarchies =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getChosePaletteColor()
                    .hierarchyList

    return paletteColorHierarchies.firstOrNull {
        getColorFromPaletteColor(it) != Color.TRANSPARENT
    }?.let { getColorFromPaletteColor(it) } ?: Color.WHITE
}

fun Activity.setCrashlytics() {
    if (BuildConfig.DEBUG.not()) Fabric.with(this, Crashlytics())
}

fun <T> Gson.fromJsonOrNull(json: String, type: Type,
                            onError: Throwable.() -> Unit = { Timber.e(this) }): T? =
        try {
            this.fromJson(json, type)
        } catch (t: Throwable) {
            onError(t)
            null
        }

fun String.foldBreak(): String =
        this.replace(Regex("[\r\n]"), " ")