package org.privacymatters.safespace.experimental.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.privacymatters.safespace.R
import org.privacymatters.safespace.lib.Reload

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // This switch ensures that only switching from activities of this app, the item list
        // will reload (to prevent clearing of selected items during app switching)
        Reload.value = true

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()

    }
}