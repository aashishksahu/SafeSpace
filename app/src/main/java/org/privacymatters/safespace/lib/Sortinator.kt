package org.privacymatters.safespace.lib

import android.content.SharedPreferences
import android.view.View
import android.widget.RadioGroup
import org.privacymatters.safespace.R

class Sortinator(sharedPref: SharedPreferences, ops: Operations) {

    private var folderSort: String? = null
    private var fileSortBy: String? = null
    private var fileSortOrder: String? = null
    private var ops: Operations? = null
    private var sortFolderGroup: RadioGroup? = null
    private var sortByGroup: RadioGroup? = null
    private var sortOrderGroup: RadioGroup? = null
    private var sharedPref: SharedPreferences

    init {

        this.ops = ops
        this.sharedPref = sharedPref

        folderSort =
            sharedPref.getString(Constants.FOLDER_SORT, "") // Folder - Ascending or Descending
        fileSortBy = sharedPref.getString(Constants.FILE_SORT_BY, "") // Name, Date or Size
        fileSortOrder =
            sharedPref.getString(Constants.FILE_SORT_ORDER, "") // Ascending or Descending

    }

    fun registerListeners(sortLayout: View) {
        sortFolderGroup = sortLayout.findViewById(R.id.sortFoldersBy)
        sortByGroup = sortLayout.findViewById(R.id.sortGroupBy)
        sortOrderGroup = sortLayout.findViewById(R.id.sortGroupOrder)

        sortByGroup?.setOnCheckedChangeListener { _, checkedId ->
            fileSortBySelector(checkedId)
        }

        sortOrderGroup?.setOnCheckedChangeListener { _, checkedId ->
            fileSortOrderSelector(checkedId)
        }

        sortFolderGroup?.setOnCheckedChangeListener { _, checkedId ->
            folderSortSelector(checkedId)
        }
    }

    fun sortFiles(files: List<FileItem>): List<FileItem> {

        // get selected radio buttons if the user doesn't change selection
        fileSortBySelector(sortOrderGroup?.checkedRadioButtonId)
        fileSortOrderSelector(sortByGroup?.checkedRadioButtonId)

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

    fun sortFolders(folders: List<FolderItem>): List<FolderItem> {

        // get selected radio buttons if the user doesn't change selection
        folderSortSelector(sortFolderGroup?.checkedRadioButtonId)

        // Ascending or descending
        return when (folderSort) {
            Constants.ASC -> folders.sortedBy { it.name }
            Constants.DESC -> folders.sortedByDescending { it.name }
            else -> folders.sortedBy { it.name }
        }

    }

    private fun fileSortBySelector(checkedId: Int?) {
        if (checkedId != null){
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

    private fun folderSortSelector(checkedId: Int?) {
        if (checkedId != null) {
            folderSort = when (checkedId) {
                R.id.sortFolderAsc -> Constants.ASC
                R.id.sortFolderDesc -> Constants.DESC
                else -> Constants.ASC
            }

            val editor = sharedPref.edit()
            editor?.putString(Constants.FOLDER_SORT, folderSort)
            editor?.apply()
        }
    }
}
//Todo: debug sorting, check the flow and correct it
