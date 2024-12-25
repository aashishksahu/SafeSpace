package org.privacymatters.safespace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.privacymatters.safespace.utils.LockTimer
import org.privacymatters.safespace.utils.Reload

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val appTitle = findViewById<TextView>(R.id.app_title)

        ViewCompat.setOnApplyWindowInsetsListener(appTitle) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
            }

            WindowInsetsCompat.CONSUMED
        }

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
            var name: String,
            var link: String
        )

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
