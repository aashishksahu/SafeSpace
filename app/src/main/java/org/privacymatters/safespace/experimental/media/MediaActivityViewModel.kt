package org.privacymatters.safespace.experimental.media

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import org.privacymatters.safespace.experimental.mainn.DataManager
import org.privacymatters.safespace.experimental.mainn.Item
import org.privacymatters.safespace.lib.utils.Constants

class MediaActivityViewModel(application: Application) : AndroidViewModel(application) {

    private var fileSortBy = Constants.NAME
    private var fileSortOrder = Constants.ASC
    private val sharedPref: SharedPreferences =
        application.getSharedPreferences(Constants.SHARED_PREF_FILE, Context.MODE_PRIVATE)
    var currentPosition = 0
    private var ops = DataManager
    var itemList: List<Item> = ops.baseItemList

    init {
        // Name, Date or Size
        fileSortBy = sharedPref.getString(Constants.FILE_SORT_BY, Constants.NAME)!!

        // Ascending or Descending
        fileSortOrder = sharedPref.getString(Constants.FILE_SORT_ORDER, Constants.ASC)!!

        getItems()
    }

    private fun getItems() {
        if (ops.baseItemList.isEmpty()) {
            ops.getSortedItems(fileSortBy, fileSortOrder)
        }
        ops.itemStateList.clear()
        ops.itemStateList.addAll(ops.baseItemList)
    }

    fun getPath(fileName: String): String {
        return ops.joinPath(ops.getInternalPath(), fileName)
    }

}