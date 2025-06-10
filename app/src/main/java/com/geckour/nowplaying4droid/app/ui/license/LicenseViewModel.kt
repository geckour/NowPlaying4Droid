package com.geckour.nowplaying4droid.app.ui.license

import androidx.lifecycle.ViewModel
import com.geckour.nowplaying4droid.R
import com.geckour.nowplaying4droid.app.domain.model.LicenseItem

class LicenseViewModel : ViewModel() {

    val listItems = listOf(
        LicenseItem(
            R.string.library_coroutines,
            R.string.license_coroutines,
            false
        ),
        LicenseItem(R.string.library_androidx, R.string.license_androidx, false),
        LicenseItem(
            R.string.library_databinding,
            R.string.license_databinding,
            false
        ),
        LicenseItem(R.string.library_timber, R.string.license_timber, false),
        LicenseItem(R.string.library_stetho, R.string.license_stetho, false),
        LicenseItem(R.string.library_koin, R.string.license_koin, false),
        LicenseItem(R.string.library_retrofit, R.string.license_retrofit, false),
        LicenseItem(R.string.library_okhttp, R.string.license_okhttp, false),
        LicenseItem(
            R.string.library_kotlin_serialization,
            R.string.license_kotlin_serialization,
            false
        ),
        LicenseItem(
            R.string.library_kotlin_serialization_converter,
            R.string.license_kotlin_serialization_converter,
            false
        ),
        LicenseItem(R.string.library_coil, R.string.license_coil, false),
        LicenseItem(
            R.string.library_twitter4j,
            R.string.license_twitter4j,
            false
        ),
        LicenseItem(
            R.string.library_mastodon4j,
            R.string.license_mastodon4j,
            false
        )
    )
}