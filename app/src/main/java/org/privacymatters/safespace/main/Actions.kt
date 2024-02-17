package org.privacymatters.safespace.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.privacymatters.safespace.CameraActivity
import org.privacymatters.safespace.R
import org.privacymatters.safespace.lib.fileManager.Operations
import org.privacymatters.safespace.lib.utils.Constants

class Actions(
    private val extendedFab: ExtendedFloatingActionButton,
    private val cameraFab: FloatingActionButton,
    private val importFilesFab: FloatingActionButton,
    private val createDirFab: FloatingActionButton,
    private val createNoteFab: FloatingActionButton,
    private val mainActivity: MainActivity
) {

    private var isFabVisible = false
    private val folderNamePattern = Regex("[~`!@#\$%^&*()+=|\\\\:;\"'>?/<,\\[\\]{}]")
    private val ops: Operations = Operations(mainActivity.application)
    private var importList: ArrayList<Uri> = ArrayList()
    private val fabArea: LinearLayout

    init {

        fabArea = mainActivity.findViewById(R.id.fabArea)
        fabArea.visibility = View.GONE

        fabArea.setOnClickListener {
            hideFab()
        }

        extendedFab.shrink()
        cameraFab.visibility = View.GONE
        importFilesFab.visibility = View.GONE
        createDirFab.visibility = View.GONE
        createNoteFab.visibility = View.GONE

        setExtendedFabListener()
        setCameraFabListener()
        setImportFilesFabListener()
        setCreateDirFabListener()
        setCreateNoteFabListener()

    }

    fun extendedFabVisible(value: Boolean) {
        if (value) {
            extendedFab.visibility = View.VISIBLE
        } else {
            extendedFab.visibility = View.GONE
            hideFab()
        }
    }

    private fun setExtendedFabListener() {
        extendedFab.setOnClickListener {
            if (isFabVisible) {
                hideFab()
            } else {
                showFab()
            }
        }
    }

    private fun setCameraFabListener() {
        cameraFab.setOnClickListener {
            hideFab()
            val intent = Intent(mainActivity, CameraActivity::class.java)
            mainActivity.startActivity(intent)
        }
    }

    private fun setImportFilesFabListener() {
        // File picker result
        val selectFilesActivityResult =
            mainActivity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

                importList.clear()

                if (result.resultCode == AppCompatActivity.RESULT_OK) {

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
                        mainActivity.applicationContext,
                        mainActivity.getString(R.string.import_files_progress),
                        Toast.LENGTH_SHORT
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
                                        mainActivity.updateRecyclerView()
                                    }
                                }

                                -1 -> {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        Toast.makeText(
                                            mainActivity.applicationContext,
                                            mainActivity.getString(R.string.import_files_error),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    }

                }

            }

        importFilesFab.setOnClickListener {
            hideFab()
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.type = "*/*"
            selectFilesActivityResult.launch(intent)
        }
    }

    private fun setCreateDirFabListener() {
        createDirFab.setOnClickListener {
            hideFab()
            createDirPopup(createDirFab.context)
        }
    }

    private fun setCreateNoteFabListener() {
        createNoteFab.setOnClickListener {
            hideFab()
            createTextNote(createNoteFab.context)
        }
    }


    private fun showFab() {
        cameraFab.show()
        importFilesFab.show()
        createDirFab.show()
        createNoteFab.show()
        fabArea.visibility = View.VISIBLE
        isFabVisible = true
    }

    private fun hideFab() {
        cameraFab.hide()
        importFilesFab.hide()
        createDirFab.hide()
        createNoteFab.hide()
        fabArea.visibility = View.GONE
        isFabVisible = false
    }

    private fun createDirPopup(context: Context) {

        val builder = MaterialAlertDialogBuilder(context)

        val inflater: LayoutInflater = mainActivity.layoutInflater
        val createFolderLayout = inflater.inflate(R.layout.create_dir, null)
        val folderNameTextView =
            createFolderLayout.findViewById<TextInputLayout>(R.id.createDirTextLayout)

        builder.setTitle(mainActivity.getString(R.string.create_folder))
            .setCancelable(true)
            .setView(createFolderLayout)
            .setPositiveButton(mainActivity.getString(R.string.create)) { _, _ ->

                if (!folderNamePattern.containsMatchIn(folderNameTextView.editText?.text.toString())) {

                    if (ops.createDir(
                            ops.getInternalPath(),
                            folderNameTextView.editText?.text.toString()
                        ) == 1
                    ) {
                        mainActivity.updateRecyclerView()
                    }
                } else {

                    Toast.makeText(
                        context,
                        mainActivity.getString(R.string.create_folder_invalid_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNeutralButton(mainActivity.getString(R.string.cancel)) { dialog, _ ->
                // Dismiss the dialog
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun createTextNote(viewContext: Context) {

        val builder = MaterialAlertDialogBuilder(viewContext)

        val inflater: LayoutInflater = mainActivity.layoutInflater
        val textNoteNameLayout = inflater.inflate(R.layout.text_note_name_layout, null)
        val noteNameTextView =
            textNoteNameLayout.findViewById<TextInputLayout>(R.id.newNoteTextLayout)

        builder.setTitle(mainActivity.getString(R.string.new_text_note))
            .setCancelable(true)
            .setView(textNoteNameLayout)
            .setPositiveButton(mainActivity.getString(R.string.create)) { _, _ ->

                if (!folderNamePattern.containsMatchIn(noteNameTextView.editText?.text.toString())) {
                    val result = ops.createTextNote(
                        noteNameTextView.editText!!.text.toString() + "." + Constants.TXT
                    )

                    if (result == Constants.FILE_EXIST) {
                        Toast.makeText(
                            viewContext,
                            mainActivity.getString(R.string.file_exists_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        mainActivity.loadDocument(result)
                        mainActivity.updateRecyclerView()
                    }
                } else {
                    Toast.makeText(
                        viewContext,
                        mainActivity.getString(R.string.create_folder_invalid_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNeutralButton(mainActivity.getString(R.string.cancel)) { dialog, _ ->
                // Dismiss the dialog
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

}