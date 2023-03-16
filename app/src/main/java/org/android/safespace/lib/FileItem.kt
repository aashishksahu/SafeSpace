package org.android.safespace.lib

class FileItem(
    name: String,
    size: Long,
    isDir: Boolean
) {

    var name: String
    var size: Long
    var isDir: Boolean

    init {
        this.name = name
        this.size = size
        this.isDir = isDir
    }

}