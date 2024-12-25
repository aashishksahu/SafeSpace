package org.privacymatters.safespace.document

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.privacymatters.safespace.R
import org.privacymatters.safespace.main.DataManager
import org.privacymatters.safespace.utils.Constants
import org.privacymatters.safespace.utils.LockTimer
import org.privacymatters.safespace.utils.Reload
import org.privacymatters.safespace.utils.SetTheme
import org.privacymatters.safespace.utils.Utils
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException


class TextDocumentActivity : AppCompatActivity() {

    enum class ScrollDirection {
        GO_UP, GO_DOWN
    }

    private lateinit var textFileContentView: EditText
    private lateinit var scrollTo: ImageButton
    private lateinit var scrollView: NestedScrollView
    private var scrollDirection = ScrollDirection.GO_DOWN
    private lateinit var file: File
    private val ops = DataManager

    override fun onCreate(savedInstanceState: Bundle?) {

        val sharedPref = getSharedPreferences(Constants.SHARED_PREF_FILE, Context.MODE_PRIVATE)

        SetTheme.setTheme(
            delegate, applicationContext,
            sharedPref.getString(getString(R.string.change_theme), getString(R.string.System))!!
        )

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContentView(R.layout.activity_text_document_view)

        ops.ready(application)

        // This switch ensures that only switching from activities of this app, the item list
        // will reload (to prevent clearing of selected items during app switching)
        Reload.value = true

        val mode = findViewById<TextView>(R.id.mode)
        scrollView = findViewById(R.id.scrollView2)
        scrollTo = findViewById(R.id.scrollTo)
        textFileContentView = findViewById(R.id.textView)

        ViewCompat.setOnApplyWindowInsetsListener(mode) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top + 10
            }

            WindowInsetsCompat.CONSUMED
        }

        ViewCompat.setOnApplyWindowInsetsListener(scrollTo) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updateLayoutParams<MarginLayoutParams> {
                leftMargin = insets.left
                bottomMargin = insets.bottom + 10
                rightMargin = insets.right + 10
            }

            WindowInsetsCompat.CONSUMED
        }

        textFileContentView.addTextChangedListener {
            saveFile(textFileContentView.text.toString(), file)
        }

        scrollView.setOnScrollChangeListener(({ _, _, y, _, oldY ->
            if ((y - oldY) >= 0) { // going down
                scrollDirection = ScrollDirection.GO_UP
                scrollTo.setImageResource(R.drawable.keyboard_arrow_up_24dp_e8eaed_fill0_wght400_grad0_opsz24)
            } else { // going up
                scrollDirection = ScrollDirection.GO_DOWN
                scrollTo.setImageResource(R.drawable.keyboard_arrow_down_32dp_e8eaed_fill0_wght400_grad0_opsz40)
            }
        }))

        scrollTo.setOnClickListener {
            when (scrollDirection) {
                ScrollDirection.GO_UP -> scrollView.fullScroll(NestedScrollView.FOCUS_UP)
                ScrollDirection.GO_DOWN -> scrollView.fullScroll(NestedScrollView.FOCUS_DOWN)
            }
        }

        file = File(intent.extras?.getString(Constants.INTENT_KEY_PATH)!!)

        val content = StringBuilder()

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

            Utils.exportToLog(application, "@TextDocumentView.onCreate()", e)

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
            Utils.exportToLog(application, "@TextDocumentView.onCreate()", e)
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

    private fun saveFile(contentToSave: String?, file: File?) {

        try {
            if (contentToSave?.isNotEmpty() == true && file != null) {
                file.writeText(contentToSave)
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
        LockTimer.stop()
        LockTimer.start()
        super.onPause()
    }

}