package org.privacymatters.safespace.experimental.main

data class Item(
    val name: String,
    val size: String,
    val isDir: Boolean,
    val itemCount: String,
    val lastModified: String,
    var isSelected: Boolean,
)
