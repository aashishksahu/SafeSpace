package org.privacymatters.safespace.experimental.mainn

data class Item(
    val name: String,
    val size: String,
    val isDir: Boolean,
    val itemCount: String,
    val lastModified: String,
    val isSelected: Boolean,
)
