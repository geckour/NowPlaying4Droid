package com.geckour.nowplaying4gpm.util

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.view.View
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.activity.SettingsActivity
import com.geckour.nowplaying4gpm.activity.SettingsActivity.Companion.paletteArray
import kotlinx.coroutines.experimental.Job
import timber.log.Timber

fun String.getSharingText(title: String, artist: String, album: String): String =
        this.splitIncludeDelimiter("'", "TI", "AR", "AL")
                .let { nonEscapeList ->
                    val singleQuoteElement = nonEscapeList.mapIndexed { i, s -> Pair(i, s) }.filter { it.second == "'" }.toMutableList()
                    var list4operate = nonEscapeList

                    while (singleQuoteElement.size > 1) {
                        val fromElement = singleQuoteElement[singleQuoteElement.lastIndex - 1]
                        val toElement = singleQuoteElement[singleQuoteElement.lastIndex]

                        val concatString =
                                nonEscapeList.subList(fromElement.first, toElement.first + 1).joinToString("")

                        list4operate = listOf(
                                *list4operate.subList(0, fromElement.first).toTypedArray(),
                                concatString,
                                *list4operate.subList(
                                        if (toElement.first < nonEscapeList.lastIndex - 1) toElement.first + 1 else nonEscapeList.lastIndex,
                                        nonEscapeList.size
                                ).toTypedArray()
                        )
                        singleQuoteElement.removeAt(singleQuoteElement.lastIndex)
                        singleQuoteElement.removeAt(singleQuoteElement.lastIndex)
                    }

                    list4operate.apply { Timber.d("escaped list: $this") }
                }.joinToString("") {
                    when (it) {
                        "TI" -> title
                        "AR" -> artist
                        "AL" -> album
                        else -> it
                    }
                }

fun String.splitIncludeDelimiter(vararg delimiters: String) =
        delimiters.joinToString("|").let { pattern ->
            this.split(Regex("(?=$pattern)"))
                    .flatMap { it.split(Regex("(?<=$pattern)")) }
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

fun SharedPreferences.init(context: Context) {
    edit().apply {
        if (contains(SettingsActivity.PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name).not())
            putString(SettingsActivity.PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, context.getString(R.string.default_sharing_text_pattern))
        if (contains(SettingsActivity.PrefKey.PREF_KEY_CHOSEN_COLOR_INDEX.name).not())
            putInt(SettingsActivity.PrefKey.PREF_KEY_CHOSEN_COLOR_INDEX.name, paletteArray.indexOf(R.string.palette_light_vibrant))
        if (contains(SettingsActivity.PrefKey.PREF_KEY_WHETHER_RESIDE.name).not())
            putBoolean(SettingsActivity.PrefKey.PREF_KEY_WHETHER_RESIDE.name, true)
        if (contains(SettingsActivity.PrefKey.PREF_KEY_WHETHER_USE_API.name).not())
            putBoolean(SettingsActivity.PrefKey.PREF_KEY_WHETHER_USE_API.name, true)
        if (contains(SettingsActivity.PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK.name).not())
            putBoolean(SettingsActivity.PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK.name, true)
        if (contains(SettingsActivity.PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG.name).not())
            putBoolean(SettingsActivity.PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG.name, true)
    }.apply()
}

fun SharedPreferences.refreshCurrentMetadata(title: String?, artist: String?, album: String?) =
        edit().apply {
            if (title != null) putString(SettingsActivity.PrefKey.PREF_KEY_CURRENT_TITLE.name, title)
            else remove(SettingsActivity.PrefKey.PREF_KEY_CURRENT_TITLE.name)

            if (artist != null) putString(SettingsActivity.PrefKey.PREF_KEY_CURRENT_ARTIST.name, artist)
            else remove(SettingsActivity.PrefKey.PREF_KEY_CURRENT_ARTIST.name)

            if (album != null) putString(SettingsActivity.PrefKey.PREF_KEY_CURRENT_ALBUM.name, album)
            else remove(SettingsActivity.PrefKey.PREF_KEY_CURRENT_ALBUM.name)
        }.apply()

fun SharedPreferences.getFormatPattern(context: Context): String =
        getString(SettingsActivity.PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, context.getString(R.string.default_sharing_text_pattern))

fun SharedPreferences.getChoseColorIndex(): Int =
        getInt(SettingsActivity.PrefKey.PREF_KEY_CHOSEN_COLOR_INDEX.name, paletteArray.indexOf(R.string.palette_light_vibrant))

fun SharedPreferences.getWhetherResideSummaryResId(): Int =
        if (getWhetherReside()) R.string.pref_item_summary_switch_on
        else R.string.pref_item_summary_switch_off

fun SharedPreferences.getWhetherUseApiSummaryResId(): Int =
        if (getWhetherUseApi()) R.string.pref_item_summary_switch_on
        else R.string.pref_item_summary_switch_off

fun SharedPreferences.getWhetherBundleArtworkSummaryResId(): Int =
        if (getWhetherBundleArtwork()) R.string.pref_item_summary_switch_on
        else R.string.pref_item_summary_switch_off

fun SharedPreferences.getWhetherColorizeNotificationBgSummaryResId(): Int =
        if (getWhetherColorizeNotificationBg()) R.string.pref_item_summary_switch_on
        else R.string.pref_item_summary_switch_off

fun SharedPreferences.getWhetherReside(): Boolean =
        contains(SettingsActivity.PrefKey.PREF_KEY_WHETHER_RESIDE.name).not()
                || getBoolean(SettingsActivity.PrefKey.PREF_KEY_WHETHER_RESIDE.name, true)

fun SharedPreferences.getWhetherUseApi(): Boolean =
        contains(SettingsActivity.PrefKey.PREF_KEY_WHETHER_USE_API.name).not()
                || getBoolean(SettingsActivity.PrefKey.PREF_KEY_WHETHER_USE_API.name, true)

fun SharedPreferences.getWhetherBundleArtwork(): Boolean =
        contains(SettingsActivity.PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK.name).not()
                || getBoolean(SettingsActivity.PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK.name, true)

fun SharedPreferences.getWhetherColorizeNotificationBg(): Boolean =
        contains(SettingsActivity.PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG.name).not()
                || getBoolean(SettingsActivity.PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG.name, true)

fun List<Job>.cancelAll() = forEach { it.cancel() }