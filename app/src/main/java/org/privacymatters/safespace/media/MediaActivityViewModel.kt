package org.privacymatters.safespace.media

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import org.privacymatters.safespace.experimental.main.DataManager
import org.privacymatters.safespace.utils.Utils
import org.privacymatters.safespace.utils.Constants

class MediaActivityViewModel(application: Application) : AndroidViewModel(application) {

    private var fileSortBy = Constants.NAME
    private var fileSortOrder = Constants.ASC
    private var ops = DataManager

    private val sharedPref: SharedPreferences =
        application.getSharedPreferences(Constants.SHARED_PREF_FILE, Context.MODE_PRIVATE)


    var mediaList: ArrayList<String> = ArrayList()

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

        val itemList = ops.baseItemList.filter { item ->
            Utils.getFileType(item.name) in listOf(
                Constants.IMAGE_TYPE,
                Constants.VIDEO_TYPE,
                Constants.AUDIO_TYPE
            )
        }

        itemList.forEachIndexed { _, mediaItem ->
            mediaList.add(ops.joinPath(ops.getInternalPath(), mediaItem.name))
        }

    }

    fun getPosition(): Int {
        // return the index of the item selected in main activity
        return ops.positionHistory.intValue
    }

    fun setPosition(pos: Int) {
        // set the index of currently open item in MediaView
        // Todo: return the actual index from the complete list
        ops.positionHistory.intValue = pos
    }

}