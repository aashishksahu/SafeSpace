package org.privacymatters.safespace.main

import java.util.UUID

data class Item(
    val id : UUID,
    val name: String,
    val size: Long,
    val isDir: Boolean,
    val itemCount: String,
    val lastModified: Long,
    val isSelected: Boolean,
)
