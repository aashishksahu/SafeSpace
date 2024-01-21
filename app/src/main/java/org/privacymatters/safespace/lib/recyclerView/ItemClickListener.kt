package org.privacymatters.safespace.lib.recyclerView

import android.view.View
import org.privacymatters.safespace.lib.fileManager.FileItem

interface ItemClickListener {
    fun onClick(data: FileItem)
    fun onLongClick(data: FileItem, view: View)
    fun onItemSelect(data: FileItem, selectedItems: ArrayList<FileItem>)
}