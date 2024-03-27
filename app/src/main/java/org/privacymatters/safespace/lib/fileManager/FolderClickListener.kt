package org.privacymatters.safespace.lib.fileManager

import android.view.View

interface FolderClickListener {
    fun onFolderSelect(folderItem: FolderItem)
    fun onFolderLongPress(folderItem: FolderItem, view: View)

}