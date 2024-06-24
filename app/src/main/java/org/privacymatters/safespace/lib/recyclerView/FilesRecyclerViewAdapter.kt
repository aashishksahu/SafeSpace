package org.privacymatters.safespace.lib.recyclerView

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import org.privacymatters.safespace.R
import org.privacymatters.safespace.lib.fileManager.FileItem
import org.privacymatters.safespace.lib.fileManager.Operations
import org.privacymatters.safespace.utils.Utils
import org.privacymatters.safespace.utils.Constants


class FilesRecyclerViewAdapter(
    private val onItemClickListener: ItemClickListener,
    private val messages: Map<String, String>,
    private val viewModel: Operations
) :
    RecyclerView.Adapter<FilesRecyclerViewAdapter.ViewHolder>() {

    private lateinit var fileItemList: List<FileItem>

    private val selectedItems = ArrayList<FileItem>()
    private var selectedItemsPosition: IntArray? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val fileName: TextView = itemView.findViewById(R.id.fileName)
        val fileDescription: TextView = itemView.findViewById(R.id.fileDescription)
        val fileIcon: ShapeableImageView = itemView.findViewById(R.id.fileIcon)
        val fileLastModified: TextView = itemView.findViewById(R.id.fileDateModified)
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

        holder.fileLastModified.text = Utils.convertLongToTime(fileItem.lastModified)

        holder.fileName.text = fileItem.name
        if (fileItem.isDir) {
            holder.fileDescription.text = messages["directory_indicator"]
        } else {
            holder.fileDescription.text = Utils.getSize(fileItem.size)
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
                selectedItemsPosition?.set(position, 1)
            } else {
                setFileIcon(holder, fileItem)
                selectedItems.remove(fileItem)
                selectedItemsPosition?.set(position, 0)
            }
            onItemClickListener.onItemSelect(fileItem, selectedItems)
        }

        // update the right icons while scrolling
        if (selectedItemsPosition!![position] == 1) {
            holder.fileIcon.setImageResource(R.drawable.check_circle_white_36dp)
        } else {
            setFileIcon(holder, fileItem)
        }
    }

    private fun setFileIcon(holder: ViewHolder, fileItem: FileItem) {
        if (fileItem.isDir) {
            holder.fileIcon.setImageResource(R.drawable.folder_36dp)

        } else if (Utils.getFileType(fileItem.name) == Constants.DOCUMENT_TYPE) {
            holder.fileIcon.setImageResource(R.drawable.description_white_36dp)

        } else if (Utils.getFileType(fileItem.name) == Constants.ZIP) {
            holder.fileIcon.setImageResource(R.drawable.folder_zip_black_24dp)

        } else if (Utils.getFileType(fileItem.name) == Constants.PDF) {
            holder.fileIcon.setImageResource(R.drawable.pdf_icon)

        } else if (Utils.getFileType(fileItem.name) == Constants.JSON) {
            holder.fileIcon.setImageResource(R.drawable.edit_note_black_36dp)

        } else if (Utils.getFileType(fileItem.name) == Constants.XML) {
            holder.fileIcon.setImageResource(R.drawable.edit_note_black_36dp)

        } else if (Utils.getFileType(fileItem.name) == Constants.TXT) {
            holder.fileIcon.setImageResource(R.drawable.edit_note_black_36dp)

        } else if (Utils.getFileType(fileItem.name) == Constants.IMAGE_TYPE) {
            Glide.with(holder.fileIcon)
                .load(
                    viewModel.joinPath(
                        viewModel.getFilesDir(),
                        viewModel.getInternalPath(),
                        fileItem.name
                    )
                )
                .centerCrop()
                .placeholder(R.drawable.image_white_36dp)
                .into(holder.fileIcon)
        } else if (Utils.getFileType(fileItem.name) == Constants.VIDEO_TYPE) {
            Glide.with(holder.fileIcon)
                .load(
                    viewModel.joinPath(
                        viewModel.getFilesDir(),
                        viewModel.getInternalPath(),
                        fileItem.name
                    )
                )
                .centerCrop()
                .placeholder(R.drawable.video_file_white_36dp)
                .into(holder.fileIcon)
        } else if (Utils.getFileType(fileItem.name) == Constants.AUDIO_TYPE) {

            Glide.with(holder.fileIcon)
                .load(
                    viewModel.joinPath(
                        viewModel.getFilesDir(),
                        viewModel.getInternalPath(),
                        fileItem.name
                    )
                )
                .centerCrop()
                .placeholder(R.drawable.music_note_white_36dp)
                .into(holder.fileIcon)
        } else if (Utils.getFileType(fileItem.name) == Constants.OTHER_TYPE) {
            holder.fileIcon.setImageResource(R.drawable.insert_drive_file_white_36dp)
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<FileItem>, emptyMsg: TextView) {
        // This method updates the adapter with the new updated data.
        // Replaces the old data with the new one and notify listeners about that change.

        if (data.isEmpty()) {
            emptyMsg.visibility = View.VISIBLE
        } else {
            emptyMsg.visibility = View.GONE
        }

        this.fileItemList = data
        selectedItemsPosition = IntArray(data.size)
        notifyDataSetChanged()
    }

}