package org.privacymatters.safespace.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.privacymatters.safespace.R
import org.privacymatters.safespace.main.ui.BottomAppBar
import org.privacymatters.safespace.main.ui.ItemList
import org.privacymatters.safespace.main.ui.SafeSpaceTheme
import org.privacymatters.safespace.main.ui.TopAppBar
import org.privacymatters.safespace.utils.LockTimer
import org.privacymatters.safespace.utils.Reload

class MainnActivity : AppCompatActivity() {

    private val notificationPermissionRequestCode = 100
    private lateinit var topAppBar: TopAppBar
    private lateinit var bottomAppBar: BottomAppBar

    lateinit var snackBarHostState: SnackbarHostState
    lateinit var selectItemsActivityResult: ActivityResultLauncher<Intent>
    lateinit var exportItemsActivityResult: ActivityResultLauncher<Intent>

    val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        registerFilePickerListener()
        selectExportDirActivityResult()

        // request notification permission for Android 13 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isNotificationPermissionGranted()) {
                requestNotificationPermission()
            }
        }

        enableEdgeToEdge()

        setContent {

            MainActivity()

        }

        // back button - system navigation
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!viewModel.isRootDirectory()) {
                    viewModel.returnToPreviousLocation()
                    viewModel.getItems()
                    viewModel.getInternalPath()
                } else {
                    finish()
                }
            }
        })

    }

    @Composable
    fun MainActivity() {
        SafeSpaceTheme {
            snackBarHostState = remember { SnackbarHostState() }

            val appBarState by remember {
                viewModel.appBarType
            }

            Scaffold(
                snackbarHost = {
                    SnackbarHost(hostState = snackBarHostState)
                },
                topBar = {
                    topAppBar = TopAppBar(this)

                    AnimatedContent(appBarState, label = "") { target ->

                        when (target) {
                            ActionBarType.NORMAL -> topAppBar.NormalTopBar()
                            ActionBarType.LONG_PRESS -> topAppBar.LongPressTopBar()
                            ActionBarType.MOVE -> topAppBar.NormalTopBar()
                            ActionBarType.COPY -> topAppBar.NormalTopBar()
                        }
                    }
                },
                bottomBar = {
                    bottomAppBar = BottomAppBar(this)

                    AnimatedContent(appBarState, label = "") { target ->

                        Box(Modifier.safeDrawingPadding()) {
                            when (target) {
                                ActionBarType.NORMAL -> {
                                    bottomAppBar.NormalActionBar()
                                }

                                ActionBarType.LONG_PRESS -> {
                                    bottomAppBar.LongPressActionBar()
                                }

                                ActionBarType.MOVE -> {
                                    bottomAppBar.MoveActionBar()
                                }

                                ActionBarType.COPY -> {
                                    bottomAppBar.CopyActionBar()
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                val lazyListDisplay = ItemList(this)
                // migrate data from root folder to avoid issues with user created root folder inside
                if (!viewModel.isMigrationComplete()) {
                    ShowChangeLog()
                }

                Column(
                    modifier = Modifier
                        .padding(top = innerPadding.calculateTopPadding())
                ) {
                    topAppBar.BreadCrumbs()
                    lazyListDisplay.LazyList(innerPadding)
                }
            }
        }
    }


    private fun registerFilePickerListener() {
        // File picker result
        selectItemsActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                viewModel.importFiles(result)
            }
    }

    private fun selectExportDirActivityResult() {
        exportItemsActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

                if (result.resultCode == RESULT_OK) {
                    result.data.also { intent ->
                        val uri = intent?.data
                        if (uri != null) {
                            viewModel.exportItems(uri)
                            showMessage(getString(R.string.only_files))
                        }
                    }
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun isNotificationPermissionGranted(): Boolean {
        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        )

        return permission == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            notificationPermissionRequestCode
        )
    }

    private fun showMessage(msg: String) {
        lifecycleScope.launch {
            snackBarHostState.showSnackbar(msg)
        }
    }

    @Composable
    private fun ShowChangeLog() {
        var migrationMsg by remember { mutableStateOf(true) }
        if (migrationMsg) {
            AlertDialog(
                icon = {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = getString(R.string.migration_headline)
                    )
                },
                title = {
                    Text(text = getString(R.string.migration_headline))
                },
                text = {
                    Text(text = getString(R.string.migration_content))
                },
                onDismissRequest = {
                    migrationMsg = false
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.migrateFromRoot()
                            migrationMsg = false
                        }
                    ) {
                        Text(getString(R.string.ok))
                    }
                }
            )

        }
    }

    override fun onResume() {
        super.onResume()

        LockTimer.stop()
        LockTimer.start(this)

        if (Reload.value) {
            viewModel.getItems()
            Reload.value = false
        }
    }

    override fun onStop() {
        LockTimer.stop()
        LockTimer.start(this)
        super.onStop()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        LockTimer.stop()
        LockTimer.start(this)
        return super.onTouchEvent(event)
    }
}


