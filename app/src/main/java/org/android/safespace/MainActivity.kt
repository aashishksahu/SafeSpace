package org.android.safespace

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import org.android.safespace.lib.FileUtils
import org.android.safespace.viewmodel.MainActivityViewModel

// Todo: Create file browser recycler view

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainActivityViewModel
    private lateinit var fileUtils: FileUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = MainActivityViewModel(application)

        fileUtils = FileUtils(applicationContext)

        val selectFilesActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

                if (result.resultCode == Activity.RESULT_OK) {

                    val data: Intent? = result.data

                    //If multiple files selected
                    if (data?.clipData != null) {
                        val count = data.clipData?.itemCount ?: 0

                        for (i in 0 until count) {
                            fileUtils.importFile(
                                data.clipData?.getItemAt(i)?.uri!!,
                                viewModel.getInternalPath()

                            )
                        }
                    }

                    //If single file selected
                    else if (data?.data != null) {
                        fileUtils.importFile(
                            data.data!!,
                            viewModel.getInternalPath()
                        )

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

                    fileUtils.createDir(
                        viewModel.getInternalPath(),
                        folderNameTextView.editText?.text.toString()
                    )
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

}