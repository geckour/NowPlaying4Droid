package com.geckour.nowplaying4gpm.util

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.preference.PreferenceManager
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import com.crashlytics.android.Crashlytics
import com.geckour.nowplaying4gpm.BuildConfig
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.geckour.nowplaying4gpm.ui.SettingsActivity
import com.google.gson.Gson
import io.fabric.sdk.android.Fabric
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
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

enum class Visibility {
    PUBLIC,
    UNLISTED,
    PRIVATE;

    fun getSummaryResId(): Int =
            when (this) {
                PUBLIC -> R.string.mastodon_visibility_public
                UNLISTED -> R.string.mastodon_visibility_unlisted
                PRIVATE -> R.string.mastodon_visibility_private
            }

    companion object {
        fun getFromIndex(index: Int): Visibility =
                Visibility.values().getOrNull(index) ?: PUBLIC
    }
}

enum class FormatPattern(val value: String) {
    S_QUOTE("'"),
    S_QUOTE_DOUBLE("''"),
    TITLE("TI"),
    ARTIST("AR"),
    ALBUM("AL"),
    COMPOSER("CO"),
    PLAYER_NAME("PN"),
    SPOTIFY_URL("SU"),
    NEW_LINE("\\n");

    companion object {
        val replaceablePatterns: List<FormatPattern> =
                values().toMutableList().apply {
                    removeAll(listOf(S_QUOTE, S_QUOTE_DOUBLE, NEW_LINE))
                }
    }
}

fun String.getSharingText(trackInfo: TrackInfo, modifiers: List<FormatPatternModifier>): String =
        this.splitConsideringEscape().joinToString("") {
            return@joinToString Regex("^'(.+)'$").let { regex ->
                if (it.matches(regex)) it.replace(regex, "$1")
                else when (it) {
                    FormatPattern.S_QUOTE.value -> ""
                    FormatPattern.S_QUOTE_DOUBLE.value -> "'"
                    FormatPattern.TITLE.value -> trackInfo.coreElement.title
                            ?.getReplacerWithModifier(modifiers, it) ?: ""
                    FormatPattern.ARTIST.value -> trackInfo.coreElement.artist
                            ?.getReplacerWithModifier(modifiers, it) ?: ""
                    FormatPattern.ALBUM.value -> trackInfo.coreElement.album
                            ?.getReplacerWithModifier(modifiers, it) ?: ""
                    FormatPattern.COMPOSER.value -> trackInfo.coreElement.composer
                            ?.getReplacerWithModifier(modifiers, it) ?: ""
                    FormatPattern.PLAYER_NAME.value -> trackInfo.playerAppName
                            ?.getReplacerWithModifier(modifiers, it) ?: ""
                    FormatPattern.SPOTIFY_URL.value -> trackInfo.spotifyUrl
                            ?.getReplacerWithModifier(modifiers, it) ?: ""
                    FormatPattern.NEW_LINE.value -> "\n"
                    else -> it
                }
            }
        }

fun String.getReplacerWithModifier(modifiers: List<FormatPatternModifier>, identifier: String): String =
        "${modifiers.getPrefix(identifier)}$this${modifiers.getSuffix(identifier)}"

fun List<FormatPatternModifier>.getPrefix(value: String): String =
        this.firstOrNull { m -> m.key.value == value }?.prefix ?: ""

fun List<FormatPatternModifier>.getSuffix(value: String): String =
        this.firstOrNull { m -> m.key.value == value }?.suffix ?: ""

fun String.containsPattern(pattern: FormatPattern): Boolean =
        this.splitConsideringEscape().contains(pattern.value)

val String.containedPatterns: List<FormatPattern>
    get() =
        this.splitConsideringEscape().mapNotNull { delimiter ->
            FormatPattern.values().firstOrNull { it.value == delimiter }
        }

private fun String.splitConsideringEscape(): List<String> =
        this.splitIncludeDelimiter(
                FormatPattern.S_QUOTE_DOUBLE.value,
                FormatPattern.S_QUOTE.value,
                FormatPattern.TITLE.value,
                FormatPattern.ARTIST.value,
                FormatPattern.ALBUM.value,
                FormatPattern.COMPOSER.value,
                FormatPattern.PLAYER_NAME.value,
                FormatPattern.SPOTIFY_URL.value,
                "\\\\n"
        ).let { splitList ->
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
        }

fun String.splitIncludeDelimiter(vararg delimiters: String) =
        delimiters.joinToString("|")
                .let { pattern ->
                    this.split(Regex("(?<=$pattern)|(?=$pattern)"))
                }

fun String.escapeSql(): String = replace("'", "''")

fun AlertDialog.Builder.generate(
        view: View,
        title: String,
        message: String? = null,
        callback: (dialog: DialogInterface, which: Int) -> Unit = { _, _ -> }): AlertDialog {
    setTitle(title)
    if (message != null) setMessage(message)
    setView(view)
    setPositiveButton(R.string.dialog_button_ok) { dialog, which -> callback(dialog, which) }
    setNegativeButton(R.string.dialog_button_ng) { dialog, _ -> dialog.dismiss() }

    return create()
}

fun Context.checkStoragePermission(onNotGranted: ((context: Context) -> Unit)? = null,
                                   onGranted: (context: Context) -> Unit = {}) {
    if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED) {
        onGranted(this)
    } else {
        onNotGranted?.invoke(this)
                ?: startActivity(
                        SettingsActivity.getIntent(this).apply {
                            flags = flags or Intent.FLAG_ACTIVITY_NEW_TASK
                        })
    }
}

suspend fun Context.checkStoragePermissionAsync(onNotGranted: (suspend (Context) -> Unit)? = null,
                                   onGranted: suspend (Context) -> Unit = {}) {
    if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED) {
        onGranted(this)
    } else {
        onNotGranted?.invoke(this)
                ?: startActivity(
                        SettingsActivity.getIntent(this).apply {
                            flags = flags or Intent.FLAG_ACTIVITY_NEW_TASK
                        })
    }
}

fun Bitmap.similarity(bitmap: Bitmap): Deferred<Float?> = GlobalScope.async(Dispatchers.IO) {
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

fun Context.setCrashlytics() {
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

fun String.getAppName(context: Context): String? =
        try {
            context.packageManager.let {
                it.getApplicationLabel(it.getApplicationInfo(this, 0)).toString()
            }
        } catch (t: PackageManager.NameNotFoundException) {
            null
        }

fun <T> MutableList<T>.swap(from: Int, to: Int) {
    val tmp = this[to]
    this[to] = this[from]
    this[from] = tmp
}