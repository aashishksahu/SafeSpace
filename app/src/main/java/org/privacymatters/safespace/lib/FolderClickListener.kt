package org.privacymatters.safespace.lib

import android.view.View

interface FolderClickListener {
    fun onFolderSelect(folderItem: FolderItem)
    fun onFolderLongPress(folderItem: FolderItem, view: View)

}