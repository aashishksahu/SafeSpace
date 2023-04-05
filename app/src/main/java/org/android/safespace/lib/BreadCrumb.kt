package org.android.safespace.lib

import com.google.android.material.button.MaterialButton

class BreadCrumb(
    path: String,
    pathBtn: MaterialButton
) {
    val path: String
    val pathBtn: MaterialButton

    init {
        this.path = path
        this.pathBtn = pathBtn
    }


}