package org.privacymatters.safespace.experimental.media

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.privacymatters.safespace.experimental.main.DataManager
import org.privacymatters.safespace.experimental.main.Item
import org.privacymatters.safespace.utils.Constants
import org.privacymatters.safespace.utils.Utils
import java.io.File

class MediaActivityViewModel(private val application: Application) : AndroidViewModel(application) {

    private val ops = DataManager

    var openedItemPosition: Int

    private val originalList = ops.itemListFlow.value

    val itemList: List<Item> = originalList.filter { item ->
        Utils.getFileType(item.name) in listOf(
            Constants.IMAGE_TYPE,
            Constants.VIDEO_TYPE,
            Constants.AUDIO_TYPE
        )
    }

    init {
        openedItemPosition = itemList.indexOf(ops.openedItem)
    }

    fun getFilePath(pos: Int): String {
        return File(
            ops.joinPath(
                ops.getInternalPath(),
                itemList[pos].name
            )
        ).canonicalPath
    }

    fun setPositionHistory() {
        ops.positionHistory.intValue = openedItemPosition
    }
}