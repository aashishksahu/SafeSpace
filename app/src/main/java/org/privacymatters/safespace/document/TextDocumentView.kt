package org.privacymatters.safespace.document

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.privacymatters.safespace.R
import org.privacymatters.safespace.lib.Reload
import org.privacymatters.safespace.utils.Constants
import org.privacymatters.safespace.utils.SetTheme
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException

class TextDocumentView : AppCompatActivity() {

    private lateinit var textFileContentView: EditText
    private lateinit var file: File

    override fun onCreate(savedInstanceState: Bundle?) {

        val sharedPref = getSharedPreferences(Constants.SHARED_PREF_FILE, Context.MODE_PRIVATE)

        SetTheme.setTheme(
            delegate,
            applicationContext,
            sharedPref.getString(getString(R.string.change_theme), getString(R.string.System))!!
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_document_view)

        // This switch ensures that only switching from activities of this app, the item list
        // will reload (to prevent clearing of selected items during app switching)
        Reload.value = true

        textFileContentView = findViewById(R.id.textView)

        file = File(intent.extras?.getString(Constants.INTENT_KEY_PATH)!!)

        val content = StringBuilder()

        val mode = findViewById<TextView>(R.id.mode)
        mode.text = file.name

        try {

            val buffer = BufferedReader(FileReader(file))
            var line: String?

            while (buffer.readLine().also { line = it } != null) {
                content.append(line)
                content.append('\n')
            }
            buffer.close()

        } catch (e: FileNotFoundException) {
            val builder = MaterialAlertDialogBuilder(textFileContentView.context)

            builder.setTitle(getString(R.string.text_exception_title))
                .setCancelable(true)
                .setMessage(getString(R.string.text_exception_subtitle))
                .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        } catch (e: IOException) {
            val builder = MaterialAlertDialogBuilder(textFileContentView.context)

            builder.setTitle(getString(R.string.text_exception_IO))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }

        textFileContentView.setText(content.toString())
    }

    override fun onStop() {
        super.onStop()
        saveFile(textFileContentView.text.toString(), file)
    }

    private fun saveFile(contentToSave: String?, file: File?) {

        try {
            if (contentToSave?.isNotEmpty() == true && file != null) {
                file.writeText(contentToSave)
            }

        } catch (e: Exception) {
            Toast.makeText(
                applicationContext,
                getString(R.string.save_error),
                Toast.LENGTH_LONG
            ).show()
            Log.e(Constants.TAG_ERROR, "@TextDocumentView.saveFile ", e)
        }
    }


}