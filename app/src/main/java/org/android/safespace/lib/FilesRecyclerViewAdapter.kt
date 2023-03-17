package org.android.safespace.lib

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import org.android.safespace.R

class FilesRecyclerViewAdapter(
    private val onItemClickListener: ItemClickListener
) :
    RecyclerView.Adapter<FilesRecyclerViewAdapter.ViewHolder>() {

    private lateinit var fileItemList: List<FileItem>

    private val documentType = "document"
    private val audioType = "audio"
    private val videoType = "video"
    private val imageType = "image"
    private val otherType = "other"

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val fileName: TextView = itemView.findViewById(R.id.fileName)
        val fileDescription: TextView = itemView.findViewById(R.id.fileDescription)
        val fileIcon: ShapeableImageView = itemView.findViewById(R.id.fileIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        // Inflate the custom layout
        val fileView = inflater.inflate(R.layout.files_view, parent, false)
        // Return a new holder instance
        return ViewHolder(fileView)
    }

    override fun getItemCount(): Int {
        return fileItemList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fileItem = fileItemList[position]

        holder.fileName.text = fileItem.name
        holder.fileDescription.text = getSize(fileItem.size)

        // tap on item
        holder.itemView.setOnClickListener {
            onItemClickListener.onClick(fileItem)
        }

        // long press on item
        holder.itemView.setOnLongClickListener {
            onItemClickListener.onLongClick(fileItem, holder.itemView)
            true
        }

        if (fileItem.isDir) {
            holder.fileIcon.setImageResource(R.drawable.folder_36dp)
        } else if (getFileType(fileItem.name) == documentType) {
            holder.fileIcon.setImageResource(R.drawable.description_white_36dp)
        } else if (getFileType(fileItem.name) == imageType) {
            holder.fileIcon.setImageResource(R.drawable.image_white_36dp)
        } else if (getFileType(fileItem.name) == audioType) {
            holder.fileIcon.setImageResource(R.drawable.music_note_white_36dp)
        } else if (getFileType(fileItem.name) == videoType) {
            holder.fileIcon.setImageResource(R.drawable.video_file_white_36dp)
        } else if (getFileType(fileItem.name) == otherType) {
            holder.fileIcon.setImageResource(R.drawable.insert_drive_file_white_36dp)
        }

    }

    private fun getFileType(fileName: String): String {
        val fileExtension = fileName.split(".").last()

        val imageExtensions = arrayOf(
            "jpg",
            "png",
            "gif",
            "webp",
            "tiff",
            "psd",
            "raw",
            "bmp",
            "svg",
            "heif"
        )

        val audioExtensions = arrayOf(
            "aif",
            "cd",
            "midi",
            "mp3",
            "mp2",
            "mpeg",
            "ogg",
            "wav",
            "wma"
        )

        val documentExtensions = arrayOf(
            "csv",
            "dat",
            "db",
            "log",
            "mdb",
            "sav",
            "sql",
            "tar",
            "ods",
            "xlsx",
            "xls",
            "xlsm",
            "xlsb",
            "xml",
            "doc",
            "odt",
            "pdf",
            "rtf",
            "tex",
            "txt",
            "wpd"
        )

        val videoExtensions = arrayOf(
            "3g2",
            "3gp",
            "avi",
            "flv",
            "h264",
            "m4v",
            "mkv",
            "mov",
            "mp4",
            "mpg",
            "mpeg",
            "rm",
            "swf",
            "vob",
            "webm",
            "wmv"
        )

        return when (fileExtension.lowercase()) {
            in imageExtensions -> {
                imageType
            }
            in audioExtensions -> {
                audioType
            }
            in documentExtensions -> {
                documentType
            }
            in videoExtensions -> {
                videoType
            }
            else -> otherType
        }

    }

    private fun getSize(sizeInBytes: Long): String {

        val unit = arrayOf("Bytes", "KB", "MB", "GB", "TB")
        var unitIndex = 0
        var size = sizeInBytes

        try {

            if (sizeInBytes in 0..1024) {
                return sizeInBytes.toString() + " " + unit[unitIndex]
            } else {
                while (size >= 1024) {
                    unitIndex += 1
                    size /= 1024L
                }
            }

        } catch (e: ArrayIndexOutOfBoundsException) {
            return "File size too big"
        }

        return size.toString() + " " + unit[unitIndex]

    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<FileItem>) {
        // This method updates the adapter with the new updated data from database.
        // Replaces the old data with the new one and notify listeners about that change.
        this.fileItemList = data
        notifyDataSetChanged()
    }

}