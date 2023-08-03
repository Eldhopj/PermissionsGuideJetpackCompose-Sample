package com.plcoding.permissionsguidecompose

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plcoding.permissionsguidecompose.ui.theme.PermissionsGuideComposeTheme

class MainActivity : ComponentActivity() {

    private val multiplePermissionsToRequest = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CALL_PHONE,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PermissionsGuideComposeTheme {
                val viewModel = viewModel<MainViewModel>()
                val dialogQueue = viewModel.visiblePermissionDialogQueue

                val singlePermissionResultLauncher = rememberLauncherForActivityResult( //
                    contract = ActivityResultContracts.RequestPermission(), // Which activity get launched
                    onResult = { isGranted ->
                        viewModel.onPermissionResult(
                            permission = Manifest.permission.CAMERA,
                            isGranted = isGranted
                        )
                    }
                )

                val multiplePermissionResultLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { perms ->
                        multiplePermissionsToRequest.forEach { permission ->
                            viewModel.onPermissionResult(
                                permission = permission,
                                isGranted = perms[permission] == true
                            )
                        }
                    }
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(onClick = {
                        singlePermissionResultLauncher.launch(
                            Manifest.permission.CAMERA
                        )
                    }) {
                        Text(text = "Request one permission")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        multiplePermissionResultLauncher.launch(multiplePermissionsToRequest)
                    }) {
                        Text(text = "Request multiple permission")
                    }
                }


                RationalPermissionRequest(dialogQueue, multiplePermissionResultLauncher)
            }
        }
    }

    @Composable
    private fun RationalPermissionRequest(
        dialogQueue: SnapshotStateList<String>,
        multiplePermissionResultLauncher: ManagedActivityResultLauncher<Array<String>, MutableMap<String, Boolean>>,
        viewModel: MainViewModel = viewModel()
    ) {
        dialogQueue
            .reversed()
            .forEach { permission ->
                RationalPermissionDialog(
                    permissionTextProvider = when (permission) {
                        Manifest.permission.CAMERA -> {
                            CameraPermissionTextProvider()
                        }

                        Manifest.permission.RECORD_AUDIO -> {
                            RecordAudioPermissionTextProvider()
                        }

                        Manifest.permission.CALL_PHONE -> {
                            PhoneCallPermissionTextProvider()
                        }

                        else -> return@forEach
                    },
                    isPermanentlyDeclined = !shouldShowRequestPermissionRationale( // If the permission is permanently declined or <not yet asked> it comes as true
                        permission
                    ),
                    onDismiss = viewModel::dismissDialog,
                    onOkClick = {
                        viewModel.dismissDialog()
                        multiplePermissionResultLauncher.launch(
                            arrayOf(permission)
                        )
                    },
                    onGoToAppSettingsClick = ::openAppSettings
                )
            }
    }
}


fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}
