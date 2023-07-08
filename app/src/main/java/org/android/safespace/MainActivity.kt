package org.android.safespace

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.android.safespace.lib.Constants
import org.android.safespace.lib.FileItem
import org.android.safespace.lib.FilesRecyclerViewAdapter
import org.android.safespace.lib.FolderClickListener
import org.android.safespace.lib.FolderItem
import org.android.safespace.lib.FolderRecyclerViewAdapter
import org.android.safespace.lib.ItemClickListener
import org.android.safespace.lib.Operations
import org.android.safespace.lib.Utils


/*
 Todo:
  * Implement about page
  *
  * Sort options [Low Priority]
*/

class MainActivity : AppCompatActivity(), ItemClickListener, FolderClickListener {

    private lateinit var ops: Operations

    private lateinit var nothingHereText: TextView
    private var importList: ArrayList<Uri> = ArrayList()
    private lateinit var filesRecyclerView: RecyclerView
    private lateinit var filesRecyclerViewAdapter: FilesRecyclerViewAdapter
    private lateinit var folderRecyclerView: RecyclerView
    private lateinit var folderRecyclerViewAdapter: FolderRecyclerViewAdapter
    private lateinit var deleteButton: MaterialButton
    private lateinit var clearButton: MaterialButton
    private lateinit var exportButton: MaterialButton
    private var selectedItems = ArrayList<FileItem>()
    private val folderNamePattern = Regex("^[a-zA-Z\\d ]*\$")
    private lateinit var fileMoveCopyView: ConstraintLayout
    private lateinit var fileMoveCopyName: TextView
    private lateinit var fileMoveCopyOperation: TextView
    private lateinit var fileMoveCopyButton: MaterialButton
    private lateinit var sharedPref: SharedPreferences
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var selectExportDirActivityResult: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initialize things on activity start

        ops = Operations(application)
        sharedPref = getPreferences(MODE_PRIVATE)
        nothingHereText = findViewById(R.id.nothingHere) // show this when recycler view is empty

        val filesRVAdapterTexts = mapOf(
            "directory_indicator" to getString(R.string.directory_indicator)
        )

        val folderRVAdapterTexts = mapOf(
            "items" to getString(R.string.items),
            "item" to getString(R.string.item)
        )

        filesRecyclerView = findViewById(R.id.filesRecyclerView)
        filesRecyclerViewAdapter = FilesRecyclerViewAdapter(this, filesRVAdapterTexts, ops)

        folderRecyclerView = findViewById(R.id.folderRecyclerView)
        folderRecyclerViewAdapter = FolderRecyclerViewAdapter(this, folderRVAdapterTexts)

        deleteButton = findViewById(R.id.deleteButton)
        clearButton = findViewById(R.id.clearButton)
        exportButton = findViewById(R.id.exportButton)
        fileMoveCopyView = findViewById(R.id.moveCopyFileView)
        fileMoveCopyName = findViewById(R.id.moveCopyFileName)
        fileMoveCopyOperation = findViewById(R.id.moveCopyFileOperation)
        fileMoveCopyButton = findViewById(R.id.moveCopyFileButton)
        val fileMoveCopyButtonCancel: MaterialButton = findViewById(R.id.moveCopyFileButtonCancel)
        topAppBar = findViewById(R.id.topAppBar)

        // initialize at first run of app. Sets the root directory
        if (!sharedPref.getBoolean(Constants.APP_FIRST_RUN, false)) {
            if (initializeApp() == 1) {
                with(sharedPref.edit()) {
                    putBoolean(Constants.APP_FIRST_RUN, true)
                    apply()
                }
            }
        }

        val (fileList, folderList) = ops.getContents(ops.getInternalPath())

        folderRecyclerViewAdapter.setData(folderList)
        val horizontalLayoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        folderRecyclerView.layoutManager = horizontalLayoutManager
        folderRecyclerView.adapter = folderRecyclerViewAdapter

        filesRecyclerViewAdapter.setData(fileList, nothingHereText)
        filesRecyclerView.layoutManager = LinearLayoutManager(this)
        filesRecyclerView.adapter = filesRecyclerViewAdapter

