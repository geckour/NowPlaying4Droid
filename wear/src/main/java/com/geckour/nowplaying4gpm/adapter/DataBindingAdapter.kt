package com.geckour.nowplaying4gpm.adapter

import androidx.databinding.BindingAdapter
import android.graphics.Bitmap
import android.widget.ImageView
import com.geckour.nowplaying4gpm.R

@BindingAdapter("app:imageBitmap")
fun loadBitmapString(imageView: ImageView, bitmap: Bitmap?) {
    if (bitmap == null) imageView.setImageResource(R.drawable.ic_placeholder)
    else imageView.setImageBitmap(bitmap)
}