package com.geckour.nowplaying4gpm.ui.license

import androidx.lifecycle.ViewModel
import com.geckour.nowplaying4gpm.R

class LicenseViewModel : ViewModel() {
    val listItems = listOf(
        LicenseListAdapter.LicenseItem(R.string.library_coroutines, R.string.license_coroutines, false),
        LicenseListAdapter.LicenseItem(R.string.library_androidx, R.string.license_androidx, false),
        LicenseListAdapter.LicenseItem(R.string.library_databinding, R.string.license_databinding, false),
        LicenseListAdapter.LicenseItem(R.string.library_timber, R.string.license_timber, false),
        LicenseListAdapter.LicenseItem(R.string.library_stetho, R.string.license_stetho, false),
        LicenseListAdapter.LicenseItem(R.string.library_retrofit, R.string.license_retrofit, false),
        LicenseListAdapter.LicenseItem(R.string.library_okhttp, R.string.license_okhttp, false),
        LicenseListAdapter.LicenseItem(
            R.string.library_coroutines4retrofit,
            R.string.license_coroutines4retrofit,
            false
        ),
        LicenseListAdapter.LicenseItem(R.string.library_glide, R.string.license_glide, false),
        LicenseListAdapter.LicenseItem(R.string.library_twitter4j, R.string.license_twitter4j, false),
        LicenseListAdapter.LicenseItem(R.string.library_gson, R.string.license_gson, false),
        LicenseListAdapter.LicenseItem(R.string.library_mastodon4j, R.string.license_mastodon4j, false)
    )
}