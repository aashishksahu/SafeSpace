package org.privacymatters.safespace

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.privacymatters.safespace.lib.Constants
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException

class TextDocumentView : AppCompatActivity() {

    private lateinit var textFileContentView: EditText
    private lateinit var file: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_document_view)

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
            Log.d("ERROR", e.message!!)
        } catch (e: IOException) {
            Log.d("ERROR", e.message!!)
        }

        textFileContentView.setText(content.toString())

        val saveButton = findViewById<FloatingActionButton>(R.id.saveButton)

        saveButton.setOnClickListener {

            try {
                saveFile(textFileContentView.text.toString(), file)

                Toast.makeText(
                    applicationContext,
                    getString(R.string.save_success),
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.save_error),
                    Toast.LENGTH_LONG
                ).show()
                Log.d("ERROR", e.message!!)
            }


        }
    }

    override fun onStop() {
        super.onStop()
        saveFile(textFileContentView.text.toString(), file)
    }

    private fun saveFile(contentToSave: String?, file: File?) {

        if (contentToSave?.isNotEmpty() == true && file != null) {
            file.writeText(contentToSave)
        }

    }


}