package com.geckour.nowplaying4gpm.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.activity.SettingsActivity
import timber.log.Timber

enum class PrefKey {
    PREF_KEY_PATTERN_FORMAT_SHARE_TEXT,
    PREF_KEY_CHOSEN_COLOR_INDEX,
    PREF_KEY_WHETHER_RESIDE,
    PREF_KEY_WHETHER_USE_API,
    PREF_KEY_WHETHER_BUNDLE_ARTWORK,
    PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG,
    PREF_KEY_CURRENT_TITLE,
    PREF_KEY_CURRENT_ARTIST,
    PREF_KEY_CURRENT_ALBUM,
    PREF_KEY_WHETHER_SONG_CHANGED,
    PREF_KEY_TEMP_ALBUM_ART_URI_STRING,
    PREF_KEY_BILLING_DONATE
}

fun SharedPreferences.init(context: Context) {
    edit().apply {
        if (contains(PrefKey.PREF_KEY_WHETHER_USE_API.name).not())
            putBoolean(PrefKey.PREF_KEY_WHETHER_USE_API.name, false)
        if (contains(PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name).not())
            putString(PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, context.getString(R.string.default_sharing_text_pattern))
        if (contains(PrefKey.PREF_KEY_CHOSEN_COLOR_INDEX.name).not())
            putInt(PrefKey.PREF_KEY_CHOSEN_COLOR_INDEX.name, SettingsActivity.paletteArray.indexOf(R.string.palette_light_vibrant))
        if (contains(PrefKey.PREF_KEY_WHETHER_RESIDE.name).not())
            putBoolean(PrefKey.PREF_KEY_WHETHER_RESIDE.name, true)
        if (contains(PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK.name).not())
            putBoolean(PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK.name, true)
        if (contains(PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG.name).not())
            putBoolean(PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG.name, true)
        if (contains(PrefKey.PREF_KEY_BILLING_DONATE.name).not())
            putBoolean(PrefKey.PREF_KEY_BILLING_DONATE.name, false)
    }.apply()
}

fun SharedPreferences.refreshCurrentMetadata(title: String?, artist: String?, album: String?) =
        edit().apply {
            if (title != null) putString(PrefKey.PREF_KEY_CURRENT_TITLE.name, title)
            else remove(PrefKey.PREF_KEY_CURRENT_TITLE.name)

            if (artist != null) putString(PrefKey.PREF_KEY_CURRENT_ARTIST.name, artist)
            else remove(PrefKey.PREF_KEY_CURRENT_ARTIST.name)

            if (album != null) putString(PrefKey.PREF_KEY_CURRENT_ALBUM.name, album)
            else remove(PrefKey.PREF_KEY_CURRENT_ALBUM.name)
        }.apply()

fun SharedPreferences.getFormatPattern(context: Context): String =
        getString(PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, context.getString(R.string.default_sharing_text_pattern))

fun SharedPreferences.getTempArtUri(): Uri? =
        if (contains(PrefKey.PREF_KEY_TEMP_ALBUM_ART_URI_STRING.name)) {
            this.getString(PrefKey.PREF_KEY_TEMP_ALBUM_ART_URI_STRING.name, null).let {
                try {
                    Uri.parse(it)
                } catch (e: Throwable) {
                    Timber.d(e)
                    null
                }
            }
        } else null

fun SharedPreferences.setTempArtUriString(uri: Uri?) =
        edit().putString(PrefKey.PREF_KEY_TEMP_ALBUM_ART_URI_STRING.name, uri?.toString()).apply()

fun SharedPreferences.deleteTempArt(context: Context) {
    val uri = getTempArtUri()
    if (uri != null) context.contentResolver.delete(uri, null, null)
}

fun SharedPreferences.getSharingText(context: Context): String? {
    val title = getCurrentTitle()
    val artist = getCurrentArtist()
    val album = getCurrentAlbum()

    if (title == null || artist == null || album == null) return null
    return getFormatPattern(context).getSharingText(title, artist, album)
}

fun SharedPreferences.getCurrentTitle(): String? =
        if (contains(PrefKey.PREF_KEY_CURRENT_TITLE.name))
            getString(PrefKey.PREF_KEY_CURRENT_TITLE.name, null)
        else null

fun SharedPreferences.getCurrentArtist(): String? =
        if (contains(PrefKey.PREF_KEY_CURRENT_ARTIST.name))
            getString(PrefKey.PREF_KEY_CURRENT_ARTIST.name, null)
        else null

fun SharedPreferences.getCurrentAlbum(): String? =
        if (contains(PrefKey.PREF_KEY_CURRENT_ALBUM.name))
            getString(PrefKey.PREF_KEY_CURRENT_ALBUM.name, null)
        else null

fun SharedPreferences.getWhetherSongChanged(): Boolean =
        if (contains(PrefKey.PREF_KEY_WHETHER_SONG_CHANGED.name))
            getBoolean(PrefKey.PREF_KEY_WHETHER_SONG_CHANGED.name, true)
        else true

fun SharedPreferences.getChoseColorIndex(): Int =
        getInt(PrefKey.PREF_KEY_CHOSEN_COLOR_INDEX.name, SettingsActivity.paletteArray.indexOf(R.string.palette_light_vibrant))

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
        contains(PrefKey.PREF_KEY_WHETHER_RESIDE.name).not()
                || getBoolean(PrefKey.PREF_KEY_WHETHER_RESIDE.name, true)

fun SharedPreferences.getWhetherUseApi(): Boolean =
        contains(PrefKey.PREF_KEY_WHETHER_USE_API.name)
                && getBoolean(PrefKey.PREF_KEY_WHETHER_USE_API.name, false)

fun SharedPreferences.getWhetherBundleArtwork(): Boolean =
        contains(PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK.name).not()
                || getBoolean(PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK.name, true)

fun SharedPreferences.getWhetherColorizeNotificationBg(): Boolean =
        contains(PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG.name).not()
                || getBoolean(PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG.name, true)

fun SharedPreferences.getDonateBillingState(): Boolean =
        contains(PrefKey.PREF_KEY_BILLING_DONATE.name)
                && getBoolean(PrefKey.PREF_KEY_BILLING_DONATE.name, false)