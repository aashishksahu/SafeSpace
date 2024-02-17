package org.privacymatters.safespace

import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.privacymatters.safespace.lib.utils.Constants
import org.privacymatters.safespace.lib.mediaManager.PdfAdapter
import java.io.File


class PDFView : AppCompatActivity() {
    private lateinit var pdfRecyclerView: RecyclerView
    private lateinit var pdfRecyclerViewAdapter: PdfAdapter
    private lateinit var renderer: PdfRenderer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdfview)

        try {
            val path = File(intent.extras?.getString(Constants.INTENT_KEY_PATH)!!)

            renderer = PdfRenderer(ParcelFileDescriptor.open(path, ParcelFileDescriptor.MODE_READ_ONLY))

            val mSnapHelper: SnapHelper = PagerSnapHelper()

            pdfRecyclerView = findViewById(R.id.pdfRecyclerView)
            mSnapHelper.attachToRecyclerView(pdfRecyclerView)

            pdfRecyclerViewAdapter = PdfAdapter(renderer)
            pdfRecyclerView.layoutManager = LinearLayoutManager(this)
            pdfRecyclerView.adapter = pdfRecyclerViewAdapter

        } catch (_: Exception) {
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
}