package org.privacymatters.safespace.lib

import android.content.SharedPreferences
import android.view.View
import android.widget.RadioGroup
import org.privacymatters.safespace.R

class Sort {

    companion object {

        private var initDone = false
        private var folderSort: String? = null
        private var fileSortBy: String? = null
        private var fileSortOrder: String? = null
        private var ops: Operations? = null

        fun init(sharedPref: SharedPreferences, sortButtonView: View, ops: Operations) {

            this.ops = ops

            folderSort = sharedPref.getString(Constants.FOLDER_SORT, "") // Folder - Ascending or Descending
            fileSortBy = sharedPref.getString(Constants.SORT_TYPE, "") // Name, Date or Size
            fileSortOrder =
                sharedPref.getString(Constants.SORT_ORDER, "") // Ascending or Descending

            val sortFolder = sortButtonView.findViewById<RadioGroup>(R.id.sortFoldersBy)
            val sortGroupBy = sortButtonView.findViewById<RadioGroup>(R.id.sortGroupBy)
            val sortGroupOrder = sortButtonView.findViewById<RadioGroup>(R.id.sortGroupOrder)

            when (folderSort) {
                "", Constants.ASC -> sortFolder.check(R.id.sortAsc)
                Constants.DESC -> sortFolder.check(R.id.sortDesc)
            }

            when (fileSortBy) {
                "", Constants.NAME -> sortGroupBy.check(R.id.sortByName)
                Constants.SIZE -> sortGroupBy.check(R.id.sortBySize)
                Constants.DATE -> sortGroupBy.check(R.id.sortByDate)
            }

            when (fileSortOrder) {
                "", Constants.ASC -> sortGroupBy.check(R.id.sortAsc)
                Constants.DESC -> sortGroupBy.check(R.id.sortDesc)
            }

            sortGroupBy.setOnCheckedChangeListener { _, checkedId ->
                fileSortBy = when (checkedId) {
                    R.id.sortByName -> Constants.NAME
                    R.id.sortBySize -> Constants.SIZE
                    R.id.sortByDate -> Constants.DATE
                    else -> Constants.NAME
                }

            }

            sortGroupOrder.setOnCheckedChangeListener { _, checkedId ->
                fileSortOrder = when (checkedId) {
                    R.id.sortAsc -> Constants.ASC
                    R.id.sortDesc -> Constants.DESC
                    else -> Constants.ASC
                }
            }

            sortFolder.setOnCheckedChangeListener { _, checkedId ->
                folderSort = when (checkedId) {
                    R.id.sortAsc -> Constants.ASC
                    R.id.sortDesc -> Constants.DESC
                    else -> Constants.ASC
                }
            }

            initDone = true

        }

        fun sortFiles(files: List<FileItem>): List<FileItem> {


            // Ascending or descending
            when (fileSortOrder) {
                Constants.ASC -> {
                    // name, date or size
                    when (fileSortBy) {
                        Constants.NAME -> files.sortedBy { it.name }
                        Constants.SIZE -> files.sortedBy { it.size }
                        Constants.DATE -> files.sortedBy { it.lastModified }
                    }
                }

                Constants.DESC -> {
                    // name, date or size
                    when (fileSortBy) {
                        Constants.NAME -> files.sortedByDescending { it.name }
                        Constants.SIZE -> files.sortedByDescending { it.size }
                        Constants.DATE -> files.sortedByDescending { it.lastModified }
                    }
                }
            }


            return files
        }

        fun sortFolders(folders: List<FolderItem>): List<FolderItem> {

            // Ascending or descending
            when (folderSort) {
                Constants.ASC -> folders.sortedBy { it.name }
                Constants.DESC -> folders.sortedByDescending { it.name }
            }

            return folders
        }

    }

}
