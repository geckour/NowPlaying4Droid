package com.geckour.nowplaying4droid.app.ui.di

import com.geckour.nowplaying4droid.app.ui.settings.SettingsViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val settingsViewModelModule = module {
    viewModel<SettingsViewModel>()
}