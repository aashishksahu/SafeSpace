package org.android.safespace.lib

class FileItem(
    name: String,
    size: Long,
    isDir: Boolean,
    lastModified: Long
) {

    var name: String
    var size: Long
    var isDir: Boolean
    var lastModified: Long

    init {
        this.name = name
        this.size = size
        this.isDir = isDir
        this.lastModified = lastModified
    }

}