package org.privacymatters.safespace.experimental.main.ui

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import kotlinx.coroutines.launch
import org.privacymatters.safespace.media.MediaActivity
import org.privacymatters.safespace.document.PDFView
import org.privacymatters.safespace.R
import org.privacymatters.safespace.document.TextDocumentView
import org.privacymatters.safespace.experimental.main.Item
import org.privacymatters.safespace.experimental.main.MainnActivity
import org.privacymatters.safespace.utils.Utils
import org.privacymatters.safespace.utils.Constants
import java.io.File

class ItemList(private val activity: MainnActivity) {

    private lateinit var itemList: List<Item>

    @Composable
    fun LazyList(innerPadding: PaddingValues) {
        itemList = activity.viewModel.itemList

        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        val currentIndex = remember { activity.viewModel.scrollToPosition }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
        ) {

            coroutineScope.launch {
                // todo: Scroll after back press not working
                listState.animateScrollToItem(currentIndex.intValue)
            }

            items(itemList) { item ->
                if (item.isDir) {
                    FolderCard(item)
                } else {
                    FileCard(item)
                }
                if (item == itemList.last()) {
                    Spacer(modifier = Modifier.padding(innerPadding.calculateTopPadding()))
                }
            }
        }
    }

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    private fun FileCard(item: Item) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(10.dp)
                .clickable { openItem(item) }
        ) {
            GlideImage(
                modifier = Modifier
                    .size(64.dp)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary),
                model = File(activity.viewModel.getIconPath(item.name)).canonicalPath,
                contentDescription = activity.getString(R.string.file_icon_description),
                contentScale = ContentScale.Crop,
                failure = placeholder(R.drawable.description_white_36dp),
                loading = placeholder(R.drawable.description_white_36dp)
            )
            Column(
                modifier = Modifier
                    .padding(10.dp)
            ) {
                val (name, ext) = Utils.getFileNameAndExtension(item.name)

                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = item.size)

                        Spacer(modifier = Modifier.padding(4.dp))


                        Text(
                            text = ext.uppercase(),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(color = MaterialTheme.colorScheme.secondary)
                                .padding(start = 2.dp, end = 2.dp),
                            color = MaterialTheme.colorScheme.onSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(text = item.lastModified)
                }
            }
        }
    }

    @Composable
    private fun FolderCard(item: Item) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(10.dp)
                .clickable { openItem(item) }
        ) {
            Image(
                modifier = Modifier
                    .size(64.dp)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary),
                painter = painterResource(R.drawable.folder_36dp),
                contentDescription = activity.getString(R.string.file_folder_placeholder)
            )
            Column(
                modifier = Modifier
                    .padding(10.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    text = item.itemCount
                )

            }
        }
    }

    private fun openItem(item: Item) {
        if (item.isDir) {
            activity.viewModel.travelToLocation(item.name)
        } else {
            val filePath = activity.viewModel.getPath(item.name)

            when (Utils.getFileType(item.name)) {
                Constants.IMAGE_TYPE,
                Constants.VIDEO_TYPE,
                Constants.AUDIO_TYPE -> {
                    val mediaViewIntent = Intent(activity, MediaActivity::class.java)
                    mediaViewIntent.putExtra(Constants.INTENT_KEY_INDEX, itemList.indexOf(item))
                    activity.startActivity(mediaViewIntent)
                }

                Constants.DOCUMENT_TYPE, Constants.TXT, Constants.JSON, Constants.XML, Constants.PDF -> {

                    var documentViewIntent: Intent? = null

                    if (filePath.split('.').last() == Constants.PDF) {
                        documentViewIntent = Intent(activity, PDFView::class.java)

                    } else if (filePath.split('.').last() in arrayOf(
                            Constants.TXT,
                            Constants.JSON,
                            Constants.XML
                        )
                    ) {
                        documentViewIntent = Intent(activity, TextDocumentView::class.java)
                    }

                    if (documentViewIntent != null) {
                        documentViewIntent.putExtra(Constants.INTENT_KEY_PATH, filePath)
                        activity.startActivity(documentViewIntent)
                    }
                }

                Constants.ZIP -> {
                    activity.viewModel.extractZip(filePath)
                }

                else -> {
                    showMessage(activity.getString(R.string.unsupported_format))
                }
            }
        }
    }

    private fun showMessage(msg: String) {
        activity.lifecycleScope.launch {
            activity.snackBarHostState.showSnackbar(msg)
        }
    }

}