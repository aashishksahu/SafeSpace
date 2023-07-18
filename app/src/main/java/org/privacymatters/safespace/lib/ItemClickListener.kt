package org.privacymatters.safespace.lib

import android.view.View

interface ItemClickListener {
    fun onClick(data: FileItem)
    fun onLongClick(data: FileItem, view: View)
    fun onItemSelect(data: FileItem, selectedItems: ArrayList<FileItem>)
}