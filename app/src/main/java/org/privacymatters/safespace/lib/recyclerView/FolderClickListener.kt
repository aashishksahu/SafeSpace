package org.privacymatters.safespace.lib.recyclerView

import android.view.View
import org.privacymatters.safespace.lib.fileManager.FolderItem

interface FolderClickListener {
    fun onFolderSelect(folderItem: FolderItem)
    fun onFolderLongPress(folderItem: FolderItem, view: View)

}