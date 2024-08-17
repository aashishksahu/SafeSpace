package org.privacymatters.safespace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import org.privacymatters.safespace.experimental.main.ui.SafeSpaceTheme
import org.privacymatters.safespace.utils.Utils
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException


class LogActivity : ComponentActivity() {
    private val content = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val logsFolder = File(application.filesDir.canonicalPath + File.separator + "logs")
        val logFile = File(logsFolder.canonicalPath + File.separator + "safe_space_log.txt")

        try {
            val buffer = BufferedReader(FileReader(logFile))
            var line: String?

            while (buffer.readLine().also { line = it } != null) {
                content.append(line)
                content.append('\n')
            }
            buffer.close()
        } catch (e: FileNotFoundException) {
            content.clear()
            content.append(getString(R.string.text_exception_title))

        } catch (e: IOException) {
            content.clear()
            content.append(getString(R.string.text_exception_IO))
        }

        setContent {
            SafeSpaceTheme {
                Box(Modifier.safeDrawingPadding()) {
                    Scaffold(modifier = Modifier.fillMaxSize(),
                        topBar = {
                            IconButton(

                                onClick = {
                                    Utils.clearLogs(application)
                                }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.delete_white_36dp),
                                    contentDescription = getString(R.string.context_menu_delete),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }) { innerPadding ->
                        SelectionContainer {
                            Text(
                                text = content.toString(),
                                modifier = Modifier
                                    .padding(
                                        PaddingValues(
                                            top = innerPadding.calculateTopPadding(),
                                            bottom = innerPadding.calculateBottomPadding(),
                                            start = 5.dp,
                                            end = 5.dp
                                        )
                                    )
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .horizontalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            }
        }
    }
}
