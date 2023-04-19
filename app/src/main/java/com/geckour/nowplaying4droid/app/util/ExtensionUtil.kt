package com.geckour.nowplaying4droid.app.util

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.Html
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.palette.graphics.Palette
import androidx.preference.PreferenceManager
import com.geckour.nowplaying4droid.BuildConfig
import com.geckour.nowplaying4droid.R
import com.geckour.nowplaying4droid.app.domain.model.MediaIdInfo
import com.geckour.nowplaying4droid.app.domain.model.TrackDetail
import com.geckour.nowplaying4droid.app.service.NotificationService
import com.geckour.nowplaying4droid.app.ui.settings.SettingsActivity
import com.geckour.nowplayingsubjectbuilder.lib.model.FormatPattern
import com.geckour.nowplayingsubjectbuilder.lib.model.FormatPatternModifier
import com.geckour.nowplayingsubjectbuilder.lib.model.TrackInfo
import com.geckour.nowplayingsubjectbuilder.lib.util.splitConsideringEscape
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.sys1yagi.mastodon4j.api.entity.Status
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

enum class PaletteColor {
    LIGHT_VIBRANT {
        override val hierarchyList: List<PaletteColor>
            get() = listOf(
                LIGHT_VIBRANT, VIBRANT, DARK_VIBRANT, LIGHT_MUTED, MUTED, DARK_MUTED
            )
    },
    VIBRANT {
        override val hierarchyList: List<PaletteColor>
            get() = listOf(
                VIBRANT, LIGHT_VIBRANT, DARK_VIBRANT, MUTED, LIGHT_MUTED, DARK_MUTED
            )
    },
    DARK_VIBRANT {
        override val hierarchyList: List<PaletteColor>
            get() = listOf(
                DARK_VIBRANT, VIBRANT, LIGHT_VIBRANT, DARK_MUTED, MUTED, LIGHT_MUTED
            )
    },
    LIGHT_MUTED {
        override val hierarchyList: List<PaletteColor>
            get() = listOf(
                LIGHT_MUTED, MUTED, DARK_MUTED, LIGHT_VIBRANT, VIBRANT, DARK_VIBRANT
            )
    },
    MUTED {
        override val hierarchyList: List<PaletteColor>
            get() = listOf(
                MUTED, LIGHT_MUTED, DARK_MUTED, VIBRANT, LIGHT_VIBRANT, DARK_VIBRANT
            )
    },
    DARK_MUTED {
        override val hierarchyList: List<PaletteColor>
            get() = listOf(
                DARK_MUTED, MUTED, LIGHT_MUTED, DARK_VIBRANT, VIBRANT, LIGHT_VIBRANT
            )
    };

    abstract val hierarchyList: List<PaletteColor>

    fun getSummaryResId(): Int = when (this) {
        LIGHT_VIBRANT -> R.string.palette_light_vibrant
        VIBRANT -> R.string.palette_vibrant
        DARK_VIBRANT -> R.string.palette_dark_vibrant
        LIGHT_MUTED -> R.string.palette_light_muted
        MUTED -> R.string.palette_muted
        DARK_MUTED -> R.string.palette_dark_muted
    }
}

enum class Visibility {
    PUBLIC, UNLISTED, PRIVATE;

    fun getSummaryResId(): Int = when (this) {
        PUBLIC -> R.string.mastodon_visibility_public
        UNLISTED -> R.string.mastodon_visibility_unlisted
        PRIVATE -> R.string.mastodon_visibility_private
    }
}

inline fun <reified T> withCatching(
    onError: (Throwable) -> Unit = {},
    block: () -> T
) = runCatching {
    block()
}.onFailure {
    Timber.e(it)
    FirebaseCrashlytics.getInstance().recordException(it)
    onError(it)
}.getOrNull()

fun String.getSharingText(
    trackInfo: TrackInfo?,
    modifiers: List<FormatPatternModifier>,
    requireMatchAllPattern: Boolean
): String? =
    trackInfo?.getSharingSubject(this, modifiers, requireMatchAllPattern)

fun String.containsPattern(pattern: FormatPattern): Boolean =
    this.splitConsideringEscape().contains(pattern.value)

fun Context.checkStoragePermission(
    onNotGranted: ((context: Context) -> Unit)? = null, onGranted: (context: Context) -> Unit = {}
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        onGranted(this)
    } else {
        onNotGranted?.invoke(this) ?: startActivity(SettingsActivity.getIntent(this).apply {
            flags = flags or Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}

fun String.getUri(): Uri? = withCatching { Uri.parse(this) }

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
        PreferenceManager.getDefaultSharedPreferences(context).getChosePaletteColor().hierarchyList

    return paletteColorHierarchies.firstOrNull {
        getColorFromPaletteColor(it) != Color.TRANSPARENT
    }?.let { getColorFromPaletteColor(it) } ?: Color.WHITE
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> Json.parseOrNull(
    json: String?,
    deserializationStrategy: DeserializationStrategy<T>? = null,
    onError: Throwable.() -> Unit = {}
): T? = withCatching(onError) {
    deserializationStrategy?.let { this.decodeFromString(it, json!!) }
        ?: this.decodeFromString(T::class.serializer(), json!!)
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> Json.parseListOrNull(
    json: String?,
    onError: Throwable.() -> Unit = {}
): List<T>? = this.parseOrNull(json, ListSerializer(T::class.serializer()), onError)

fun String.foldBreaks(): String = this.replace(Regex("[\r\n]"), " ")

fun <T> MutableList<T>.swap(from: Int, to: Int) {
    val tmp = this[to]
    this[to] = this[from]
    this[from] = tmp
}

fun Context.getArtworkUriFromDevice(trackCoreElement: TrackDetail.TrackCoreElement): Uri? =
    getMediaIdInfoFromDevice(trackCoreElement)?.let {
        withCatching {
            val contentUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it.mediaTrackId
            )
            val retriever = MediaMetadataRetriever().apply {
                setDataSource(this@getArtworkUriFromDevice, contentUri)
            }
            retriever.embeddedPicture?.toBitmap()?.refreshArtworkUri(this)
                ?: ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), it.mediaAlbumId
                ).also { uri ->
                    contentResolver.openInputStream(uri)?.close() ?: throw IllegalStateException()
                    PreferenceManager.getDefaultSharedPreferences(this).refreshTempArtwork(uri)
                }
        }
    }

