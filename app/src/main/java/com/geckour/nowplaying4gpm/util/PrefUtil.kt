package com.geckour.nowplaying4gpm.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.domain.model.ArtworkInfo
import com.geckour.nowplaying4gpm.domain.model.TrackInfo
import com.google.gson.Gson
import timber.log.Timber
import twitter4j.auth.AccessToken

enum class PrefKey {
    PREF_KEY_PATTERN_FORMAT_SHARE_TEXT,
    PREF_KEY_CHOSEN_PALETTE_COLOR,
    PREF_KEY_WHETHER_RESIDE,
    PREF_KEY_WHETHER_USE_API,
    PREF_KEY_WHETHER_BUNDLE_ARTWORK,
    PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG,
    PREF_KEY_WHETHER_SHOW_ARTWORK_IN_WIDGET,
    PREF_KEY_WHETHER_LAUNCH_GPM_WITH_WIDGET_ARTWORK,
    PREF_KEY_CURRENT_TRACK_INFO,
    PREF_KEY_TEMP_ARTWORK_INFO,
    PREF_KEY_BILLING_DONATE,
    PREF_KEY_TWITTER_ACCESS_TOKEN,
    PREF_KEY_FLAG_ALERT_AUTH_TWITTER,
    PREF_KEY_NODE_ID_RECEIVE_REQUEST_DELEGATE_SHARE
}

fun SharedPreferences.init(context: Context) {
    edit().apply {
        if (contains(PrefKey.PREF_KEY_WHETHER_USE_API.name).not())
            putBoolean(PrefKey.PREF_KEY_WHETHER_USE_API.name, false)
        if (contains(PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name).not())
            putString(PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name, context.getString(R.string.default_sharing_text_pattern))
        if (contains(PrefKey.PREF_KEY_CHOSEN_PALETTE_COLOR.name).not())
            putInt(PrefKey.PREF_KEY_CHOSEN_PALETTE_COLOR.name, PaletteColor.LIGHT_VIBRANT.ordinal)
        if (contains(PrefKey.PREF_KEY_WHETHER_RESIDE.name).not())
            putBoolean(PrefKey.PREF_KEY_WHETHER_RESIDE.name, true)
        if (contains(PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK.name).not())
            putBoolean(PrefKey.PREF_KEY_WHETHER_BUNDLE_ARTWORK.name, true)
        if (contains(PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG.name).not())
            putBoolean(PrefKey.PREF_KEY_WHETHER_COLORIZE_NOTIFICATION_BG.name, true)
        if (contains(PrefKey.PREF_KEY_WHETHER_SHOW_ARTWORK_IN_WIDGET.name).not())
            putBoolean(PrefKey.PREF_KEY_WHETHER_SHOW_ARTWORK_IN_WIDGET.name, true)
        if (contains(PrefKey.PREF_KEY_WHETHER_LAUNCH_GPM_WITH_WIDGET_ARTWORK.name).not())
            putBoolean(PrefKey.PREF_KEY_WHETHER_LAUNCH_GPM_WITH_WIDGET_ARTWORK.name, true)
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

private fun SharedPreferences.setTempArtworkInfo(artworkUri: Uri?) {
    edit().putString(
            PrefKey.PREF_KEY_TEMP_ARTWORK_INFO.name,
            Gson().toJson(ArtworkInfo(artworkUri?.toString()))).apply()
}

fun SharedPreferences.getTempArtworkInfo(): ArtworkInfo? =
        if (contains(PrefKey.PREF_KEY_TEMP_ARTWORK_INFO.name))
            Gson().fromJson(getString(PrefKey.PREF_KEY_TEMP_ARTWORK_INFO.name, null), ArtworkInfo::class.java)
        else null

fun SharedPreferences.getTempArtworkUri(context: Context): Uri? {
    val uri = getTempArtworkInfo()?.artworkUriString?.getUri() ?: return null

    return try {
        context.contentResolver.openInputStream(uri).close()
        uri
    } catch (e: Throwable) {
        Timber.e(e)
        null
    }
}

fun SharedPreferences.refreshTempArtwork(artworkUri: Uri?) {
    setTempArtworkInfo(artworkUri)
}

fun SharedPreferences.getSharingText(context: Context): String? {
    val trackInfo = getCurrentTrackInfo() ?: return null

    if (trackInfo.coreElement.isAllNonNull.not()) return null
    return getFormatPattern(context).getSharingText(trackInfo.coreElement)
}

fun SharedPreferences.getCurrentTrackInfo(): TrackInfo? =
        if (contains(PrefKey.PREF_KEY_CURRENT_TRACK_INFO.name))
            try {
                Gson().fromJson(
                        getString(PrefKey.PREF_KEY_CURRENT_TRACK_INFO.name, null),
                        TrackInfo::class.java)
            } catch (t: Throwable) {
                Timber.e(t)
                refreshCurrentTrackInfo(TrackInfo.empty)
                null
            }
        else null

fun SharedPreferences.getChosePaletteColor(): PaletteColor =
        PaletteColor.values().getOrNull(
                getInt(
                        PrefKey.PREF_KEY_CHOSEN_PALETTE_COLOR.name,
                        PaletteColor.LIGHT_VIBRANT.ordinal)
        ) ?: PaletteColor.LIGHT_VIBRANT

fun SharedPreferences.getSwitchSummaryResId(key: PrefKey): Int =
        if (getSwitchState(key)) R.string.pref_item_summary_switch_on
        else R.string.pref_item_summary_switch_off

fun SharedPreferences.getSwitchState(key: PrefKey): Boolean =
        contains(key.name).not()
                || getBoolean(key.name, true)

fun SharedPreferences.getDonateBillingState(): Boolean =
        contains(PrefKey.PREF_KEY_BILLING_DONATE.name)
                && getBoolean(PrefKey.PREF_KEY_BILLING_DONATE.name, false)

fun SharedPreferences.storeTwitterAccessToken(accessToken: AccessToken) {
    edit().putString(PrefKey.PREF_KEY_TWITTER_ACCESS_TOKEN.name, Gson().toJson(accessToken)).apply()
}

fun SharedPreferences.getTwitterAccessToken(): AccessToken? =
        if (contains(PrefKey.PREF_KEY_TWITTER_ACCESS_TOKEN.name))
            Gson().fromJson(getString(PrefKey.PREF_KEY_TWITTER_ACCESS_TOKEN.name, null), AccessToken::class.java)
        else null

fun SharedPreferences.setAlertTwitterAuthFlag(flag: Boolean) {
    edit().putBoolean(PrefKey.PREF_KEY_FLAG_ALERT_AUTH_TWITTER.name, flag).apply()
}

fun SharedPreferences.getAlertTwitterAuthFlag(): Boolean =
        if (contains(PrefKey.PREF_KEY_FLAG_ALERT_AUTH_TWITTER.name))
            getBoolean(PrefKey.PREF_KEY_FLAG_ALERT_AUTH_TWITTER.name, false)
        else false

fun SharedPreferences.setReceivedDelegateShareNodeId(nodeId: String?) {
    edit().putString(PrefKey.PREF_KEY_NODE_ID_RECEIVE_REQUEST_DELEGATE_SHARE.name, nodeId).apply()
}

fun SharedPreferences.getReceivedDelegateShareNodeId(): String? =
        if (contains(PrefKey.PREF_KEY_NODE_ID_RECEIVE_REQUEST_DELEGATE_SHARE.name))
            getString(PrefKey.PREF_KEY_NODE_ID_RECEIVE_REQUEST_DELEGATE_SHARE.name, null)
        else null