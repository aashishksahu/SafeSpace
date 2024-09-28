package org.privacymatters.safespace.main.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.privacymatters.safespace.R
import org.privacymatters.safespace.camera.CameraActivity
import org.privacymatters.safespace.document.TextDocumentView
import org.privacymatters.safespace.main.ActionBarType
import org.privacymatters.safespace.main.FileOpCode
import org.privacymatters.safespace.main.MainnActivity
import org.privacymatters.safespace.utils.Constants

class BottomAppBar(private val activity: MainnActivity) {

    private val createFolderShowDialog = mutableStateOf(false)
    private val createNoteShowDialog = mutableStateOf(false)
    private val renameShowDialog = mutableStateOf(false)
    private val namePattern = Regex("[~`!@#\$%^&*()+=|\\\\:;\"'>?/<,\\[\\]{}]")
    private var name: String = ""

    @Composable
    fun NormalActionBar() {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(
                    elevation = 3.dp, shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)

        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically

            ) {

                Button(
                    onClick = { openCamera() }, contentPadding = PaddingValues(5.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.photo_camera_black_24dp),
                            contentDescription = activity.getString(R.string.open_camera),
                        )
                        Text(
                            text = activity.getString(R.string.open_camera),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(start = 2.dp, end = 2.dp)
                        )
                    }
                }

                Button(
                    onClick = { importFiles() }, contentPadding = PaddingValues(5.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.add_fill0_wght400_grad0_opsz24),
                            contentDescription = activity.getString(R.string.import_files)
                        )
                        Text(
                            text = activity.getString(R.string.import_files),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(start = 2.dp, end = 2.dp)
                        )
                    }
                }

                Button(
                    onClick = { createFolderShowDialog.value = true },
                    contentPadding = PaddingValues(5.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val showDialog = remember { createFolderShowDialog }

                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.baseline_create_new_folder_24),
                            contentDescription = activity.getString(R.string.create_folder)
                        )
                        Text(
                            text = activity.getString(R.string.create_folder),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(start = 2.dp, end = 2.dp)
                        )
                        if (showDialog.value) {
                            Prompt(
                                onConfirmation = { isValid ->
                                    if (name.isNotEmpty() && isValid) {
                                        try {
                                            activity.viewModel.createFolder(name)
                                        } catch (e: Exception) {
                                            activity.viewModel.exportToLog(
                                                "@BottomAppBar.NormalActionBar()", e
                                            )
                                            showMessage(activity.getString(R.string.create_folder_invalid_error))
                                        }
                                        name = ""
                                        createFolderShowDialog.value = false
                                    }
                                },
                                onDismiss = {
                                    name = ""
                                    createFolderShowDialog.value = false
                                },
                                dialogTitle = activity.getString(R.string.create_folder),
                                okBtnText = activity.getString(R.string.create)
                            )
                        }
                    }
                }

                Button(
                    onClick = { createNoteShowDialog.value = true },
                    contentPadding = PaddingValues(5.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val showDialog = remember { createNoteShowDialog }

                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.edit_note_black_36dp),
                            contentDescription = activity.getString(R.string.create_txt_menu)
                        )
                        Text(
                            text = activity.getString(R.string.create_txt_menu),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(start = 2.dp, end = 2.dp)
                        )
                        if (showDialog.value) {
                            Prompt(
                                onConfirmation = { isValid ->
                                    if (name.isNotEmpty() && isValid) {
                                        val noteFile = activity.viewModel.createTextNote(name)
                                        val documentViewIntent = Intent(
                                            activity.application, TextDocumentView::class.java
                                        )

                                        documentViewIntent.putExtra(
                                            Constants.INTENT_KEY_PATH, noteFile.canonicalPath
                                        )

                                        name = ""
                                        createNoteShowDialog.value = false

                                        activity.startActivity(documentViewIntent)
                                    }
                                },
                                onDismiss = {
                                    name = ""
                                    createNoteShowDialog.value = false
                                },
                                dialogTitle = activity.getString(R.string.create_txt_menu),
                                okBtnText = activity.getString(R.string.create)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun LongPressActionBar() {
        val selectedFileCount = remember { activity.viewModel.selectedFileCount }
        val selectedFolderCount = remember { activity.viewModel.selectedFolderCount }

        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(
                    elevation = 3.dp, shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))

        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                var displayDeleteConfirmation by remember { mutableStateOf(false) }

                Button(
                    onClick = { displayDeleteConfirmation = true }, colors = ButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color.Red,
                        disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        disabledContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ), contentPadding = PaddingValues(5.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.background(MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.delete_white_36dp),
                            contentDescription = activity.getString(R.string.context_menu_delete),

                            )
                        Text(
                            text = activity.getString(R.string.context_menu_delete),
                            color = Color.Red,
                            modifier = Modifier.padding(start = 2.dp, end = 2.dp)
                        )
                    }
                }


                if (selectedFileCount.intValue + selectedFolderCount.intValue <= 1) {
                    Button(
                        onClick = {
                            renameShowDialog.value = true
                        }, colors = ButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            disabledContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ), contentPadding = PaddingValues(5.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val showDialog = remember { renameShowDialog }
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.drive_file_rename_outline_32dp_5f6368),
                                contentDescription = activity.getString(R.string.context_menu_rename),
                            )
                            Text(
                                text = activity.getString(R.string.context_menu_rename),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(start = 2.dp, end = 2.dp)
                            )
                            if (showDialog.value) {
                                Prompt(
                                    onConfirmation = { isValid ->
                                        if (name.isNotEmpty() && isValid) {

                                            if (!activity.viewModel.renameFile(name)) {
                                                showMessage(activity.getString(R.string.generic_error))
                                            }
                                            name = ""
                                            renameShowDialog.value = false
                                        }
                                    },
                                    onDismiss = {
                                        name = ""
                                        renameShowDialog.value = false
                                    },
                                    dialogTitle = activity.getString(R.string.context_menu_rename),
                                    okBtnText = activity.getString(R.string.context_menu_rename)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        activity.viewModel.setFromPath()
                        activity.viewModel.appBarType.value = ActionBarType.MOVE
                    }, colors = ButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        disabledContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ), contentPadding = PaddingValues(5.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.drive_file_move_black_24dp),
                            contentDescription = activity.getString(R.string.context_menu_move),
                        )
                        Text(
                            text = activity.getString(R.string.context_menu_move),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(start = 2.dp, end = 2.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        activity.viewModel.setFromPath()
                        activity.viewModel.appBarType.value = ActionBarType.COPY
                    }, colors = ButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        disabledContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ), contentPadding = PaddingValues(5.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.file_copy_black_24dp),
                            contentDescription = activity.getString(R.string.context_menu_copy),
                        )
                        Text(
                            text = activity.getString(R.string.context_menu_copy),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(start = 2.dp, end = 2.dp)
                        )
                    }
                }
                Button(
                    onClick = {
                        exportFiles()
                    }, colors = ButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        disabledContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ), contentPadding = PaddingValues(5.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.file_download_black_24dp),
                            contentDescription = activity.getString(R.string.multi_export),
                        )
                        Text(
                            text = activity.getString(R.string.multi_export),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(start = 2.dp, end = 2.dp)
                        )
                    }
                }

                if (displayDeleteConfirmation) {
                    AlertDialog(icon = {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = activity.getString(R.string.context_menu_delete)
                        )
                    }, title = {
                        Text(text = activity.getString(R.string.context_menu_delete))
                    }, text = {
                        Text(text = activity.getString(R.string.delete_confirmation))
                    }, onDismissRequest = {
                        displayDeleteConfirmation = false
                    }, confirmButton = {
                        TextButton(onClick = {
                            activity.viewModel.deleteItems()
                            activity.viewModel.appBarType.value = ActionBarType.NORMAL
                        }) {
                            Text(activity.getString(R.string.context_menu_delete))
                        }
                    }, dismissButton = {
                        TextButton(onClick = {
                            displayDeleteConfirmation = false
                        }) {
                            Text(activity.getString(R.string.cancel))
                        }
                    })
                }
            }
        }
    }

    @Composable
    fun MoveActionBar() {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(
                    elevation = 3.dp, shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)

        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = {
                    activity.viewModel.clearSelection()
                    activity.viewModel.appBarType.value = ActionBarType.NORMAL
                }) {
                    Text(
                        text = activity.getString(R.string.cancel),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }

                Spacer(modifier = Modifier.padding(10.dp))

                Button(
                    onClick = {
                        when (activity.viewModel.moveToDestination()) {
                            FileOpCode.SUCCESS -> showMessage(activity.getString(R.string.move_copy_file_success))
                            FileOpCode.FAIL -> showMessage(activity.getString(R.string.move_copy_file_failure))
                            FileOpCode.EXISTS -> showMessage(activity.getString(R.string.file_exists_error))
                            FileOpCode.SAME_PATH -> showMessage(activity.getString(R.string.same_path))
                            FileOpCode.NO_SPACE -> showMessage(activity.getString(R.string.backup_err_space))
                        }
                        activity.viewModel.appBarType.value = ActionBarType.NORMAL
                        activity.viewModel.clearSelection()
                    },
                ) {
                    Text(text = activity.getString(R.string.move_btn_text))
                }
            }
        }
    }

    @Composable
    fun CopyActionBar() {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(
                    elevation = 3.dp, shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)

        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = {
                    activity.viewModel.clearSelection()
                    activity.viewModel.appBarType.value = ActionBarType.NORMAL
                }) {
                    Text(
                        text = activity.getString(R.string.cancel),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }

                Spacer(modifier = Modifier.padding(10.dp))

                Button(onClick = {
                    when (activity.viewModel.copyToDestination()) {
                        FileOpCode.SUCCESS -> showMessage(activity.getString(R.string.move_copy_file_success))
                        FileOpCode.FAIL -> showMessage(activity.getString(R.string.move_copy_file_failure))
                        FileOpCode.EXISTS -> showMessage(activity.getString(R.string.file_exists_error))
                        FileOpCode.SAME_PATH -> showMessage(activity.getString(R.string.same_path))
                        FileOpCode.NO_SPACE -> showMessage(activity.getString(R.string.backup_err_space))
                    }
                    activity.viewModel.appBarType.value = ActionBarType.NORMAL
                    activity.viewModel.clearSelection()
                }) {
                    Text(text = activity.getString(R.string.copy_file_title))
                }
            }
        }

    }

    @Composable
    fun Prompt(
        onConfirmation: (Boolean) -> Unit,
        onDismiss: () -> Unit,
        dialogTitle: String,
        okBtnText: String
    ) {
        val textFieldContent = remember { mutableStateOf(TextFieldValue(name)) }
        val isValid = remember { mutableStateOf(true) }

        AlertDialog(title = {
            Text(text = dialogTitle)
        }, text = {

            OutlinedTextField(
                label = { Text(activity.getString(R.string.name)) },
                value = textFieldContent.value,
                onValueChange = {
                    textFieldContent.value = it
                    if (namePattern.containsMatchIn(textFieldContent.value.text)) {
                        isValid.value = false
                    } else {
                        isValid.value = true
                        name = textFieldContent.value.text
                    }
                },
                isError = !isValid.value,
                supportingText = {
                    if (!isValid.value) {
                        Text(
                            text = activity.getString(R.string.create_folder_invalid_error),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                trailingIcon = {
                    if (!isValid.value) Icon(
                        Icons.Filled.Warning,
                        contentDescription = "",
                        tint = MaterialTheme.colorScheme.error
                    )
                },
            )
        }, dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(text = activity.getString(R.string.cancel))
            }
            textFieldContent.value = TextFieldValue("")
        }, onDismissRequest = {
            onDismiss()
            textFieldContent.value = TextFieldValue("")
        }, confirmButton = {
            TextButton(onClick = { onConfirmation(isValid.value) }) {
                Text(text = okBtnText)
            }
        })
    }

    private fun openCamera() {
        val cameraIntent = Intent(activity, CameraActivity::class.java)
        activity.startActivity(cameraIntent)
    }

    private fun importFiles() {
//        showMessage(activity.getString(R.string.import_files_progress))
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "*/*"
        activity.selectItemsActivityResult.launch(intent)
    }

    private fun exportFiles() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        activity.exportItemsActivityResult.launch(intent)
        activity.viewModel.appBarType.value = ActionBarType.NORMAL
    }

    private fun showMessage(msg: String) {
        activity.lifecycleScope.launch {
            activity.snackBarHostState.showSnackbar(msg)
        }
    }

}