private fun Context.getMediaIdInfoFromDevice(
    trackCoreElement: TrackDetail.TrackCoreElement
): MediaIdInfo? {
    val args = trackCoreElement.contentQueryArgs ?: return null

    return withCatching {
        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ALBUM_ID),
            contentQuerySelection,
            args,
            null
        )?.use { it.getMediaIdInfoFromDevice() }
    }
}

private fun Cursor?.getMediaIdInfoFromDevice(): MediaIdInfo? =
    if (this?.moveToFirst() == true) {
        withCatching {
            MediaIdInfo(
                getLong(getColumnIndexOrThrow(MediaStore.Audio.Media._ID)),
                getLong(getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
            )
        }
    } else null

fun ByteArray.toBitmap(): Bitmap? =
    withCatching { BitmapFactory.decodeByteArray(this, 0, this.size) }

fun Bitmap.refreshArtworkUri(context: Context): Uri? {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val dirName = "images"
    val fileName = "temp_artwork.png"
    val dir = File(context.cacheDir, dirName)
    val file = File(dir, fileName)

    if (file.exists()) file.delete()
    if (dir.exists().not()) dir.mkdirs()

    FileOutputStream(file).use {
        compress(Bitmap.CompressFormat.PNG, 100, it)
        it.flush()
    }

    return FileProvider.getUriForFile(context, BuildConfig.FILES_AUTHORITY, file).apply {
        sharedPreferences.refreshTempArtwork(this)
    }
}

fun Bitmap.toByteArray(): ByteArray? =
    if (isRecycled) null
    else {
        ByteArrayOutputStream().apply {
            compress(Bitmap.CompressFormat.PNG, 100, this)
        }.toByteArray()
    }

@OptIn(InternalSerializationApi::class)
fun Serializable.asString(): String =
    ByteArrayOutputStream().use { byteArrayStream ->
        ObjectOutputStream(byteArrayStream).writeObject(this)
        json.encodeToString(ByteArray::class.serializer(), byteArrayStream.toByteArray())
    }

fun Context.digMediaController(playerPackageName: String? = null): MediaController? =
    withCatching {
        getSystemService(MediaSessionManager::class.java)
            ?.getActiveSessions(NotificationService.getComponentName(applicationContext))
            ?.let { sessions ->
                if (playerPackageName == null) sessions.firstOrNull()
                else sessions.firstOrNull { it.packageName == playerPackageName }
            }
    }

inline fun <reified T : Serializable> String.toSerializableObject(): T? =
    withCatching {
        json.parseOrNull<ByteArray>(this).let { byteArray ->
            ByteArrayInputStream(byteArray).use { ObjectInputStream(it).readObject() as T }
        }
    }

fun MediaMetadata.getTrackCoreElement(): TrackDetail.TrackCoreElement = this.let {
    val track: String? =
        if (it.containsKey(MediaMetadata.METADATA_KEY_TITLE)) it.getString(MediaMetadata.METADATA_KEY_TITLE)
        else null
    val artist: String? = when {
        it.containsKey(MediaMetadata.METADATA_KEY_ARTIST) -> it.getString(MediaMetadata.METADATA_KEY_ARTIST)
        it.containsKey(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) -> it.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        else -> null
    }
    val album: String? =
        if (it.containsKey(MediaMetadata.METADATA_KEY_ALBUM)) it.getString(MediaMetadata.METADATA_KEY_ALBUM)
        else null
    val composer: String? =
        if (it.containsKey(MediaMetadata.METADATA_KEY_COMPOSER)) it.getString(MediaMetadata.METADATA_KEY_COMPOSER)
        else null

    TrackDetail.TrackCoreElement(track, artist, album, composer)
}

fun NotificationManager.destroyNotification() {
    cancelAll()
}

private suspend fun Status.getNotification(context: Context): Notification {
    val notificationBuilder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(
                context,
                NotificationService.Channel.NOTIFICATION_CHANNEL_SHARE.name
            )
        else Notification.Builder(context)

    return notificationBuilder.apply {
        val notificationText =
            Html.fromHtml(this@getNotification.content, Html.FROM_HTML_MODE_COMPACT).toString()

        val thumb = this@getNotification.mediaAttachments
            .firstOrNull()
            ?.url
            ?.let { context.getBitmapFromUriString(it) }

        setSmallIcon(R.drawable.ic_notification_notify)
        setLargeIcon(thumb)
        setContentTitle(context.getString(R.string.notification_title_notify_success_mastodon))
        setContentText(notificationText)
        style = Notification.DecoratedMediaCustomViewStyle()
        thumb?.apply {
            val color = Palette.from(this)
                .maximumColorCount(24)
                .generate()
                .getOptimizedColor(context)
            setColor(color)
        }
    }.build()
}