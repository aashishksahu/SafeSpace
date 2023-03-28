package org.android.safespace.lib

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import org.android.safespace.R
import org.android.safespace.viewmodel.MainActivityViewModel

class FilesRecyclerViewAdapter(
    private val onItemClickListener: ItemClickListener,
    private val messages: Map<String, String>,
    private val viewModel: MainActivityViewModel
) :
    RecyclerView.Adapter<FilesRecyclerViewAdapter.ViewHolder>() {

    private lateinit var fileItemList: List<FileItem>

    private val selectedItems = ArrayList<FileItem>()

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
        if (fileItem.isDir) {
            holder.fileDescription.text = messages["directory_indicator"]
        } else {
            holder.fileDescription.text = viewModel.getSize(fileItem.size)
        }

        // tap on item
        holder.itemView.setOnClickListener {
            onItemClickListener.onClick(fileItem)
        }

        // long press on item
        holder.itemView.setOnLongClickListener {
            onItemClickListener.onLongClick(fileItem, holder.itemView)
            true
        }

        setFileIcon(holder, fileItem)

        holder.fileIcon.setOnClickListener {
            if (fileItem !in selectedItems) {
                holder.fileIcon.setImageResource(R.drawable.check_circle_white_36dp)
                selectedItems.add(fileItem)
            } else {
                setFileIcon(holder, fileItem)
                selectedItems.remove(fileItem)
            }
            onItemClickListener.onItemSelect(fileItem, selectedItems)
        }

    }

    private fun setFileIcon(holder: ViewHolder, fileItem: FileItem) {
        if (fileItem.isDir) {
            holder.fileIcon.setImageResource(R.drawable.folder_36dp)
        } else if (viewModel.getFileType(fileItem.name) == Constants.DOCUMENT_TYPE) {
            holder.fileIcon.setImageResource(R.drawable.description_white_36dp)
        } else if (viewModel.getFileType(fileItem.name) == Constants.IMAGE_TYPE) {
            holder.fileIcon.setImageResource(R.drawable.image_white_36dp)
        } else if (viewModel.getFileType(fileItem.name) == Constants.AUDIO_TYPE) {
            holder.fileIcon.setImageResource(R.drawable.music_note_white_36dp)
        } else if (viewModel.getFileType(fileItem.name) == Constants.VIDEO_TYPE) {
            holder.fileIcon.setImageResource(R.drawable.video_file_white_36dp)
        } else if (viewModel.getFileType(fileItem.name) == Constants.OTHER_TYPE) {
            holder.fileIcon.setImageResource(R.drawable.insert_drive_file_white_36dp)
        }
    }



    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<FileItem>) {
        // This method updates the adapter with the new updated data from database.
        // Replaces the old data with the new one and notify listeners about that change.
        this.fileItemList = data
        notifyDataSetChanged()
    }

}