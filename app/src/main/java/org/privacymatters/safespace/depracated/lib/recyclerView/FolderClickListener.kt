package org.privacymatters.safespace.depracated.lib.recyclerView

import android.view.View
import org.privacymatters.safespace.depracated.lib.fileManager.FolderItem

interface FolderClickListener {
    fun onFolderSelect(folderItem: FolderItem)
    fun onFolderLongPress(folderItem: FolderItem, view: View)

}