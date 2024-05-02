package org.privacymatters.safespace.experimental.mainn

data class Item(
    val name: String,
    val size: Long,
    val isDir: Boolean,
    val itemCount: Int,
    val lastModified: Long
)
