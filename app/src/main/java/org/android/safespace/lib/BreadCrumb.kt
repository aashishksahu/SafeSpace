package org.android.safespace.lib

import android.widget.Button

class BreadCrumb(
    path: String,
    pathBtn: Button
) {
    val path: String
    val pathBtn: Button

    init {
        this.path = path
        this.pathBtn = pathBtn
    }


}