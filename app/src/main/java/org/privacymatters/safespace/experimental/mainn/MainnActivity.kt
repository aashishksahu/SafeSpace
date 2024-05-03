package org.privacymatters.safespace.experimental.mainn

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import org.privacymatters.safespace.CameraActivity
import org.privacymatters.safespace.R
import org.privacymatters.safespace.experimental.mainn.ui.theme.SafeSpaceTheme
import org.privacymatters.safespace.experimental.settings.SettingsActivity

@OptIn(ExperimentalMaterial3Api::class)
class MainnActivity : AppCompatActivity() {

    private val viewModel: MainActivityViewModel by viewModels()
//    private val folderNamePattern = Regex("[~`!@#\$%^&*()+=|\\\\:;\"'>?/<,\\[\\]{}]")
//    private val ops: Operations = Operations(application)
//    private var importList: ArrayList<Uri> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.ops.internalPath.observe(this) {
            viewModel.getContents()
        }

        /*       File picker result
                val selectFilesActivityResult =
                    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                               importList.clear()

                        if (result.resultCode == AppCompatActivity.RESULT_OK) {

                            val data: Intent? = result.data

                            //If multiple files selected
                            if (data?.clipData != null) {
                                val count = data.clipData?.itemCount ?: 0

                                for (i in 0 until count) {
                                    importList.add(data.clipData?.getItemAt(i)?.uri!!)
                                }
                            }

                            //If single file selected
                            else if (data?.data != null) {
                                importList.add(data.data!!)
                            }

                            Toast.makeTextapplicationContextgetString(R.string.import_files_progress),
                                Toast.LENGTH_SHORT
                            ).show()

                            for (uri in importList) {

                                CoroutineScope(Dispatchers.IO).launch {
                                    val importResult = ops.importFile(
                                        uri, ops.getInternalPath(),
                                    )

                                    when (importResult) {
                                        // 1: success, -1: failure
                                        1 -> {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                updateRecyclerView()
                                            }
                                        }

                                        -1 -> {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                Toast.makeText(
                                                    applicationContext,
                                                    getString(R.string.import_files_error),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            }
                               }
                        }
        */

        setContent {
            MainActivity()
        }

    }

    @Composable
    fun MainActivity() {
        SafeSpaceTheme {
            Scaffold(
                topBar = {
                    TopAppBar()
                },
                bottomBar = {
                    // TODO: Add listener to viewModel.longPressAction changes and change the bottom bar accordingly on long press
                    NormalActionBar()
//                    LongPressActionBar()
//                    MoveActionBar()
//                    CopyActionBar()
                }
            ) { innerPadding ->
                ItemList(innerPadding)
            }
        }
    }

    @Composable
    private fun TopAppBar() {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.primary,
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
                        imageVector = ImageVector.vectorResource(R.drawable.settings_black_36dp),
                        contentDescription = getString(R.string.title_activity_settings)
                    )
                }
            }
        )
    }


    @Composable
    private fun ItemList(innerPadding: PaddingValues) {

        val itemList = viewModel.itemList.observeAsState()

        LazyColumn {
            items(itemList.value as List<Item>) { item ->
                ItemCard(item)
            }
        }

    }

    @Composable
    private fun ItemCard(item: Item) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(64.dp),
                bitmap = item.icon.asImageBitmap(),
                contentDescription = getString(R.string.file_icon_description)
            )
            Column {
                Text(text = item.name, style = MaterialTheme.typography.headlineMedium)
                if (item.isDir) {
                    Text(text = item.itemCount)
                } else {
                    Text(text = item.size)
                }
                Text(text = item.lastModified)
            }
        }
    }

    @Composable
    private fun NormalActionBar() {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.primary,
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
                horizontalArrangement = Arrangement.Center,
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
                        contentDescription = getString(R.string.open_camera),
                    )
                    Text(text = getString(R.string.open_camera))
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable { openCamera() }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.add_fill0_wght400_grad0_opsz24),
                        contentDescription = getString(R.string.import_files)
                    )
                    Text(text = getString(R.string.import_files))
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable { openCamera() }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.baseline_create_new_folder_24),
                        contentDescription = getString(R.string.create_folder)
                    )
                    Text(text = getString(R.string.create_folder))
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable { openCamera() }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.baseline_create_new_folder_24),
                        contentDescription = getString(R.string.create_txt_menu)
                    )
                    Text(text = getString(R.string.create_txt_menu))
                }
            }
        }
    }

    @Composable
    private fun LongPressActionBar() {
        BottomAppBar(
            containerColor = Color(0xFFe6e6ef),
            contentColor = MaterialTheme.colorScheme.primary,
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
                    onClick = { deleteItems() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.delete_white_36dp),
                        contentDescription = getString(R.string.context_menu_delete)
                    )
                }
                IconButton(
                    onClick = { moveItems() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.drive_file_move_black_24dp),
                        contentDescription = getString(R.string.context_menu_move)
                    )
                }
                IconButton(
                    onClick = { copyItems() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.file_copy_black_24dp),
                        contentDescription = getString(R.string.context_menu_copy)
                    )
                }
                IconButton(
                    onClick = { clearSelection() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.clear_all_black_24dp),
                        contentDescription = getString(R.string.multi_clear)
                    )
                }
                IconButton(
                    onClick = { exportSelection() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.file_download_black_24dp),
                        contentDescription = getString(R.string.multi_export)
                    )
                }
                IconButton(
                    onClick = { shareFiles() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.share_black_36dp),
                        contentDescription = getString(R.string.context_menu_share)
                    )
                }
            }
        }
    }

    @Composable
    private fun MoveActionBar() {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { moveToDestination() }
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)

        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = getString(R.string.move_btn_text))
            }
        }
    }

    @Composable
    private fun CopyActionBar() {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { copyToDestination() }
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)

        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(text = getString(R.string.copy_file_title))
            }
        }
    }

    private fun moveToDestination() {
        TODO("Not yet implemented")
    }

    private fun copyToDestination() {
        TODO("Not yet implemented")
    }

    private fun shareFiles() {
        TODO("Not yet implemented")
    }

    private fun exportSelection() {
        TODO("Not yet implemented")
    }

    private fun clearSelection() {
        TODO("Not yet implemented")
    }

    private fun copyItems() {
        TODO("Not yet implemented")
    }

    private fun moveItems() {
        TODO("Not yet implemented")
    }

    private fun deleteItems() {
        TODO("Not yet implemented")
    }

    private fun createTextNote() {
        TODO("Not yet implemented")
    }

    private fun createFolder() {
        TODO("Not yet implemented")
    }

    private fun importFiles() {
        TODO("Not yet implemented")
    }

    private fun openCamera() {
        val cameraIntent = Intent(this, CameraActivity::class.java)
        startActivity(cameraIntent)
    }

    private fun openSettings() {
        val settingsIntent = Intent(this, SettingsActivity::class.java)
        startActivity(settingsIntent)
    }
}


