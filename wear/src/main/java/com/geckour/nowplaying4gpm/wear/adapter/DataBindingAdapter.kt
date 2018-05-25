package com.geckour.nowplaying4gpm.wear.adapter

import android.databinding.BindingAdapter
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.ImageView
import com.geckour.nowplaying4gpm.wear.R

@BindingAdapter("app:imageBitmap")
fun loadBitmapString(imageView: ImageView, bitmapString: String?) {
    if (bitmapString == null) imageView.setImageResource(R.drawable.ic_placeholder)
    else imageView.setImageBitmap(Base64.decode(bitmapString, Base64.DEFAULT).let { BitmapFactory.decodeByteArray(it, 0, it.size) })
}