package org.privacymatters.safespace.settings

import android.os.Bundle
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import org.privacymatters.safespace.R
import org.privacymatters.safespace.utils.LockTimer
import org.privacymatters.safespace.utils.Reload

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        // This switch ensures that only switching from activities of this app, the item list
        // will reload (to prevent clearing of selected items during app switching)
        Reload.value = true

        LockTimer.stop()

        val settingsTitle = findViewById<TextView>(R.id.settings_title)

        ViewCompat.setOnApplyWindowInsetsListener(settingsTitle) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
            }

            WindowInsetsCompat.CONSUMED
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()

    }

    override fun onResume() {
        LockTimer.checkLock(this)
        super.onResume()
    }

    override fun onPause() {
        LockTimer.start()
        super.onPause()
    }
}