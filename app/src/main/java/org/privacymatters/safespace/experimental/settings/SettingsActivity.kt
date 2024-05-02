package org.privacymatters.safespace.experimental.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.privacymatters.safespace.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()

    }
}