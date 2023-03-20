package org.android.safespace

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.marginStart
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import org.android.safespace.lib.FileItem
import org.android.safespace.lib.FileUtils
import org.android.safespace.lib.FilesRecyclerViewAdapter
import org.android.safespace.lib.ItemClickListener
import org.android.safespace.viewmodel.MainActivityViewModel

/*
 Todo:
  * Add recycler view navigation slide animation
  * Long press context menu on file item
  * Sort options [Low Priority]
  * Change icons [Low Priority]
  * Add thumbnails for files [Low Priority]
*/

class MainActivity : AppCompatActivity(), ItemClickListener {

    private lateinit var viewModel: MainActivityViewModel
    private lateinit var fileUtils: FileUtils
    private lateinit var fileList: List<FileItem>
    private lateinit var filesRecyclerView: RecyclerView
    private lateinit var filesRecyclerViewAdapter: FilesRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = MainActivityViewModel(application)

        fileUtils = FileUtils(applicationContext)

        fileList = fileUtils.getContents(applicationContext, viewModel.getInternalPath())

        // Notes: created a click listener interface with onClick method, implemented the same in
        //        main activity, then, passed the entire context (this) in the adapter
        //        onItemClickListener, adapter called the method with the row item data

        filesRecyclerView = findViewById(R.id.filesRecyclerView)
        filesRecyclerViewAdapter = FilesRecyclerViewAdapter(this)
        filesRecyclerViewAdapter.setData(fileList)
        filesRecyclerView.adapter = filesRecyclerViewAdapter
        filesRecyclerView.layoutManager = LinearLayoutManager(this)


        // File picker result
        val selectFilesActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

                if (result.resultCode == Activity.RESULT_OK) {

                    val data: Intent? = result.data
                    var importSuccess = true

                    //If multiple files selected
                    if (data?.clipData != null) {
                        val count = data.clipData?.itemCount ?: 0

                        for (i in 0 until count) {
                            if (fileUtils.importFile(
                                    data.clipData?.getItemAt(i)?.uri!!,
                                    viewModel.getInternalPath()
                                ) != 1
                            ) // 1: success, -1: failure
                            {
                                importSuccess = false
                            }
                        }
                    }

                    //If single file selected
                    else if (data?.data != null) {
                        importSuccess = fileUtils.importFile(
                            data.data!!,
                            viewModel.getInternalPath()
                        ) == 1 // 1: success, -1: failure

                    }

                    when (importSuccess) {
                        true -> {
                            filesRecyclerViewAdapter.setData(
                                fileUtils.getContents(
                                    applicationContext,
                                    viewModel.getInternalPath()
                                )
                            )
                        }
                        else -> {
                            Toast.makeText(
                                applicationContext,
                                getString(R.string.import_files_error),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }

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
            }
            true
        }

        // back button on screen
        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            backButtonAction()
        }

        // back button - system navigation
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                backButtonAction()
            }
        })


        // about button
        val aboutBtn = findViewById<Button>(R.id.about)
        aboutBtn.setOnClickListener {
            // open new intent with MIT Licence and github link and library credits
        }
    }

    private fun createDirPopup(context: Context) {

        val folderNamePattern = Regex("^[a-zA-Z\\d ]*\$")

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

                    if (fileUtils.createDir(
                            viewModel.getInternalPath(),
                            folderNameTextView.editText?.text.toString()
                        ) == 1
                    ) {
                        filesRecyclerViewAdapter.setData(
                            fileUtils.getContents(
                                applicationContext,
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
        if (data.isDir) {
            viewModel.setInternalPath(data.name)
            filesRecyclerViewAdapter.setData(
                fileUtils.getContents(
                    applicationContext,
                    viewModel.getInternalPath()
                )
            )
        } else {
            // ToDo: Open file in respective app
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

            filesRecyclerViewAdapter.setData(
                fileUtils.getContents(
                    view.context,
                    viewModel.getInternalPath()
                )
            )

            true
        }

        popup.show()
    }

    private fun backButtonAction() {
        if (!viewModel.isRootDirectory()) {
            viewModel.setPreviousPath()
            filesRecyclerViewAdapter.setData(
                fileUtils.getContents(
                    applicationContext,
                    viewModel.getInternalPath()
                )
            )
        }
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

                val result = fileUtils.renameFile(
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
                }
            }
            .setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
                // Dismiss the dialog
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun deleteFilePopup(file: FileItem, context: Context) {

        // Todo: create layout for delete popup with textview

        val builder = MaterialAlertDialogBuilder(context, R.style.dialogTheme)
        builder.setTitle(getString(R.string.context_menu_delete))
            .setCancelable(true)
            .setPositiveButton(getString(R.string.context_menu_delete)) { _, _ ->

                val result = fileUtils.deleteFile(
                    file,
                    viewModel.getInternalPath()
                )

                if (result == 0) {
                    Toast.makeText(
                        context,
                        getString(R.string.generic_error),
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

}