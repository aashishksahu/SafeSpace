package org.privacymatters.safespace.experimental.media

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import kotlinx.coroutines.launch
import org.privacymatters.safespace.R
import org.privacymatters.safespace.experimental.main.ui.SafeSpaceTheme
import org.privacymatters.safespace.utils.Constants
import org.privacymatters.safespace.utils.Utils

class MediaActivity : ComponentActivity() {

    private val viewModel: MediaActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafeSpaceTheme {
                MediaPager()
            }
        }

        // back button - system navigation
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.setPositionHistory() // Todo: doesn't work
                finish()
            }
        })

    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
    @Composable
    private fun MediaPager() {

        // Display 10 items
        val pagerState = rememberPagerState(pageCount = {
            viewModel.itemList.size
        })

        val coroutineScope = rememberCoroutineScope()

        HorizontalPager(
            state = pagerState
        ) { page ->

            viewModel.openedItemPosition = page

            LaunchedEffect(pagerState) {
                coroutineScope.launch {
                    pagerState.scrollToPage(viewModel.openedItemPosition)
                }
            }
            when (Utils.getFileType(viewModel.itemList[page].name)) {
                Constants.IMAGE_TYPE -> {
                    GlideImage(
                        modifier = Modifier.fillMaxSize(),
                        model = viewModel.getFilePath(page),
                        contentDescription = getString(R.string.media_view_desc),
                        contentScale = ContentScale.Fit,
                        failure = placeholder(R.drawable.image_white_36dp),
                        loading = placeholder(R.drawable.image_white_36dp)
                    )
                }

                Constants.VIDEO_TYPE,
                Constants.AUDIO_TYPE -> {
                    // Todo: Add Video Player function
                }
            }
        }


    }


}
