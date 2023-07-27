package org.privacymatters.safespace

import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import org.privacymatters.safespace.lib.Constants
import org.privacymatters.safespace.lib.PdfAdapter
import java.io.File
import java.io.FileNotFoundException


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

        } catch (e: FileNotFoundException) {
            Log.d("ERROR", e.message!!)
        }

    }

    override fun onStop() {
        super.onStop()
        renderer.close()
    }
}