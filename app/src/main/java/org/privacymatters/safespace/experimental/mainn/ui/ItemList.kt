package org.privacymatters.safespace.experimental.mainn.ui

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.privacymatters.safespace.MediaActivity
import org.privacymatters.safespace.PDFView
import org.privacymatters.safespace.R
import org.privacymatters.safespace.TextDocumentView
import org.privacymatters.safespace.experimental.mainn.Item
import org.privacymatters.safespace.experimental.mainn.MainnActivity
import org.privacymatters.safespace.lib.fileManager.Utils
import org.privacymatters.safespace.lib.utils.Constants

class ItemList(private val activity: MainnActivity) {

    private lateinit var itemList: List<Item>

    @Composable
    fun LazyList(innerPadding: PaddingValues) {
        itemList = activity.viewModel.itemList

        LazyColumn(
            modifier = Modifier
//                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxWidth()
        ) {
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

    @Composable
    private fun FileCard(item: Item) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(10.dp)
                .clickable { openItem(item) }
        ) {
            if (item.icon == null) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        modifier = Modifier.size(32.dp),
                        painter = painterResource(id = item.iconDrawable),
                        contentDescription = activity.getString(R.string.file_icon_description),
                    )
                }
            } else {
                Image(
                    modifier = Modifier
                        .size(64.dp)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    bitmap = item.icon.asImageBitmap(),
                    contentDescription = activity.getString(R.string.file_icon_description),
                )
            }
            Column(
                modifier = Modifier
                    .padding(5.dp)
            ) {
                val (name, ext) = Utils.getFileNameAndExtension(item.name)

                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
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
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource(id = item.iconDrawable),
                    contentDescription = activity.getString(R.string.file_icon_description),
                )

            }
            Column(
                modifier = Modifier
                    .padding(5.dp)
            ) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
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
                    mediaViewIntent.putExtra(Constants.INTENT_KEY_PATH, filePath)
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