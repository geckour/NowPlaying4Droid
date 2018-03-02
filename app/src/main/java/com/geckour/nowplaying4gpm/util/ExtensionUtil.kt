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
        this.splitIncludeDelimiter("''", "'", "TI", "AR", "AL")
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

fun SharedPreferences.getSharingText(context: Context): String? {
    val title = getCurrentTitle()
    val artist = getCurrentArtist()
    val album = getCurrentAlbum()


    return if (title == null || artist == null || album == null) null
    else getFormatPattern(context).getSharingText(title, artist, album)
}

fun SharedPreferences.getCurrentTitle(): String? =
        if (contains(SettingsActivity.PrefKey.PREF_KEY_CURRENT_TITLE.name)) getString(SettingsActivity.PrefKey.PREF_KEY_CURRENT_TITLE.name, null)
        else null

fun SharedPreferences.getCurrentArtist(): String? =
        if (contains(SettingsActivity.PrefKey.PREF_KEY_CURRENT_ARTIST.name)) getString(SettingsActivity.PrefKey.PREF_KEY_CURRENT_ARTIST.name, null)
        else null

fun SharedPreferences.getCurrentAlbum(): String? =
        if (contains(SettingsActivity.PrefKey.PREF_KEY_CURRENT_ALBUM.name)) getString(SettingsActivity.PrefKey.PREF_KEY_CURRENT_ALBUM.name, null)
        else null

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