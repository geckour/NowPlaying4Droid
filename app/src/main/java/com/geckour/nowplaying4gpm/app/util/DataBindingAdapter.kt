package com.geckour.nowplaying4gpm.app.util

import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.databinding.BindingAdapter

@BindingAdapter("app:strResId")
fun setTextByResId(textView: TextView, @StringRes resId: Int) {
    textView.setText(resId)
}

@BindingAdapter("app:visibility")
fun setVisibility(view: View, visible: Boolean) {
    view.visibility = if (visible) View.VISIBLE else View.GONE
}