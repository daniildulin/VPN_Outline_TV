package app.android.outlinevpntv.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import app.android.outlinevpntv.R
import app.android.outlinevpntv.data.preferences.PreferencesManager
import app.android.outlinevpntv.data.remote.ParseUrlOutline
import app.android.outlinevpntv.viewmodel.ServerDialogViewModel

@Composable
fun ServerDialog(
    currentName: String,
    currentKey: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val viewModel: ServerDialogViewModel = viewModel(
        factory = ServerDialogViewModel.Factory(ParseUrlOutline.Validate.Base())
    )

    var serverName by remember { mutableStateOf(currentName) }
    var serverKey by remember { mutableStateOf(currentKey) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isKeyError by remember { mutableStateOf(false) }
    var showFileManagerDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = (LocalContext.current as? Activity)
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val preferencesManager = remember { PreferencesManager(context) }
    var savedVpnKeys by remember { mutableStateOf(preferencesManager.getVpnKeys()) }
    var expanded by remember { mutableStateOf(false) }

   val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showFileManagerDialog = true
        } else {
            Toast.makeText(context, R.string.you_must_grant_permission, Toast.LENGTH_SHORT).show()
        }
    }

    fun validateKey(key: String) {
        isKeyError = !viewModel.validate(key)
    }

    fun setServerKey(key: String) {
        serverKey = key
        validateKey(key)
    }

    AlertDialog(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = R.string.edit_server_info),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val clipboardText = clipboardManager.getText()?.text
                        if (!clipboardText.isNullOrEmpty()) {
                            val parsedName = clipboardText.substringAfterLast("#", serverName)
                            serverName = parsedName
                            setServerKey(clipboardText)
                        } else {
                            Toast.makeText(context, R.string.clipboard_empty, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentPaste,
                        contentDescription = "Paste from clipboard"
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            if (Environment.isExternalStorageManager()) {
                               showFileManagerDialog = true
                            } else {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                intent.data = Uri.parse("package:${context.packageName}")
                                activity?.startActivity(intent)
                            }
                        } else {
                            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
                            val hasPermission = (
                                    ContextCompat.checkSelfPermission(context, permission) ==
                                            PackageManager.PERMISSION_GRANTED
                                    )

                            if (hasPermission) {
                                showFileManagerDialog = true
                            } else {
                               requestPermissionLauncher.launch(permission)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = "Read from file"
                    )
                }
            }
        },
        text = {
            Column {
                Box {
                    OutlinedTextField(
                        value = serverName,
                        onValueChange = { serverName = it },
                        label = { Text(stringResource(id = R.string.saved_vpn_keys)) },
                        singleLine = true,
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .width(200.dp)
                            .height(56.dp),
                        trailingIcon = {
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Dropdown Menu"
                                )
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.add_new_key)) },
                            onClick = {
                                expanded = false
                                serverName = ""
                                setServerKey("")
                            }
                        )

                        savedVpnKeys.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(text = item.name) },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        preferencesManager.deleteVpnKey(item.name)
                                        savedVpnKeys = preferencesManager.getVpnKeys()
                                        serverName = ""
                                        setServerKey("")
                                        expanded = false
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete"
                                        )
                                    }
                                },
                                onClick = {
                                    expanded = false
                                    serverName = item.name
                                    setServerKey(item.key)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Поле "Имя сервера"
                OutlinedTextField(
                    value = serverName,
                    onValueChange = { serverName = it },
                    label = { Text(stringResource(id = R.string.server_name)) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = serverKey,
                    isError = isKeyError,
                    supportingText = {
                        if (isKeyError) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = context.getString(R.string.wrong_outline_key_format),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    onValueChange = {
                        val parsedName = it.substringAfterLast("#", serverName)
                        serverName = parsedName
                        setServerKey(it)
                    },
                    label = { Text(stringResource(id = R.string.outline_key)) },
                    singleLine = true
                )

                errorMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Red
                    )
                }

                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    serverName = context.getString(R.string.default_server_name)
                    serverKey = ""
                }) {
                    Text(stringResource(id = R.string.clear))
                }

                TextButton(onClick = {
                    if (!isLoading) onDismiss()
                }) {
                    Text(stringResource(id = R.string.cancel))
                }

                TextButton(
                    onClick = {
                        isLoading = true
                        try {
                            onSave(serverName, serverKey)
                            preferencesManager.addOrUpdateVpnKey(serverName, serverKey)
                            savedVpnKeys = preferencesManager.getVpnKeys()
                            isLoading = false
                            onDismiss()
                        } catch (e: Exception) {
                            errorMessage = e.message
                            isLoading = false
                        }
                    },
                    enabled = !isLoading && !isKeyError
                ) {
                    Text(stringResource(id = R.string.save))
                }
            }
        }
    )

    if (showFileManagerDialog) {
        StoragePickerDialog(
            onFileSelected = { file ->
                val data = file.readText().trim()
                if (data.isNotBlank()) {
                    val parsedName = data.substringAfterLast("#", serverName)
                    serverName = parsedName
                    setServerKey(data)
                }
                showFileManagerDialog = false
            },
            onDismiss = {
                showFileManagerDialog = false
            }
        )
    }
}

@Preview
@Composable
fun DialogPreview() {
    ServerDialog(
        currentName = "Server #1",
        currentKey = "",
        onDismiss = {},
        onSave = { _, _ -> },
    )
}
