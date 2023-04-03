package org.android.safespace

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.android.safespace.lib.Constants
import java.io.File
import java.io.FileNotFoundException

class PDFView : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdfview)

        try {

            val path = File(intent.extras?.getString(Constants.INTENT_KEY_PATH)!!)

            val pdfView = findViewById<com.github.barteksc.pdfviewer.PDFView>(R.id.pdfView)

            pdfView.fromFile(path)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .defaultPage(0)
                .enableAnnotationRendering(false)
                .scrollHandle(null)
                .enableAntialiasing(true)
                .spacing(0)
                .load()

        } catch (e: FileNotFoundException) {
            Log.d("ERROR", e.message!!)
        }

    }
}