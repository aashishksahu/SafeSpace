package org.privacymatters.safespace.depracated.lib.recyclerView

import android.view.View
import org.privacymatters.safespace.depracated.lib.fileManager.FileItem

interface ItemClickListener {
    fun onClick(data: FileItem)
    fun onLongClick(data: FileItem, view: View)
    fun onItemSelect(data: FileItem, selectedItems: ArrayList<FileItem>)
}