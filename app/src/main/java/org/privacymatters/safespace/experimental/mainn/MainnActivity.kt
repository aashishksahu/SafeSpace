package org.privacymatters.safespace.experimental.mainn

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.privacymatters.safespace.experimental.mainn.ui.BottomAppBar
import org.privacymatters.safespace.experimental.mainn.ui.ItemList
import org.privacymatters.safespace.experimental.mainn.ui.SafeSpaceTheme
import org.privacymatters.safespace.experimental.mainn.ui.TopAppBar

class MainnActivity : AppCompatActivity() {

    private val notificationPermissionRequestCode = 100
    lateinit var snackBarHostState: SnackbarHostState
    val viewModel: MainActivityViewModel by viewModels()
    private lateinit var topAppBar: TopAppBar
    private lateinit var bottomAppBar: BottomAppBar
    lateinit var selectFilesActivityResult: ActivityResultLauncher<Intent>

//    private val folderNamePattern = Regex("[~`!@#\$%^&*()+=|\\\\:;\"'>?/<,\\[\\]{}]")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* TODO:
            * Add listener to viewModel.longPressAction changes and change the bottom bar
              accordingly on long press
            * Add breadcrumbs
         */

        registerFilePickerListener()

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

            Scaffold(
                snackbarHost = {
                    SnackbarHost(hostState = snackBarHostState)
                },
                topBar = {
                    topAppBar = TopAppBar(this)
                    topAppBar.NormalTopBar()
                },
                bottomBar = {
                    bottomAppBar = BottomAppBar(this)
                    bottomAppBar.NormalActionBar()
//                    LongPressActionBar()
//                    MoveActionBar()
//                    CopyActionBar()
                }
            ) { innerPadding ->
                val lazyListDisplay = ItemList(this)

                BreadCrumbs()

                lazyListDisplay.LazyList(innerPadding)
            }
        }
    }

    @Composable
    fun BreadCrumbs() {
        // Todo: "Not yet implemented")
    }

    private fun registerFilePickerListener() {
        // File picker result
        selectFilesActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                viewModel.importFiles(result)
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


