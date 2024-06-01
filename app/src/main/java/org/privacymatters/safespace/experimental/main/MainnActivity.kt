package org.privacymatters.safespace.experimental.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.privacymatters.safespace.R
import org.privacymatters.safespace.experimental.main.ui.BottomAppBar
import org.privacymatters.safespace.experimental.main.ui.ItemList
import org.privacymatters.safespace.experimental.main.ui.SafeSpaceTheme
import org.privacymatters.safespace.experimental.main.ui.TopAppBar

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

        /* TODO:
            * Improve getItems() performance
         */

        registerFilePickerListener()
        selectExportDirActivityResult()

        // request notification permission for Android 13 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isNotificationPermissionGranted()) {
                requestNotificationPermission()
            }
        }

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

    override fun onRestart() {
        super.onRestart()
        viewModel.getItems()
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
            ) { innerPadding ->
                val lazyListDisplay = ItemList(this)
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

}

