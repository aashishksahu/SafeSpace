package org.privacymatters.safespace.lib.fileManager

import android.content.SharedPreferences
import android.view.View
import android.widget.RadioGroup
import org.privacymatters.safespace.R
import org.privacymatters.safespace.lib.utils.Constants

class Sortinator(sharedPref: SharedPreferences, ops: Operations) {

    private var fileSortBy: String? = null
    private var fileSortOrder: String? = null
    private var ops: Operations? = null
    private var sortByGroup: RadioGroup? = null
    private var sortOrderGroup: RadioGroup? = null
    private var sharedPref: SharedPreferences

    init {

        this.ops = ops
        this.sharedPref = sharedPref

        fileSortBy = sharedPref.getString(Constants.FILE_SORT_BY, "") // Name, Date or Size
        fileSortOrder =
            sharedPref.getString(Constants.FILE_SORT_ORDER, "") // Ascending or Descending

    }

    fun registerListeners(sortLayout: View) {
        //used in selector methods to get the checked options
        sortByGroup = sortLayout.findViewById(R.id.sortGroupBy)
        sortOrderGroup = sortLayout.findViewById(R.id.sortGroupOrder)
    }

    fun sortFiles(files: List<FileItem>): List<FileItem> {

        // get selected radio buttons if the user doesn't change selection
        fileSortBySelector(sortByGroup?.checkedRadioButtonId)
        fileSortOrderSelector(sortOrderGroup?.checkedRadioButtonId)

        // Ascending or descending
        when (fileSortOrder) {
            Constants.ASC -> {
                // name, date or size
                return when (fileSortBy) {
                    Constants.NAME -> files.sortedBy { it.name }
                    Constants.SIZE -> files.sortedBy { it.size }
                    Constants.DATE -> files.sortedBy { it.lastModified }
                    else -> files.sortedByDescending { it.name }
                }
            }

            Constants.DESC -> {
                // name, date or size
                return when (fileSortBy) {
                    Constants.NAME -> files.sortedByDescending { it.name }
                    Constants.SIZE -> files.sortedByDescending { it.size }
                    Constants.DATE -> files.sortedByDescending { it.lastModified }
                    else -> files.sortedByDescending { it.name }
                }
            }
        }


        return files
    }

    private fun fileSortBySelector(checkedId: Int?) {
        if (checkedId != null) {
            fileSortBy = when (checkedId) {
                R.id.sortByName -> Constants.NAME
                R.id.sortBySize -> Constants.SIZE
                R.id.sortByDate -> Constants.DATE
                else -> Constants.NAME
            }

            val editor = sharedPref.edit()
            editor?.putString(Constants.FILE_SORT_BY, fileSortBy)
            editor?.apply()
        }
    }

    private fun fileSortOrderSelector(checkedId: Int?) {
        if (checkedId != null) {
            fileSortOrder = when (checkedId) {
                R.id.sortAsc -> Constants.ASC
                R.id.sortDesc -> Constants.DESC
                else -> Constants.ASC
            }

            val editor = sharedPref.edit()
            editor?.putString(Constants.FILE_SORT_ORDER, fileSortOrder)
            editor?.apply()
        }
    }

}
