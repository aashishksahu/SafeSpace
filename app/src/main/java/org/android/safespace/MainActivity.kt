package org.android.safespace

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.android.safespace.lib.*
import org.android.safespace.viewmodel.MainActivityViewModel
import java.io.File


/*
 Todo:
  * implement pdf viewer
  *
  * Sort options [Low Priority]
  * Add thumbnails for files [Low Priority]
  * Change icons [Low Priority]
*/

class MainActivity : AppCompatActivity(), ItemClickListener {

    private lateinit var viewModel: MainActivityViewModel
    private lateinit var fileList: List<FileItem>
    private var importList: ArrayList<Uri> = ArrayList()
    private lateinit var filesRecyclerView: RecyclerView
    private lateinit var filesRecyclerViewAdapter: FilesRecyclerViewAdapter
    private lateinit var deleteButton: FloatingActionButton
    private var selectedItems = ArrayList<FileItem>()
    private lateinit var breadCrumbs: TextView
    private val folderNamePattern = Regex("^[a-zA-Z\\d ]*\$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = MainActivityViewModel(application)

        fileList = viewModel.getContents(viewModel.getInternalPath())

        // Notes: created a click listener interface with onClick method, implemented the same in
        //        main activity, then, passed the entire context (this) in the adapter
        //        onItemClickListener, adapter called the method with the row item data

        val adapterMessages = mapOf(
            "directory_indicator" to getString(R.string.directory_indicator)
        )

        filesRecyclerView = findViewById(R.id.filesRecyclerView)
        filesRecyclerViewAdapter = FilesRecyclerViewAdapter(this, adapterMessages)
        filesRecyclerViewAdapter.setData(fileList)
        filesRecyclerView.adapter = filesRecyclerViewAdapter
        filesRecyclerView.layoutManager = LinearLayoutManager(this)

        breadCrumbs = findViewById(R.id.breadCrumbs)
        updateBreadCrumbs()

        deleteButton = findViewById(R.id.deleteButton)
        deleteButton.setOnClickListener {
            // file = null because multiple selections to be deleted and there's no single file
            deleteFilePopup(null, deleteButton.context)
        }

        // File picker result
        val selectFilesActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

                importList.clear()

                if (result.resultCode == RESULT_OK) {

                    val data: Intent? = result.data

                    //If multiple files selected
                    if (data?.clipData != null) {
                        val count = data.clipData?.itemCount ?: 0

                        for (i in 0 until count) {
                            importList.add(data.clipData?.getItemAt(i)?.uri!!)
                        }
                    }

                    //If single file selected
                    else if (data?.data != null) {
                        importList.add(data.data!!)
                    }

                    Toast.makeText(
                        applicationContext,
                        getString(R.string.import_files_progress),
                        Toast.LENGTH_LONG
                    ).show()

                    for (uri in importList) {

                        CoroutineScope(Dispatchers.IO).launch {
                            val importResult = viewModel.importFile(
                                uri,
                                viewModel.getInternalPath()
                            )

                            when (importResult) {
                                // 1: success, -1: failure
                                1 -> {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        filesRecyclerViewAdapter.setData(
                                            viewModel.getContents(
                                                viewModel.getInternalPath()
                                            )
                                        )
                                    }
                                }
                                -1 -> {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        Toast.makeText(
                                            applicationContext,
                                            getString(R.string.import_files_error),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                    }

                }

            }
        // End: File picker result


        // Top App Bar
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.create_dir -> {
                    createDirPopup(topAppBar.context)
                }
                R.id.import_files -> {
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    intent.type = "*/*"
                    selectFilesActivityResult.launch(intent)
                }
                R.id.about -> {
                    // open new intent with MIT Licence and github link and library credits
                }
                R.id.cryptoUtility -> {
                    // open new intent for cryptography
                }
            }
            true
        }
        // End: Top App Bar

        // back button - system navigation
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.isRootDirectory()) {
                    finish()
                }
                backButtonAction()
            }
        })
        //End: back button - system navigation

    }

    @SuppressLint("SetTextI18n")
    private fun updateBreadCrumbs() {
        breadCrumbs.text = "\\ ${viewModel.getInternalPath().replace(File.separator, " \\ ")}"
    }

    private fun createDirPopup(context: Context) {

        val builder = MaterialAlertDialogBuilder(context, R.style.dialogTheme)

        val inflater: LayoutInflater = layoutInflater
        val createFolderLayout = inflater.inflate(R.layout.create_dir, null)
        val folderNameTextView =
            createFolderLayout.findViewById<TextInputLayout>(R.id.createDirTextLayout)

        builder.setTitle(getString(R.string.create_folder))
            .setCancelable(true)
            .setView(createFolderLayout)
            .setPositiveButton(getString(R.string.create)) { _, _ ->

                if (folderNamePattern.containsMatchIn(folderNameTextView.editText?.text.toString())) {

                    if (viewModel.createDir(
                            viewModel.getInternalPath(),
                            folderNameTextView.editText?.text.toString()
                        ) == 1
                    ) {
                        filesRecyclerViewAdapter.setData(
                            viewModel.getContents(
                                viewModel.getInternalPath()
                            )
                        )
                    }
                } else {

                    Toast.makeText(
                        context,
                        getString(R.string.create_folder_invalid_error),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
                // Dismiss the dialog
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    override fun onClick(data: FileItem) {
        // item single tap
        if (data.isDir) {
            viewModel.setInternalPath(data.name)
            filesRecyclerViewAdapter.setData(
                viewModel.getContents(
                    viewModel.getInternalPath()
                )
            )
            // clear selection on directory change and hide delete button
            deleteButton.visibility = View.GONE
            // update breadcrumbs
            updateBreadCrumbs()

            this.selectedItems.clear()

        } else {

            val filePath =
                viewModel.joinPath(filesDir.absolutePath, viewModel.getInternalPath(), data.name)

            when (Utils.getFileType(data.name)) {
                Constants.IMAGE_TYPE -> {
                    loadImage(filePath)
                }
                Constants.VIDEO_TYPE, Constants.AUDIO_TYPE -> {
                    loadAV(filePath)
                }
            }

        }
    }

    override fun onLongClick(data: FileItem, view: View) {
        val popup = PopupMenu(this, view)

        popup.inflate(R.menu.files_context_menu)

        popup.setOnMenuItemClickListener { item: MenuItem? ->

            when (item!!.itemId) {
                R.id.rename_item -> {
                    renameFilePopup(data, view.context)
                }
                R.id.delete_item -> {
                    deleteFilePopup(data, view.context)
                }
                R.id.move_item -> {
                    Toast.makeText(this@MainActivity, item.title, Toast.LENGTH_SHORT).show()
                }
                R.id.copy_item -> {
                    Toast.makeText(this@MainActivity, item.title, Toast.LENGTH_SHORT).show()
                }

            }

            true
        }

        popup.show()
    }

    override fun onItemSelect(
        data: FileItem,
        selectedItems: ArrayList<FileItem>
    ) {
        // Item multi select on icon click
        this.selectedItems = selectedItems
        if (this.selectedItems.isEmpty()) {
            deleteButton.visibility = View.GONE
        } else {
            deleteButton.visibility = View.VISIBLE
        }
    }

    private fun backButtonAction() {
        if (!viewModel.isRootDirectory()) {
            // remove the current directory
            viewModel.setPreviousPath()

            // display contents of the navigated path
            filesRecyclerViewAdapter.setData(
                viewModel.getContents(
                    viewModel.getInternalPath()
                )
            )
        }
        updateBreadCrumbs()

    }


    private fun renameFilePopup(file: FileItem, context: Context) {
        val builder = MaterialAlertDialogBuilder(context, R.style.dialogTheme)

        val inflater: LayoutInflater = layoutInflater
        val renameLayout = inflater.inflate(R.layout.rename_layout, null)
        val folderNameTextView =
            renameLayout.findViewById<TextInputLayout>(R.id.renameTextLayout)

        builder.setTitle(getString(R.string.context_menu_rename))
            .setCancelable(true)
            .setView(renameLayout)
            .setPositiveButton(getString(R.string.context_menu_rename)) { _, _ ->

                if (folderNamePattern.containsMatchIn(folderNameTextView.editText?.text.toString())) {
                    val result = viewModel.renameFile(
                        file,
                        viewModel.getInternalPath(),
                        folderNameTextView.editText!!.text.toString()
                    )

                    if (result == 0) {
                        Toast.makeText(
                            context,
                            getString(R.string.generic_error),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        filesRecyclerViewAdapter.setData(
                            viewModel.getContents(
                                viewModel.getInternalPath()
                            )
                        )
                    }
                } else {
                    Toast.makeText(
                        context,
                        getString(R.string.create_folder_invalid_error),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
                // Dismiss the dialog
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun deleteFilePopup(file: FileItem?, context: Context) {

        val inflater: LayoutInflater = layoutInflater
        val deleteLayout = inflater.inflate(R.layout.delete_confirmation, null)

        val builder = MaterialAlertDialogBuilder(context, R.style.dialogTheme)
        builder.setTitle(getString(R.string.context_menu_delete))
            .setCancelable(true)
            .setView(deleteLayout)
            .setPositiveButton(getString(R.string.context_menu_delete)) { _, _ ->

                if (this.selectedItems.isNotEmpty() && file == null) {
                    for (item in this.selectedItems) {
                        viewModel.deleteFile(
                            item,
                            viewModel.getInternalPath()
                        )
                    }

                    // hide delete button after items are deleted
                    deleteButton.visibility = View.GONE

                    filesRecyclerViewAdapter.setData(
                        viewModel.getContents(
                            viewModel.getInternalPath()
                        )
                    )
                } else {

                    val result = viewModel.deleteFile(
                        file!!,
                        viewModel.getInternalPath()
                    )

                    if (result == 0) {
                        Toast.makeText(
                            context,
                            getString(R.string.generic_error),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        filesRecyclerViewAdapter.setData(
                            viewModel.getContents(
                                viewModel.getInternalPath()
                            )
                        )
                    }
                }
            }
            .setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
                // Dismiss the dialog
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun loadImage(filePath: String) {
        val imageViewIntent = Intent(this, ImageView::class.java)
        imageViewIntent.putExtra(Constants.INTENT_KEY_PATH, filePath)
        startActivity(imageViewIntent)
    }

    private fun loadAV(filePath: String) {
        val mediaViewIntent = Intent(this, MediaView::class.java)
        mediaViewIntent.putExtra(Constants.INTENT_KEY_PATH, filePath)
        startActivity(mediaViewIntent)
    }

}