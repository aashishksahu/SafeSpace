package org.privacymatters.safespace.experimental.main.ui

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
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
import org.privacymatters.safespace.R
import org.privacymatters.safespace.document.PDFView
import org.privacymatters.safespace.document.TextDocumentView
import org.privacymatters.safespace.experimental.main.ActionBarType
import org.privacymatters.safespace.experimental.main.Item
import org.privacymatters.safespace.experimental.main.MainnActivity
import org.privacymatters.safespace.media.MediaActivity
import org.privacymatters.safespace.utils.Constants
import org.privacymatters.safespace.utils.Utils
import java.io.File

class ItemList(private val activity: MainnActivity) {

    private lateinit var itemList: List<Item>

    // Todo: List scrolls to top on item select

    @Composable
    fun LazyList(innerPadding: PaddingValues) {
        itemList = activity.viewModel.itemList

        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        val currentIndex = remember { activity.viewModel.scrollToPosition }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            state = listState
        ) {
            coroutineScope.launch {
                listState.animateScrollToItem(currentIndex.intValue)
            }
            itemsIndexed(itemList, key = { _, item: Item -> item.hashCode() })
            { index, item ->
                if (item.isDir)
                    FolderCard(index)
                else
                    FileCard(index)

                if (item == itemList.last()) {
                    Spacer(modifier = Modifier.padding(innerPadding.calculateTopPadding()))
                }

                currentIndex.intValue = 0 // reset index
            }
        }
    }

    @OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
    @Composable
    private fun FileCard(index: Int) {

        val haptics = LocalHapticFeedback.current
        val selectionWatch by
            remember { derivedStateOf { activity.viewModel.itemList[index].isSelected } }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 5.dp)
                .combinedClickable(
                    onClick = {
                        openItem(index)
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        activity.viewModel.appBarType.value = ActionBarType.LONG_PRESS
                        activity.viewModel.setSelected(index)
                    }
                )
                .background(if (activity.viewModel.itemList[index].isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent),
        ) {
            GlideImage(
                modifier = Modifier
                    .size(64.dp)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary),
                        model = File (activity.viewModel.getIconPath(activity.viewModel.itemList[index].name)).canonicalPath,
                contentDescription = activity.getString(R.string.file_icon_description),
                contentScale = ContentScale.Crop,
                failure = placeholder(R.drawable.description_white_36dp),
                loading = placeholder(R.drawable.description_white_36dp)
            )
            Column(
                modifier = Modifier
                    .padding(10.dp)
            ) {
                val (name, ext) = Utils.getFileNameAndExtension(activity.viewModel.itemList[index].name)

                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (selectionWatch) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onBackground
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activity.viewModel.itemList[index].size,
                            color = if (selectionWatch) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.padding(4.dp))

                        Text(
                            text = ext.uppercase(),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(color = MaterialTheme.colorScheme.primary)
                                .padding(start = 2.dp, end = 2.dp),
                            color = if (selectionWatch) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = activity.viewModel.itemList[index].lastModified,
                        color = if (selectionWatch) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun FolderCard(index: Int) {
        val haptics = LocalHapticFeedback.current
        val selectionWatch by remember { derivedStateOf { activity.viewModel.itemList[index].isSelected } }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 5.dp)
                .combinedClickable(
                    onClick = {
                        openItem(index)
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        activity.viewModel.appBarType.value = ActionBarType.LONG_PRESS
                        activity.viewModel.setSelected(index)
                    },
                )
                .background(if (selectionWatch) MaterialTheme.colorScheme.secondary else Color.Transparent)
        ) {
            Image(
                modifier = Modifier
                    .size(64.dp)
                    .padding(10.dp)
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
                    text = activity.viewModel.itemList[index].name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (selectionWatch) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onBackground
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    text = activity.viewModel.itemList[index].itemCount,
                    color = if (selectionWatch) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onBackground
                )

            }
        }
    }

    private fun openItem(index: Int) {
        if (activity.viewModel.appBarType.value == ActionBarType.LONG_PRESS) {
            if (activity.viewModel.itemList[index].isSelected)
                activity.viewModel.setUnSelected(index)
            else
                activity.viewModel.setSelected(index)
        } else {

            if (activity.viewModel.itemList[index].isDir) {
                activity.viewModel.travelToLocation(activity.viewModel.itemList[index].name)
            } else {
                val filePath =
                    activity.viewModel.getPath(activity.viewModel.itemList[index].name)

                when (Utils.getFileType(activity.viewModel.itemList[index].name)) {
                    Constants.IMAGE_TYPE,
                    Constants.VIDEO_TYPE,
                    Constants.AUDIO_TYPE -> {
                        val mediaViewIntent = Intent(activity, MediaActivity::class.java)
                        mediaViewIntent.putExtra(Constants.INTENT_KEY_INDEX, index)
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
    }

    private fun showMessage(msg: String) {
        activity.lifecycleScope.launch {
            activity.snackBarHostState.showSnackbar(msg)
        }
    }

}