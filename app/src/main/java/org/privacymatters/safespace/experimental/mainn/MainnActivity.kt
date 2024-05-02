package org.privacymatters.safespace.experimental.mainn

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import org.privacymatters.safespace.R
import org.privacymatters.safespace.experimental.mainn.itemlist.ItemListFragment
import org.privacymatters.safespace.experimental.settings.SettingsActivity
import org.privacymatters.safespace.experimental.mainn.ui.theme.SafeSpaceTheme
import org.privacymatters.safespace.experimental.settings.SettingsFragment

class MainnActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.itemListContrainer, ItemListFragment())
            .commit()

    }
}