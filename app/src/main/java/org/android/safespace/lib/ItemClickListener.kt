package org.android.safespace.lib

import android.view.View

interface ItemClickListener {
    fun onClick(data: FileItem)
    fun onLongClick(data: FileItem, view: View)
}