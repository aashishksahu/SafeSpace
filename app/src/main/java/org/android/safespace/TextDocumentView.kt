package org.android.safespace

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.android.safespace.lib.Constants
import java.io.*

class TextDocumentView : AppCompatActivity() {

    /*
        Todo: text view input type none single line [BUG]
    */
    private var isDarkTheme = true
    private var isReadOnly = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_document_view)

        val textView = findViewById<EditText>(R.id.textView)
        // start in read-only mode
        textView.inputType = InputType.TYPE_NULL

        val mode = findViewById<TextView>(R.id.mode)

        val content = StringBuilder()

        val file = File(intent.extras?.getString(Constants.INTENT_KEY_PATH)!!)

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

        textView.setText(content.toString())


        val saveButton = findViewById<FloatingActionButton>(R.id.saveButton)
        val themeButton = findViewById<FloatingActionButton>(R.id.themeButton)

        saveButton.setOnClickListener {

            if (isReadOnly) {
                saveButton.setImageResource(R.drawable.save_white_24dp)
                textView.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
                mode.text = getString(R.string.edit_mode)
                isReadOnly = false
            } else {

                val contentToSave = textView.text.toString()

                try {
                    file.writeText(contentToSave)

                    Toast.makeText(
                        applicationContext,
                        getString(R.string.save_success),
                        Toast.LENGTH_LONG
                    ).show()

                    saveButton.setImageResource(R.drawable.edit_black_24dp)
                    textView.inputType = InputType.TYPE_NULL
                    mode.text = getString(R.string.read_only)
                    isReadOnly = true

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

        themeButton.setOnClickListener {
            if (isDarkTheme) {
                // set to bright
                textView.background = ContextCompat.getDrawable(applicationContext, R.color.white)
                textView.setTextColor(ContextCompat.getColor(applicationContext, R.color.black))
                isDarkTheme = false
            } else {
                // set to dark
                textView.background = ContextCompat.getDrawable(applicationContext, R.color.black)
                textView.setTextColor(ContextCompat.getColor(applicationContext, R.color.white))
                isDarkTheme = true
            }
        }
    }
}