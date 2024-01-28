package com.geckour.nowplaying4droid.app.ui.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.geckour.nowplaying4droid.R
import com.geckour.nowplaying4droid.app.ui.compose.MilkBlack
import com.geckour.nowplaying4droid.app.ui.compose.MilkWhite

@Composable
fun NP4DAlertDialog(
    title: String,
    message: String? = null,
    onConfirm: (() -> Unit)? = null,
    onDismissRequest: () -> Unit,
    content: (@Composable BoxScope.() -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier.padding(8.dp),
            backgroundColor = if (isSystemInDarkTheme()) MilkBlack else MilkWhite
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle1
                )
                message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                content?.let {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)) {
                        it()
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(id = R.string.dialog_button_ng))
                    }
                    onConfirm?.let {
                        TextButton(onClick = it) {
                            Text(text = stringResource(id = R.string.dialog_button_ok))
                        }
                    }
                }
            }
        }
    }
}