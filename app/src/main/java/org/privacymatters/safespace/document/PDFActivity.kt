package org.privacymatters.safespace.document

import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.privacymatters.safespace.R
import org.privacymatters.safespace.main.DataManager
import org.privacymatters.safespace.utils.Constants
import org.privacymatters.safespace.utils.LockTimer
import org.privacymatters.safespace.utils.Reload
import org.privacymatters.safespace.utils.Utils
import java.io.File


class PDFActivity : AppCompatActivity() {
    private lateinit var pdfRecyclerView: RecyclerView
    private lateinit var pdfRecyclerViewAdapter: PdfAdapter
    private lateinit var renderer: PdfRenderer
    private val ops = DataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdfview)

        ops.ready(application)

        // hide navigation and status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowInsetsControllerCompat(window, window.decorView).apply {
            // Hide the status bar
            hide(WindowInsetsCompat.Type.statusBars())
            hide(WindowInsetsCompat.Type.navigationBars())
            // Allow showing the status bar with swiping from top to bottom
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // This switch ensures that only switching from activities of this app, the item list
        // will reload (to prevent clearing of selected items during app switching)
        Reload.value = true

        try {
            val path = File(intent.extras?.getString(Constants.INTENT_KEY_PATH)!!)

            renderer =
                PdfRenderer(ParcelFileDescriptor.open(path, ParcelFileDescriptor.MODE_READ_ONLY))

            val mSnapHelper: SnapHelper = PagerSnapHelper()

            pdfRecyclerView = findViewById(R.id.pdfRecyclerView)
            mSnapHelper.attachToRecyclerView(pdfRecyclerView)

            pdfRecyclerViewAdapter = PdfAdapter(renderer)
            pdfRecyclerView.layoutManager = LinearLayoutManager(this)
            pdfRecyclerView.adapter = pdfRecyclerViewAdapter

        } catch (e: Exception) {
            Utils.exportToLog(application, "@PDFView.onCreate()", e)

            val builder = MaterialAlertDialogBuilder(pdfRecyclerView.context)

            builder.setTitle(getString(R.string.pdf_exception_title))
                .setCancelable(true)
                .setMessage(getString(R.string.pdf_exception_subtitle))
                .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                    // Dismiss the dialog
                    dialog.dismiss()
                    onStop()
                    finish()
                }
            val alert = builder.create()
            alert.show()
        }

    }

    override fun onStop() {
        super.onStop()
        renderer.close()
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