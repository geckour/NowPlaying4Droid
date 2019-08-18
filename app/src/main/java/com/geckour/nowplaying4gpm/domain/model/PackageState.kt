package com.geckour.nowplaying4gpm.domain.model

data class PackageState(
    val packageName: String,
    var state: Boolean = true
)