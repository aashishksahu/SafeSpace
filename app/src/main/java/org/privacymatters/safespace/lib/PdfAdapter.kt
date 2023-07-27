package org.privacymatters.safespace.lib

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import org.privacymatters.safespace.R

class PdfAdapter(private val renderer: PdfRenderer) :
    RecyclerView.Adapter<PdfAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val pdfView: ImageView = view.findViewById(R.id.pdfView)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.pdf_render_view, parent, false)
    )

    override fun getItemCount() = renderer.pageCount

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.pdfView.setImageBitmap(renderer.openPage(position).renderAndClose())
    }

    private fun PdfRenderer.Page.renderAndClose() = use {

        val bitmap = Bitmap.createBitmap(
            width, height, Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)


        bitmap
    }
}