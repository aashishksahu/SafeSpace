package org.privacymatters.safespace.experimental.mainn

import android.graphics.Bitmap

data class Item(
    val icon: Bitmap?,
    val iconDrawable: Int,
    val name: String,
    val size: String,
    val isDir: Boolean,
    val itemCount: String,
    val lastModified: String,
    val isSelected: Boolean,
)
