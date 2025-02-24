package org.privacymatters.safespace.document

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.privacymatters.safespace.R
import org.privacymatters.safespace.main.DataManager
import org.privacymatters.safespace.main.ui.SafeSpaceTheme
import org.privacymatters.safespace.utils.Constants
import org.privacymatters.safespace.utils.LockTimer
import org.privacymatters.safespace.utils.Reload
import org.privacymatters.safespace.utils.Utils
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException


class TextDocumentActivity : AppCompatActivity() {

    private val ops = DataManager
    private var content: String = ""
    private lateinit var file: File

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()

        ops.ready(application)

        file = File(intent.extras?.getString(Constants.INTENT_KEY_PATH)!!)

        // This switch ensures that only switching from activities of this app, the item list
        // will reload (to prevent clearing of selected items during app switching)
        Reload.value = true
        setContent {
            Box(Modifier.safeDrawingPadding()) {

                SafeSpaceTheme {

                    TextEditor(file)

                }
            }
        }
    }

    @Composable
    private fun TextEditor(file: File) {

        Scaffold(
            topBar = {
                Text(
                    modifier = Modifier.padding(10.dp),
                    text = file.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
        ) { innerPadding ->

            val fileContent = readContent(file)
            var text by remember { mutableStateOf(fileContent) }

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(innerPadding)
                    .clip(RoundedCornerShape(20.dp))
                    .background(color = MaterialTheme.colorScheme.background),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                value = text,
                onValueChange = {
                    text = it
                    content = it
                })
        }
    }

    @Composable
    private fun readContent(file: File): String {
        val fileContent = StringBuilder()
        try {

            val buffer = BufferedReader(FileReader(file))
            var line: String?

            while (buffer.readLine().also { line = it } != null) {
                fileContent.append(line)
                fileContent.append('\n')
            }
            buffer.close()


        } catch (e: FileNotFoundException) {

            Utils.exportToLog(application, "@TextDocumentView.onCreate()", e)

            Alert(
                title = getString(R.string.text_exception_title),
                dialogText = getString(R.string.text_exception_subtitle)
            )

        } catch (e: IOException) {
            Utils.exportToLog(application, "@TextDocumentView.onCreate()", e)

            Alert(
                title = getString(R.string.text_exception_title),
                dialogText = getString(R.string.text_exception_IO)
            )

        }

        return fileContent.toString()
    }

    @Composable
    private fun Alert(title: String, dialogText: String) {
        var alertVisible by remember { mutableStateOf(true) }
        if (alertVisible) {
            AlertDialog(
                icon = {
                    Icon(Icons.Filled.Info, contentDescription = "")
                },
                title = { Text(text = title) },
                text = { Text(text = dialogText) },
                onDismissRequest = {},
                confirmButton = {
                    TextButton(
                        onClick = {
                            alertVisible = false
                        }
                    ) {
                        Text(getString(R.string.ok))
                    }
                }
            )
        }
    }

    private fun saveFile(contentToSave: String?, file: File?) {

        try {
            if (contentToSave != null) {
                file?.writeText(contentToSave)
            }

        } catch (e: Exception) {
            Utils.exportToLog(application, "@TextDocumentView.saveFile()", e)
        }
    }

    override fun onResume() {
        LockTimer.stop()
        LockTimer.checkLock(this)
        super.onResume()
    }

    override fun onPause() {
        saveFile(content, file)
        LockTimer.stop()
        LockTimer.start()
        super.onPause()
    }

}