package org.privacymatters.safespace.experimental.mainn.ui

import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.privacymatters.safespace.R
import org.privacymatters.safespace.experimental.mainn.MainnActivity
import org.privacymatters.safespace.experimental.settings.SettingsActivity

@OptIn(ExperimentalMaterial3Api::class)
class TopAppBar(private val activity: MainnActivity) {

    @Composable
    fun NormalTopBar() {
        androidx.compose.material3.TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
            title = {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            actions = {
                IconButton(
                    onClick = { openSettings() }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.sort_black_36dp),
                        contentDescription = activity.getString(R.string.sort),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = { openSettings() }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.settings_black_36dp),
                        contentDescription = activity.getString(R.string.title_activity_settings),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }


    private fun openSettings() {
        val settingsIntent = Intent(activity, SettingsActivity::class.java)
        activity.startActivity(settingsIntent)
    }

    private fun showMessage(msg: String) {
        activity.lifecycleScope.launch {
            activity.snackBarHostState.showSnackbar(msg)
        }
    }

}