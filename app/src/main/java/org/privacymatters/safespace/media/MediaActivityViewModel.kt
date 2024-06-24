package org.privacymatters.safespace.media

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import org.privacymatters.safespace.experimental.main.DataManager
import org.privacymatters.safespace.experimental.main.Item
import org.privacymatters.safespace.utils.Utils
import org.privacymatters.safespace.utils.Constants

class MediaActivityViewModel(application: Application) : AndroidViewModel(application) {

    private var fileSortBy = Constants.NAME
    private var fileSortOrder = Constants.ASC
    var ops = DataManager

    private val sharedPref: SharedPreferences =
        application.getSharedPreferences(Constants.SHARED_PREF_FILE, Context.MODE_PRIVATE)

    var currentPosition = 0
    var mediaList: List<Item> = ArrayList()

    init {
        // Name, Date or Size
        fileSortBy = sharedPref.getString(Constants.FILE_SORT_BY, Constants.NAME)!!

        // Ascending or Descending
        fileSortOrder = sharedPref.getString(Constants.FILE_SORT_ORDER, Constants.ASC)!!

        getItems()

    }

    private fun getItems() {
//        if (ops.itemStateList.isEmpty()) {
        if (ops.itemListFlow.value.isEmpty()) {
            ops.getSortedItems(fileSortBy, fileSortOrder)
        }

//        mediaList = ops.itemStateList
        mediaList = ops.itemListFlow.value
            .filter { item ->
                Utils.getFileType(item.name) in listOf(
                    Constants.IMAGE_TYPE,
                    Constants.VIDEO_TYPE,
                    Constants.AUDIO_TYPE
                )
            }

//        currentPosition = mediaList.indexOf(ops.itemStateList.find { ops.openedItem == it })
        currentPosition = mediaList.indexOf(ops.itemListFlow.value.find { ops.openedItem == it })

//        itemList.forEachIndexed { _, mediaItem ->
//            mediaList.add(ops.joinPath(ops.getInternalPath(), mediaItem.name))
//        }

    }

    fun setPosition(pos: Int) {
//        ops.positionHistory.intValue = ops.itemStateList.indexOf(mediaList[pos])
        ops.positionHistory.intValue = ops.itemListFlow.value.indexOf(mediaList[pos])
    }

}