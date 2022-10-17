package com.geckour.nowplaying4gpm.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PackageState(
    val packageName: String,
    var state: Boolean = true
)