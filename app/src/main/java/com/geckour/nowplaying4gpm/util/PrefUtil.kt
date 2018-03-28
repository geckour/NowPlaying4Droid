package com.geckour.nowplaying4gpm.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.activity.SettingsActivity
import com.geckour.nowplaying4gpm.domain.model.ArtworkInfo
import com.geckour.nowplaying4gpm.domain.model.TrackCoreElement
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.google.gson.Gson
import timber.log.Timber

enum class PrefKey {
    PREF_KEY_PATTERN_FORMAT_SHARE_TEXT,
    PREF_KEY_CHOSEN_COLOR_INDEX,
    PREF_KEY_WHETHER_RESIDE,
    PREF_KEY_WHETHER_USE_API,
    PREF_KEY_WHETHER_BUNDLE_ARTWORK,
    PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG,
    PREF_KEY_CURRENT_TRACK_INFO,
    PREF_KEY_TEMP_ARTWORK_INFO,
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

fun SharedPreferences.refreshCurrentTrackInfo(trackInfo: TrackInfo) =
        edit().apply {
            putString(
                    PrefKey.PREF_KEY_CURRENT_TRACK_INFO.name,
                    Gson().toJson(trackInfo))
        }.apply()

fun SharedPreferences.getFormatPattern(context: Context): String =
        getString(PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, context.getString(R.string.default_sharing_text_pattern))

fun SharedPreferences.setTempArtworkInfo(artworkInfo: ArtworkInfo) {
    edit().putString(
            PrefKey.PREF_KEY_TEMP_ARTWORK_INFO.name,
            Gson().toJson(artworkInfo)).apply()
}

fun SharedPreferences.getTempArtworkInfo(): ArtworkInfo? =
        if (contains(PrefKey.PREF_KEY_TEMP_ARTWORK_INFO.name))
            Gson().fromJson(getString(PrefKey.PREF_KEY_TEMP_ARTWORK_INFO.name, null), ArtworkInfo::class.java)
        else null

fun SharedPreferences.deleteTempArtwork(context: Context) {
    val artworkInfo = getTempArtworkInfo() ?: return

    if (artworkInfo.fromContentResolver.not()) {
        try {
            val uri = Uri.parse(artworkInfo.artworkUriString) ?: return
            context.contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}

fun SharedPreferences.getSharingText(context: Context): String? {
    val trackInfo = getCurrentTrackInfo() ?: return null

    if (trackInfo.coreElement.isAllNonNull.not()) return null
    return getFormatPattern(context).getSharingText(trackInfo.coreElement)
}

fun SharedPreferences.getCurrentTrackInfo(): TrackInfo? =
        if (contains(PrefKey.PREF_KEY_CURRENT_TRACK_INFO.name))
            Gson().fromJson(
                    getString(PrefKey.PREF_KEY_CURRENT_TRACK_INFO.name, null),
                    TrackInfo::class.java)
        else null

fun SharedPreferences.getChoseColorIndex(): Int =
        getInt(
                PrefKey.PREF_KEY_CHOSEN_COLOR_INDEX.name,
                SettingsActivity.paletteArray.indexOf(R.string.palette_light_vibrant))

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