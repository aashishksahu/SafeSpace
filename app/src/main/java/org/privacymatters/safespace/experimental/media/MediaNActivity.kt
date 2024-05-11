package org.privacymatters.safespace.experimental.media

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import kotlinx.coroutines.launch
import org.privacymatters.safespace.R
import org.privacymatters.safespace.experimental.mainn.Item
import org.privacymatters.safespace.experimental.theme.SafeSpaceTheme
import org.privacymatters.safespace.lib.fileManager.Utils
import org.privacymatters.safespace.lib.utils.Constants
import java.io.File

class MediaNActivity : ComponentActivity() {

    private lateinit var itemList: List<Item>
    private val viewModel: MediaActivityViewModel by viewModels()


    @OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.currentPosition = intent.extras?.getInt(Constants.INTENT_KEY_INDEX) ?: 0

        enableEdgeToEdge()
        setContent {
            SafeSpaceTheme {
                itemList = viewModel.itemList
                val pagerState = rememberPagerState(pageCount = { itemList.size })
                val coroutineScope = rememberCoroutineScope()

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                ) {

                    HorizontalPager(state = pagerState) { page ->

                        when (Utils.getFileType(itemList[page].name)) {

                            Constants.IMAGE_TYPE -> {
                                GlideImage(
                                    model = File(viewModel.getPath(itemList[page].name)).canonicalPath,
                                    contentDescription = getString(R.string.photo),
                                    contentScale = ContentScale.FillWidth,
                                    failure = placeholder(R.drawable.image_white_36dp),
                                    loading = placeholder(R.drawable.image_white_36dp)
                                )
                            }

                            Constants.VIDEO_TYPE -> {
                                // Todo: Implement video player using Media3 API
                            }
                        }
                    }

                    LaunchedEffect(key1 = viewModel.currentPosition) {
                        coroutineScope.launch {
                            pagerState.scrollToPage(viewModel.currentPosition)
                        }
                    }
                }
            }
        }
    }


}
