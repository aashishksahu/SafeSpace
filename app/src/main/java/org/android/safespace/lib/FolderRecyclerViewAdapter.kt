package org.android.safespace.lib

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.android.safespace.R

class FolderRecyclerViewAdapter(
    private val folderClickListener: FolderClickListener,
    private val folderRVAdapterTexts: Map<String, String>,
) :
    RecyclerView.Adapter<FolderRecyclerViewAdapter.ViewHolder>() {
    private var folderItemList: List<FolderItem> = arrayListOf()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val folderName: TextView = itemView.findViewById(R.id.folderName)
        val folderItemCount: TextView = itemView.findViewById(R.id.folderItemCount)
        val folderCard: LinearLayout = itemView.findViewById(R.id.folderViewLinearLayout)

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FolderRecyclerViewAdapter.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        // Inflate the custom layout
        val fileView = inflater.inflate(R.layout.folder_view, parent, false)

        // Return a new holder instance
        return ViewHolder(fileView)
    }

    override fun getItemCount(): Int {
        return folderItemList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val selectedFolder = folderItemList[position]

        holder.folderName.isSelected = true
        holder.folderName.text = selectedFolder.name

        var itemCountText = selectedFolder.itemCount.toString()

        if (selectedFolder.itemCount == 1) {
            itemCountText = itemCountText + " " + folderRVAdapterTexts["item"]
        } else {
            itemCountText = itemCountText + " " + folderRVAdapterTexts["items"]
        }

        holder.folderItemCount.text = itemCountText

        holder.folderCard.setOnClickListener {
            folderClickListener.onFolderSelect(selectedFolder)
        }

        holder.folderCard.setOnLongClickListener {
            folderClickListener.onFolderLongPress(selectedFolder, holder.itemView)
            true
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<FolderItem>) {
        // This method updates the adapter with the new updated data.
        // Replaces the old data with the new one and notify listeners about that change.

        this.folderItemList = data
        notifyDataSetChanged()
    }
}