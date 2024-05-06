package org.privacymatters.safespace.experimental.mainn.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.privacymatters.safespace.CameraActivity
import org.privacymatters.safespace.R
import org.privacymatters.safespace.experimental.mainn.MainnActivity

class BottomAppBar(private val activity: MainnActivity) {

    @Composable
    fun NormalActionBar() {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)

        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically

            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable { openCamera() }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.photo_camera_black_24dp),
                        contentDescription = activity.getString(R.string.open_camera),
                    )
                    Text(
                        text = activity.getString(R.string.open_camera),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable {
                            importFiles()
                        }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.add_fill0_wght400_grad0_opsz24),
                        contentDescription = activity.getString(R.string.import_files)
                    )
                    Text(
                        text = activity.getString(R.string.import_files),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable { /* Todo: Create folder dialog */ }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.baseline_create_new_folder_24),
                        contentDescription = activity.getString(R.string.create_folder)
                    )
                    Text(
                        text = activity.getString(R.string.create_folder),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable { }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.edit_note_black_36dp),
                        contentDescription = activity.getString(R.string.create_txt_menu)
                    )
                    Text(
                        text = activity.getString(R.string.create_txt_menu),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }

    @Composable
    private fun LongPressActionBar() {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)

        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { activity.viewModel.deleteItems() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.delete_white_36dp),
                        contentDescription = activity.getString(R.string.context_menu_delete)
                    )
                }
                IconButton(
                    onClick = { activity.viewModel.moveItems() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.drive_file_move_black_24dp),
                        contentDescription = activity.getString(R.string.context_menu_move)
                    )
                }
                IconButton(
                    onClick = { activity.viewModel.copyItems() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.file_copy_black_24dp),
                        contentDescription = activity.getString(R.string.context_menu_copy)
                    )
                }
                IconButton(
                    onClick = { activity.viewModel.clearSelection() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.clear_all_black_24dp),
                        contentDescription = activity.getString(R.string.multi_clear)
                    )
                }
                IconButton(
                    onClick = { activity.viewModel.exportSelection() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.file_download_black_24dp),
                        contentDescription = activity.getString(R.string.multi_export)
                    )
                }
                IconButton(
                    onClick = { activity.viewModel.shareFiles() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.share_black_36dp),
                        contentDescription = activity.getString(R.string.context_menu_share)
                    )
                }
            }
        }
    }

    @Composable
    private fun MoveActionBar() {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { activity.viewModel.moveToDestination() }
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)

        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = activity.getString(R.string.move_btn_text))
            }
        }
    }

    @Composable
    private fun CopyActionBar() {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { activity.viewModel.copyToDestination() }
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)

        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(text = activity.getString(R.string.copy_file_title))
            }
        }
    }


    private fun openCamera() {
        val cameraIntent = Intent(activity, CameraActivity::class.java)
        activity.startActivity(cameraIntent)
    }

    private fun importFiles() {
        showMessage(activity.getString(R.string.import_files_progress))
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "*/*"
        activity.selectFilesActivityResult.launch(intent)
    }

    private fun showMessage(msg: String) {
        activity.lifecycleScope.launch {
            activity.snackBarHostState.showSnackbar(msg)
        }
    }

}