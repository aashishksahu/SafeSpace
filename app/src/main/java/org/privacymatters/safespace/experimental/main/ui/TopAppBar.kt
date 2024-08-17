package org.privacymatters.safespace.experimental.main.ui

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.privacymatters.safespace.R
import org.privacymatters.safespace.experimental.main.ActionBarType
import org.privacymatters.safespace.experimental.main.MainnActivity
import org.privacymatters.safespace.experimental.settings.SettingsActivity
import org.privacymatters.safespace.utils.Constants

@OptIn(ExperimentalMaterial3Api::class)
class TopAppBar(private val activity: MainnActivity) {

    private val showSortDialog = mutableStateOf(false)
    private lateinit var breadcrumbs: List<String>

    @Composable
    fun NormalTopBar() {
        androidx.compose.material3.TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
            title = {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            actions = {
                val showDialog = remember { showSortDialog }
                IconButton(
                    onClick = {
                        showDialog.value = true
                    }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.sort_black_36dp),
                        contentDescription = activity.getString(R.string.sort),
                    )

                    if (showDialog.value) {
                        SortDialog(
                            onDismiss = { showSortDialog.value = false },
                            dialogTitle = activity.getString(R.string.sort)
                        )
                    }

                }
                IconButton(
                    onClick = { openSettings() }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.settings_black_36dp),
                        contentDescription = activity.getString(R.string.title_activity_settings),
                    )
                }
            }
        )
    }

    @SuppressLint("PrivateResource")
    @Composable
    fun LongPressTopBar() {
        val selectedFileCountState by remember { activity.viewModel.selectedFileCount }
        val selectedFolderCountState by remember { activity.viewModel.selectedFolderCount }

        androidx.compose.material3.TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.primary,
            ),
            title = {
                OutlinedButton(
                    onClick = {
                        activity.viewModel.appBarType.value = ActionBarType.NORMAL
                        activity.viewModel.clearSelection()
                    }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = activity.getString(R.string.cancel),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.padding(5.dp))

                    val selectedItemsCount =
                        (selectedFileCountState + selectedFolderCountState).toString() + " " + stringResource(
                            id = androidx.compose.ui.R.string.selected
                        )
                    Text(
                        text = selectedItemsCount,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            actions = {
                var shareAlertState by remember { mutableStateOf(false) }

                if (selectedFileCountState < 2 && selectedFolderCountState < 1) {
                    IconButton(

                        onClick = {
                            shareAlertState = true
                        }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.share_black_36dp),
                            contentDescription = activity.getString(R.string.context_menu_share),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        if (shareAlertState) {
                            AlertDialog(
                                icon = {
                                    Icon(
                                        Icons.Filled.Warning,
                                        contentDescription = activity.getString(R.string.share_dialog_title)
                                    )
                                },
                                title = {
                                    Text(text = activity.getString(R.string.share_dialog_title))
                                },
                                text = {
                                    Text(text = activity.getString(R.string.share_dialog_description))
                                },
                                onDismissRequest = {
                                    shareAlertState = false
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            activity.viewModel.appBarType.value =
                                                ActionBarType.NORMAL
                                            activity.viewModel.shareFile()
                                        }
                                    ) {
                                        Text(activity.getString(R.string.context_menu_share))
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = {
                                            shareAlertState = false
                                        }
                                    ) {
                                        Text(activity.getString(R.string.cancel))
                                    }
                                }
                            )
                        }

                    }
                }
            }
        )
    }

    @Composable
    fun BreadCrumbs() {
        breadcrumbs = activity.viewModel.internalPathList
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        LazyRow(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            state = listState
        ) {

            coroutineScope.launch {
                listState.animateScrollToItem(breadcrumbs.lastIndex)
            }

            var rootIconDrawn = false
            items(breadcrumbs) {
                if (it == Constants.ROOT && !rootIconDrawn) {
                    rootIconDrawn = true
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = activity.getString(R.string.app_name)
                    )
                } else {
                    Text(text = it, style = MaterialTheme.typography.bodyLarge)
                }
                Text(text = " / ", style = MaterialTheme.typography.bodyLarge)
            }
        }

    }

    @Composable
    fun SortDialog(
        onDismiss: () -> Unit,
        dialogTitle: String
    ) {

        var sortBy = Constants.NAME
        var sortOrder = Constants.ASC

        AlertDialog(
            title = {
                Text(text = dialogTitle)
            },
            text = {

                Column {

                    Text(
                        text = activity.getString(R.string.sort_by_title_file),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    val sortByState = remember { mutableStateOf(sortBy) }

                    Row(
                        Modifier
                            .selectableGroup()
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        RadioButton(
                            selected = sortByState.value == Constants.NAME,
                            onClick = {
                                sortByState.value = Constants.NAME
                                sortBy = sortByState.value
                            },
                            modifier = Modifier.semantics {
                                contentDescription = activity.getString(R.string.name)
                            }
                        )
                        Text(
                            text = activity.getString(R.string.name), maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        RadioButton(
                            selected = sortByState.value == Constants.DATE,
                            onClick = {
                                sortByState.value = Constants.DATE
                                sortBy = sortByState.value
                            },
                            modifier = Modifier.semantics {
                                contentDescription = activity.getString(R.string.date)
                            }
                        )
                        Text(
                            text = activity.getString(R.string.date), maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        RadioButton(
                            selected = sortByState.value == Constants.SIZE,
                            onClick = {
                                sortByState.value = Constants.SIZE
                                sortBy = sortByState.value
                            },
                            modifier = Modifier.semantics {
                                contentDescription = activity.getString(R.string.size)
                            }
                        )
                        Text(
                            text = activity.getString(R.string.size), maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                    }
                    HorizontalDivider()
                    Spacer(modifier = Modifier.padding(5.dp))

                    Text(
                        text = activity.getString(R.string.sort_by_order),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    val sortOrderState = remember { mutableStateOf(sortOrder) }

                    Row(
                        Modifier
                            .selectableGroup()
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        RadioButton(
                            selected = sortOrderState.value == Constants.ASC,
                            onClick = {
                                sortOrderState.value = Constants.ASC
                                sortOrder = sortOrderState.value
                            },
                            modifier = Modifier.semantics {
                                contentDescription = activity.getString(R.string.asc)
                            }
                        )
                        Text(
                            text = activity.getString(R.string.asc), maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        RadioButton(
                            selected = sortOrderState.value == Constants.DESC,
                            onClick = {
                                sortOrderState.value = Constants.DESC
                                sortOrder = sortOrderState.value
                            },
                            modifier = Modifier.semantics {
                                contentDescription = activity.getString(R.string.desc)
                            }
                        )
                        Text(
                            text = activity.getString(R.string.desc), maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                    }
                    HorizontalDivider()
                    Spacer(modifier = Modifier.padding(5.dp))

                }
            },
            dismissButton = {
                TextButton(onClick = { onDismiss() }) {
                    Text(text = activity.getString(R.string.cancel))
                }
            },
            onDismissRequest = {
                onDismiss()
            },
            confirmButton = {
                TextButton(onClick = {
                    activity.viewModel.sortItems(sortBy, sortOrder)
                    showSortDialog.value = false
                }) {
                    Text(text = activity.getString(R.string.ok))
                }
            })
    }

    private fun openSettings() {
        val settingsIntent = Intent(activity, SettingsActivity::class.java)
        activity.startActivity(settingsIntent)
    }

//    private fun showMessage(msg: String) {
//        activity.lifecycleScope.launch {
//            activity.snackBarHostState.showSnackbar(msg)
//        }
//    }

}