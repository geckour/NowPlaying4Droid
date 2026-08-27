package com.geckour.nowplaying4droid.app.ui.license

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import com.geckour.nowplaying4droid.R
import com.geckour.nowplaying4droid.app.domain.model.LicenseItem
import com.geckour.nowplaying4droid.app.ui.compose.DeepRed
import com.geckour.nowplaying4droid.app.ui.compose.HazyBlue
import com.geckour.nowplaying4droid.app.ui.compose.InkBlackWeak
import com.geckour.nowplaying4droid.app.ui.compose.LightRed
import com.geckour.nowplaying4droid.app.ui.compose.SettingsTheme
import com.geckour.nowplaying4droid.app.ui.compose.SmokeWhite

class LicensesActivity : AppCompatActivity() {

    companion object {

        fun getIntent(context: Context): Intent =
            Intent(context, LicensesActivity::class.java)
    }

    private val viewModel: LicenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SettingsTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                windowInsets = WindowInsets.statusBars,
                                backgroundColor = if (isSystemInDarkTheme()) DeepRed else LightRed,
                                contentPadding = PaddingValues(8.dp),
                            ) {
                                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                                    if ((getSystemService<ActivityManager>()?.appTasks
                                        ?.sumOf { it.taskInfo?.numActivities ?: 0 } ?: 0) > 1) {
                                        IconButton(onClick = { finish() }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                                contentDescription = "Back",
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${getString(R.string.activity_title_licenses)} - ${
                                            getString(R.string.app_name)
                                        }",
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    ) {
                        Box(modifier = Modifier.padding(it)) {
                            LazyColumn(
                                modifier = Modifier.fillMaxHeight(),
                                contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                            ) {
                                items(viewModel.listItems) { item ->
                                    LicenceItem(item)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun LicenceItem(item: LicenseItem) {
        Column {
            var opened by rememberSaveable { mutableStateOf(false) }
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { opened = opened.not() }
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                text = getString(item.nameResId),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = HazyBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AnimatedVisibility(visible = opened) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    text = getString(item.textResId),
                    fontSize = 12.sp,
                    color = if (isSystemInDarkTheme()) SmokeWhite else InkBlackWeak,
                )
            }
        }
    }
}