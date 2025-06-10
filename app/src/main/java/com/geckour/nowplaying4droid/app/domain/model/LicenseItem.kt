package com.geckour.nowplaying4droid.app.domain.model

import androidx.annotation.StringRes

data class LicenseItem(
    @StringRes val nameResId: Int,
    @StringRes val textResId: Int,
    var stateOpen: Boolean
)