package org.privacymatters.safespace.lib.fileManager

class FolderItem(
    name: String,
    itemCount: Int,
) {
    var name: String
    var itemCount: Int

    init {
        this.name = name
        this.itemCount = itemCount
    }
}