        deleteButton.setOnClickListener {
            // file = null because multiple selections to be deleted and there's no single file
            deleteFilePopup(null, deleteButton.context)
        }

        clearButton.setOnClickListener {
            clearSelection()
        }

        exportButton.setOnClickListener {
            exportItems()
        }

        fileMoveCopyView.visibility = View.GONE
        fileMoveCopyName.isSelected = true

        fileMoveCopyButton.setOnClickListener {

            val fileName = fileMoveCopyName.text.toString()

            ops.moveFileTo = ops.joinPath(
                ops.getFilesDir(),
                ops.getInternalPath(),
                fileName
            )

            if (ops.moveFileFrom != null && ops.moveFileFrom != ops.moveFileTo) {
                val status = if (fileMoveCopyButton.text == getString(R.string.move_file_title)) {
                    ops.moveFile()
                } else {
                    ops.copyFile()
                }

                if (status == -1) {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.move_copy_file_failure),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.move_copy_file_success),
                        Toast.LENGTH_LONG
                    ).show()

                    updateRecyclerView()
                }
            }
            fileMoveCopyView.visibility = View.GONE

        }

        fileMoveCopyButtonCancel.setOnClickListener {
            fileMoveCopyView.visibility = View.GONE
            ops.moveFileFrom = null
            ops.moveFileTo = null
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
                            val importResult = ops.importFile(
                                uri,
                                ops.getInternalPath()
                            )

                            when (importResult) {
                                // 1: success, -1: failure
                                1 -> {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        updateRecyclerView()
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

        // Start: Directory picker result
        selectExportDirActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

                if (result.resultCode == RESULT_OK) {
                    result.data.also { intent ->
                        val uri = intent?.data
                        if (uri != null) {

                            Toast.makeText(
                                exportButton.context,
                                getString(R.string.export_in_progress),
                                Toast.LENGTH_LONG
                            ).show()

                            for (item in selectedItems) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    ops.exportItems(uri, item)
                                }
                            }
                            clearSelection()
                        }
                    }
                }

            }
        // End: Directory picker result


        // Top App Bar
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

                R.id.create_txt -> {
                    createTextNote(topAppBar.context)
                }

                R.id.about -> {
                    val intent = Intent(this, AboutActivity::class.java)
                    startActivity(intent)
                }
            }
            true
        }
        // End: Top App Bar

        // back button - system navigation
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backButtonAction()
            }
        })
        //End: back button - system navigation

    }

    private fun clearSelection() {
        toggleFloatingButtonVisibility(false)
        this.selectedItems.clear()
        updateRecyclerView()
    }

    private fun initializeApp(): Int {
        return ops.initRootDir()
    }

    private fun updateRecyclerView() {
        // display contents of the navigated path
        val (files, folders) = ops.getContents(ops.getInternalPath())

        filesRecyclerViewAdapter.setData(
            files,
            nothingHereText
        )

        folderRecyclerViewAdapter.setData(folders)
    }

    private fun createDirPopup(context: Context) {

        val builder = MaterialAlertDialogBuilder(context)

        val inflater: LayoutInflater = layoutInflater
        val createFolderLayout = inflater.inflate(R.layout.create_dir, null)
        val folderNameTextView =
            createFolderLayout.findViewById<TextInputLayout>(R.id.createDirTextLayout)

        builder.setTitle(getString(R.string.create_folder))
            .setCancelable(true)
            .setView(createFolderLayout)
            .setPositiveButton(getString(R.string.create)) { _, _ ->

                if (folderNamePattern.containsMatchIn(folderNameTextView.editText?.text.toString())) {

                    if (ops.createDir(
                            ops.getInternalPath(),
                            folderNameTextView.editText?.text.toString()
                        ) == 1
                    ) {
                        updateRecyclerView()
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

    // item single tap
    override fun onClick(data: FileItem) {
        if (!data.isDir) {

            val filePath =
                ops.joinPath(ops.getFilesDir(), ops.getInternalPath(), data.name)

            when (Utils.getFileType(data.name)) {
                Constants.IMAGE_TYPE -> {
                    loadImage(filePath)
                }

                Constants.VIDEO_TYPE, Constants.AUDIO_TYPE -> {
                    loadAV(filePath)
                }

                Constants.DOCUMENT_TYPE, Constants.TXT, Constants.PDF -> {
                    loadDocument(filePath)
                }

                else -> {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.unsupported_format),
                        Toast.LENGTH_LONG
                    ).show()
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
                    moveFile(data)
                }

                R.id.copy_item -> {
                    copyFile(data)
                }

            }

            true
        }

        popup.show()
    }

    // Item multi select on icon click
    override fun onItemSelect(data: FileItem, selectedItems: ArrayList<FileItem>) {
        this.selectedItems = selectedItems
        if (this.selectedItems.isEmpty()) {
            toggleFloatingButtonVisibility(false)
        } else {
            toggleFloatingButtonVisibility(true)
        }
    }

    private fun backButtonAction() {
        if (!ops.isRootDirectory()) {
            ops.setGetPreviousPath()
            // display contents of the navigated path
            updateRecyclerView()
        } else {
            finish()
        }
        if (ops.isPreviousRootDirectory())
            topAppBar.title = getString(R.string.app_name)
    }

    private fun renameFilePopup(file: FileItem, context: Context) {
        val builder = MaterialAlertDialogBuilder(context)

        val inflater: LayoutInflater = layoutInflater
        val renameLayout = inflater.inflate(R.layout.rename_layout, null)
        val folderNameTextView =
            renameLayout.findViewById<TextInputLayout>(R.id.renameTextLayout)

        builder.setTitle(getString(R.string.context_menu_rename))
            .setCancelable(true)
            .setView(renameLayout)
            .setPositiveButton(getString(R.string.context_menu_rename)) { _, _ ->

                if (folderNamePattern.containsMatchIn(folderNameTextView.editText?.text.toString())) {
                    val result = ops.renameFile(
                        file,
                        ops.getInternalPath(),
                        folderNameTextView.editText!!.text.toString()
                    )

                    if (result == 0) {
                        Toast.makeText(
                            context,
                            getString(R.string.generic_error),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        updateRecyclerView()
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

        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(getString(R.string.context_menu_delete))
            .setCancelable(true)
            .setView(deleteLayout)
            .setPositiveButton(getString(R.string.context_menu_delete)) { _, _ ->

                if (this.selectedItems.isNotEmpty() && file == null) {
                    for (item in this.selectedItems) {
                        ops.deleteFile(
                            item,
                            ops.getInternalPath()
                        )
                    }

                    // hide delete button after items are deleted
                    toggleFloatingButtonVisibility(false)

                    updateRecyclerView()
                } else {

                    val result = ops.deleteFile(
                        file!!,
                        ops.getInternalPath()
                    )

                    if (result == 0) {
                        Toast.makeText(
                            context,
                            getString(R.string.generic_error),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        updateRecyclerView()
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

    private fun deleteFolderPopup(folder: FolderItem?, context: Context) {

        val inflater: LayoutInflater = layoutInflater
        val deleteLayout = inflater.inflate(R.layout.delete_confirmation, null)

        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(getString(R.string.context_menu_delete))
            .setCancelable(true)
            .setView(deleteLayout)
            .setPositiveButton(getString(R.string.context_menu_delete)) { _, _ ->

                if (folder != null) {
                    val result = ops.deleteFolder(
                        folder,
                        ops.getInternalPath()
                    )
                    if (result == 0) {
                        Toast.makeText(
                            context,
                            getString(R.string.generic_error),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        updateRecyclerView()
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
        val imageViewIntent = Intent(this, PictureView::class.java)
        imageViewIntent.putExtra(Constants.INTENT_KEY_PATH, filePath)
        startActivity(imageViewIntent)
    }

    private fun loadAV(filePath: String) {
        val mediaViewIntent = Intent(this, MediaView::class.java)
        mediaViewIntent.putExtra(Constants.INTENT_KEY_PATH, filePath)
        startActivity(mediaViewIntent)
    }

    private fun loadDocument(filePath: String) {

        var documentViewIntent: Intent? = null

        if (filePath.split('.').last() == Constants.PDF) {
            documentViewIntent = Intent(this, PDFView::class.java)
        } else if (filePath.split('.').last() in arrayOf(
                Constants.TXT,
                Constants.JSON,
                Constants.XML
            )
        ) {
            documentViewIntent = Intent(this, TextDocumentView::class.java)
        }

        if (documentViewIntent != null) {
            documentViewIntent.putExtra(Constants.INTENT_KEY_PATH, filePath)
            startActivity(documentViewIntent)
        }
    }

    private fun moveFile(file: FileItem) {
        ops.moveFileFrom =
            ops.joinPath(ops.getFilesDir(), ops.getInternalPath(), file.name)

        fileMoveCopyView.visibility = View.VISIBLE
        fileMoveCopyName.text = file.name
        fileMoveCopyOperation.text = getString(R.string.move_title)
        fileMoveCopyButton.text = getString(R.string.move_file_title)

    }

    private fun copyFile(file: FileItem) {
        ops.moveFileFrom =
            ops.joinPath(ops.getFilesDir(), ops.getInternalPath(), file.name)

        fileMoveCopyView.visibility = View.VISIBLE
        fileMoveCopyName.text = file.name
        fileMoveCopyOperation.text = getString(R.string.copy_title)
        fileMoveCopyButton.text = getString(R.string.copy_file_title)
    }

    private fun createTextNote(viewContext: Context) {

        val builder = MaterialAlertDialogBuilder(viewContext)

        val inflater: LayoutInflater = layoutInflater
        val textNoteNameLayout = inflater.inflate(R.layout.text_note_name_layout, null)
        val noteNameTextView =
            textNoteNameLayout.findViewById<TextInputLayout>(R.id.newNoteTextLayout)

        builder.setTitle(getString(R.string.new_text_note))
            .setCancelable(true)
            .setView(textNoteNameLayout)
            .setPositiveButton(getString(R.string.create)) { _, _ ->

                if (folderNamePattern.containsMatchIn(noteNameTextView.editText?.text.toString())) {
                    val result = ops.createTextNote(
                        noteNameTextView.editText!!.text.toString() + "." + Constants.TXT
                    )

                    if (result == Constants.FILE_EXIST) {
                        Toast.makeText(
                            viewContext,
                            getString(R.string.file_exists_error),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        loadDocument(result)
                        updateRecyclerView()
                    }
                } else {
                    Toast.makeText(
                        viewContext,
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

    private fun exportItems() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)

        selectExportDirActivityResult.launch(intent)
    }

    private fun toggleFloatingButtonVisibility(isVisible: Boolean) {
        if (isVisible) {
            deleteButton.visibility = View.VISIBLE
            clearButton.visibility = View.VISIBLE
            exportButton.visibility = View.VISIBLE
        } else {
            deleteButton.visibility = View.GONE
            clearButton.visibility = View.GONE
            exportButton.visibility = View.GONE
        }

    }

    override fun onFolderSelect(folderItem: FolderItem) {

        topAppBar.title = folderItem.name

        ops.setInternalPath(folderItem.name)
        updateRecyclerView()

        // clear selection on directory change and hide delete button
        toggleFloatingButtonVisibility(false)

        this.selectedItems.clear()
        updateRecyclerView()
    }

    override fun onFolderLongPress(folderItem: FolderItem, view: View) {
        val popup = PopupMenu(this, view)

        popup.inflate(R.menu.folder_context_menu)

        popup.setOnMenuItemClickListener { item: MenuItem? ->

            when (item!!.itemId) {
                R.id.rename_item -> {
                    renameFilePopup(FileItem(folderItem.name, 0, true, 0), view.context)
                }

                R.id.delete_item -> {
                    deleteFolderPopup(folderItem, view.context)
                }

            }

            true
        }

        popup.show()

    }

}