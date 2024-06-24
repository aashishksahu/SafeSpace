package org.privacymatters.safespace.experimental.main

import java.util.UUID

data class Item(
    val id : UUID,
    val name: String,
    val size: String,
    val isDir: Boolean,
    val itemCount: String,
    val lastModified: String,
    val isSelected: Boolean,
)
