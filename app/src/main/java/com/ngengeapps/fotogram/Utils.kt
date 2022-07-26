package com.ngengeapps.fotogram

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.databinding.BindingAdapter

@BindingAdapter("app:image")
fun bindImageView(imageView: ImageView, bitmap: Bitmap?) {
    bitmap?.let {
        imageView.setImageBitmap(it)
    }
}