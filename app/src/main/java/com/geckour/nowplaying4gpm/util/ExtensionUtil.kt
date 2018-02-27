package com.geckour.nowplaying4gpm.util

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.view.View
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.activity.SettingsActivity
import com.geckour.nowplaying4gpm.activity.SettingsActivity.Companion.paletteArray
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import timber.log.Timber
import java.text.MessageFormat
import kotlin.coroutines.experimental.CoroutineContext


fun <T> async(context: CoroutineContext = CommonPool, block: suspend CoroutineScope.() -> T) =
        kotlinx.coroutines.experimental.async(context, block = block)

fun ui(managerList: ArrayList<Job>, onError: (Throwable) -> Unit = {}, block: suspend CoroutineScope.() -> Unit) =
        launch(UI) {
            try { block() }
            catch (e: Exception) {
                Timber.e(e)
                onError(e)
            }
        }.apply { managerList.add(this) }

fun defLaunch(managerList: ArrayList<Job>, onError: (Throwable) -> Unit = {}, block: suspend CoroutineScope.() -> Unit) =
        launch {
            try { block() }
            catch (e: Exception) {
                Timber.e(e)
                onError(e)
            }
        }.apply { managerList.add(this) }

fun String.getSharingText(title: String, artist: String, album: String): String {
    val pattern = this
            .replace("{", "'{'")
            .replace("}", "'}'")
            .replace("TI", "{0}")
            .replace("AR", "{1}")
            .replace("AL", "{2}")
    return MessageFormat.format(pattern, title, artist, album)
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
        if (contains(SettingsActivity.PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK.name).not())
            putBoolean(SettingsActivity.PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK.name, true)
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

fun SharedPreferences.getWhetherBundleArtworkSummaryResId(): Int =
        if (getWhetherBundleArtwork()) R.string.pref_item_summary_switch_on
        else R.string.pref_item_summary_switch_off

fun SharedPreferences.getWhetherReside(): Boolean =
        contains(SettingsActivity.PrefKey.PREF_KEY_WHETHER_RESIDE.name).not()
                || getBoolean(SettingsActivity.PrefKey.PREF_KEY_WHETHER_RESIDE.name, true)

fun SharedPreferences.getWhetherBundleArtwork(): Boolean =
        contains(SettingsActivity.PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK.name).not()
                || getBoolean(SettingsActivity.PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK.name, true)

fun List<Job>.cancelAll() = forEach { it.cancel() }