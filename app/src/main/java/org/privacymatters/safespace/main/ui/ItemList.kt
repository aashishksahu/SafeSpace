package org.privacymatters.safespace.main.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import kotlinx.coroutines.launch
import org.privacymatters.safespace.R
import org.privacymatters.safespace.document.PDFActivity
import org.privacymatters.safespace.document.TextDocumentActivity
import org.privacymatters.safespace.main.ActionBarType
import org.privacymatters.safespace.main.Item
import org.privacymatters.safespace.main.MainnActivity
import org.privacymatters.safespace.media.MediaActivity
import org.privacymatters.safespace.utils.Constants
import org.privacymatters.safespace.utils.Utils
import java.io.File

class ItemList(private val activity: MainnActivity) {

    @Composable
    fun LazyList(innerPadding: PaddingValues) {
        val itemList by activity.viewModel.ops.itemListFlow.collectAsStateWithLifecycle(
            lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
        )
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        val openedItemIndex by remember { activity.viewModel.ops.positionHistory }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            state = listState
        ) {

            coroutineScope.launch {
                if (openedItemIndex > -1) listState.animateScrollToItem(
                    openedItemIndex
                )
                activity.viewModel.ops.positionHistory.intValue = -1
            }

            items(itemList) { item ->

                if (item.isDir)
                    FolderCard(item)
                else
                    FileCard(item)

                if (item == itemList.last()) {
                    Spacer(modifier = Modifier.padding(innerPadding.calculateTopPadding()))
                }
            }
        }
    }

    @OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
    @Composable
    private fun FileCard(item: Item) {

        val haptics = LocalHapticFeedback.current

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
//                .padding(10.dp)
                .combinedClickable(
                    onClick = {
                        openItem(item)
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        activity.viewModel.appBarType.value = ActionBarType.LONG_PRESS
                        activity.viewModel.setSelected(item.id)
                    }
                )
                .background(if (item.isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent),
        ) {
            GlideImage(
                modifier = Modifier
                    .size(84.dp)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary),
                model = File(activity.viewModel.getIconPath(item.name)).canonicalPath,
                contentDescription = activity.getString(R.string.file_icon_description),
                contentScale = ContentScale.Crop,
                failure = placeholder(R.drawable.description_white_36dp),
                loading = placeholder(R.drawable.description_white_36dp)
            )
            Column(
                modifier = Modifier
                    .padding(end=10.dp, top = 10.dp, bottom = 10.dp)
            ) {
                val (name, ext) = Utils.getFileNameAndExtension(item.name)

                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (item.isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onBackground
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                        Text(
                            text = Utils.convertLongToDate(item.lastModified),
                            color = if (item.isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onBackground,
                        )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = Utils.getSize(item.size),
                            color = if (item.isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.padding(4.dp))

                        Text(
                            text = ext.uppercase(),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(color = MaterialTheme.colorScheme.primary)
                                .padding(start = 2.dp, end = 2.dp),
                            color = if (item.isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun FolderCard(item: Item) {
        val haptics = LocalHapticFeedback.current
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
//                .padding(10.dp)
                .combinedClickable(
                    onClick = {
                        openItem(item)
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        activity.viewModel.appBarType.value = ActionBarType.LONG_PRESS
                        activity.viewModel.setSelected(item.id)
                    },
                )
                .background(if (item.isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent)
        ) {
            Image(
                modifier = Modifier
                    .size(84.dp)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary),
                painter = painterResource(R.drawable.folder_36dp),
                contentDescription = activity.getString(R.string.file_folder_placeholder)
            )
            Column(
                modifier = Modifier
                    .padding(end=10.dp, top = 10.dp, bottom = 10.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (item.isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onBackground
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    text = item.itemCount,
                    color = if (item.isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onBackground
                )

            }
        }
    }

    private fun openItem(item: Item) {
        if (activity.viewModel.appBarType.value == ActionBarType.LONG_PRESS) {
            if (item.isSelected)
                activity.viewModel.setUnSelected(item.id)
            else
                activity.viewModel.setSelected(item.id)
        } else {

            if (item.isDir) {
                activity.viewModel.travelToLocation(item.name)
            } else {
                val filePath =
                    activity.viewModel.getPath(item.name)

                when (Utils.getFileType(item.name)) {
                    Constants.IMAGE_TYPE,
                    Constants.VIDEO_TYPE,
                    Constants.AUDIO_TYPE -> {
                        val mediaViewIntent = Intent(activity, MediaActivity::class.java)
//                        mediaViewIntent.putExtra(Constants.INTENT_KEY_INDEX, index)
                        activity.viewModel.ops.openedItem = item
                        activity.startActivity(mediaViewIntent)
                    }

                    Constants.DOCUMENT_TYPE, Constants.TXT, Constants.JSON, Constants.XML, Constants.PDF -> {

                        var documentViewIntent: Intent? = null

                        if (filePath.split('.').last() == Constants.PDF) {
                            documentViewIntent = Intent(activity, PDFActivity::class.java)

                        } else if (filePath.split('.').last() in arrayOf(
                                Constants.TXT,
                                Constants.JSON,
                                Constants.XML
                            )
                        ) {
                            documentViewIntent = Intent(activity, TextDocumentActivity::class.java)
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