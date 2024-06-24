package org.privacymatters.safespace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.privacymatters.safespace.lib.Reload

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // This switch ensures that only switching from activities of this app, the item list
        // will reload (to prevent clearing of selected items during app switching)
        Reload.value = true

        val libRV = findViewById<RecyclerView>(R.id.librariesRV)
        val libRVAdapter = LibRVAdapter()

        libRV.layoutManager = LinearLayoutManager(this)
        libRV.adapter = libRVAdapter
    }

    inner class LibRVAdapter : RecyclerView.Adapter<LibRVAdapter.ViewHolder>() {

        inner class LibrariesUsed(
            name: String,
            link: String
        ) {
            var name: String
            var link: String

            init {
                this.name = name
                this.link = link
            }
        }

        private val librariesUsedList: ArrayList<LibrariesUsed> = arrayListOf(
            LibrariesUsed(
                getString(R.string.glide),
                "https://github.com/bumptech/glide"
            ), LibrariesUsed(
                getString(R.string.androidx),
                "https://developer.android.com/jetpack/androidx"
            ), LibrariesUsed(
                getString(R.string.camerax),
                "https://developer.android.com/training/camerax"
            ), LibrariesUsed(
                getString(R.string.photoview),
                "https://github.com/Baseflow/PhotoView"
            ), LibrariesUsed(
                getString(R.string.exo),
                "https://github.com/google/ExoPlayer"
            ), LibrariesUsed(
                getString(R.string.material),
                "https://m3.material.io/"
            )
        )

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var name: TextView = itemView.findViewById(R.id.libName)
            val link: TextView = itemView.findViewById(R.id.libLink)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            // Inflate the custom layout
            val libView = inflater.inflate(R.layout.library_view, parent, false)

            // Return a new holder instance
            return ViewHolder(libView)
        }

        override fun getItemCount(): Int {
            return librariesUsedList.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val libItem = librariesUsedList[position]

            holder.name.text = libItem.name
            holder.link.text = libItem.link
        }
    }
}