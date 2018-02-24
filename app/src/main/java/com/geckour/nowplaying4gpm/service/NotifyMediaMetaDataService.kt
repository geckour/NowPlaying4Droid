package com.geckour.nowplaying4gpm.service

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.service.notification.NotificationListenerService
import android.support.v4.content.ContextCompat
import com.geckour.nowplaying4gpm.R
import com.geckour.nowplaying4gpm.activity.SettingsActivity
import com.geckour.nowplaying4gpm.activity.SharingActivity
import com.geckour.nowplaying4gpm.util.async
import com.geckour.nowplaying4gpm.util.escapeSql
import com.geckour.nowplaying4gpm.util.getSharingText
import com.geckour.nowplaying4gpm.util.ui
import kotlinx.coroutines.experimental.Job
import timber.log.Timber
import java.io.FileNotFoundException

class NotifyMediaMetaDataService: NotificationListenerService() {

    companion object {
        const val ACTION_GPM_META_CHANGED: String = "com.android.music.metachanged"
        const val ACTION_GPM_PLAY_STATE_CHANGED: String = "com.android.music.playstatechanged"
        const val EXTRA_GPM_ARTIST: String = "artist"
        const val EXTRA_GPM_ALBUM: String = "album"
        const val EXTRA_GPM_TRACK: String = "track"
        const val EXTRA_GPM_PLAYING: String = "playing"
        const val ACTION_GPM_QUEUE_CHANGED: String = "com.android.music.queuechanged"

        fun getIntent(context: Context): Intent = Intent(context, NotifyMediaMetaDataService::class.java)
    }

    private val sharedPreferences: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(applicationContext) }
    private val jobs: ArrayList<Job> = ArrayList()

    private val receiver: BroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.apply {
                if (ContextCompat.checkSelfPermission(
                                this@NotifyMediaMetaDataService,
                                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    SettingsActivity.getIntent(this@NotifyMediaMetaDataService).apply {
                        startActivity(this)
                    }
                } else {
                    when (action) {
                        ACTION_GPM_META_CHANGED -> {
                            showNotification(this)
                        }

                        ACTION_GPM_PLAY_STATE_CHANGED -> {
                            if (hasExtra(EXTRA_GPM_PLAYING) && !getBooleanExtra(EXTRA_GPM_PLAYING, true)) destroyNotification()
                            else showNotification(this)
                        }
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        val intentFilter = IntentFilter().apply {
            addAction(ACTION_GPM_META_CHANGED)
            addAction(ACTION_GPM_PLAY_STATE_CHANGED)
        }
        registerReceiver(receiver, intentFilter)
    }

    private fun showNotification(intent: Intent) {
        ui(jobs) {
            val title = if (intent.hasExtra(EXTRA_GPM_TRACK)) intent.getStringExtra(EXTRA_GPM_TRACK) else null
            val artist = if (intent.hasExtra(EXTRA_GPM_ARTIST)) intent.getStringExtra(EXTRA_GPM_ARTIST) else null
            val album = if (intent.hasExtra(EXTRA_GPM_ALBUM)) intent.getStringExtra(EXTRA_GPM_ALBUM) else null
            val albumArtUri = async {
                val cursor = contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        arrayOf(MediaStore.Audio.Media.ALBUM_ID),
                        getContentQuerySelection(title, artist, album),
                        null,
                        null
                )

                return@async (if (cursor.moveToNext()) {
                    cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)).let {
                        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), it)
                    }
                } else null).apply { cursor.close() }
            }.await()
            val albumArt =
                    try {
                        if (albumArtUri != null) {
                            contentResolver.openInputStream(albumArtUri).let {
                                BitmapFactory.decodeStream(it, null, null)
                            }
                        } else null
                    } catch (e: FileNotFoundException) {
                        Timber.e(e)
                        null
                    }

            getNotification(
                    albumArt,
                    title,
                    artist,
                    album,
                    albumArtUri
            )?.apply { (getSystemService(NotificationManager::class.java)).notify(0, this) }
        }
    }

    private fun getContentQuerySelection(title: String?, artist: String?, album: String?): String =
            "${MediaStore.Audio.Media.TITLE}='${title?.escapeSql()}' and ${MediaStore.Audio.Media.ARTIST}='${artist?.escapeSql()}' and ${MediaStore.Audio.Media.ALBUM}='${album?.escapeSql()}'"

    private fun destroyNotification() {
        (getSystemService(NotificationManager::class.java)).cancelAll()
    }

    private fun getNotification(thumb: Bitmap?, title: String?, artist: String?, album: String?, albumArtUri: Uri?): Notification? =
            if (title == null || artist == null || album == null) null
            else {
                (if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, NotificationChannel.DEFAULT_CHANNEL_ID)
                else Notification.Builder(this)).apply {
                    setSmallIcon(R.drawable.ic_notification)
                    setLargeIcon(thumb)
                    setContentTitle(getString(R.string.notification_title))
                    val notificationText =
                            sharedPreferences.getString(
                                    SettingsActivity.PrefKey.PREF_KEY_PATTERN_FORMAT_SHARE_TEXT.name,
                                    getString(R.string.default_sharing_text_pattern))
                                    .getSharingText(title, artist, album)
                    setContentText(notificationText)
                    setStyle(Notification.BigPictureStyle()
                            .setBigContentTitle(getString(R.string.notification_title))
                            .setSummaryText(notificationText).bigPicture(thumb)
                            .bigLargeIcon(thumb))
                    setContentIntent(
                            PendingIntent.getActivity(
                                    this@NotifyMediaMetaDataService,
                                    0,
                                    SharingActivity.createIntent(this@NotifyMediaMetaDataService, notificationText, albumArtUri),
                                    PendingIntent.FLAG_CANCEL_CURRENT
                            )
                    )
                }.build()
            }
}