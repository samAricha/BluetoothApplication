package com.teka.bluetoothapplication.permissions_module

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.activity.ComponentActivity
import timber.log.Timber

object PermissionManager {

    fun setupPermissionLaunchers(componentActivity: ComponentActivity): PermissionLaunchersDto {
        val multiplePermissionLauncher = componentActivity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionResults(permissions, componentActivity)
        }

        val singlePermissionLauncher = componentActivity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(componentActivity, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(componentActivity, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        val enableBluetoothLauncher = componentActivity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(componentActivity, "Bluetooth Enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(componentActivity, "Bluetooth not enabled", Toast.LENGTH_SHORT).show()
            }
        }

        val discoverableLauncher = componentActivity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(componentActivity, "Device is now discoverable", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(componentActivity, "Failed to make device discoverable", Toast.LENGTH_SHORT).show()
            }
        }

        val locationPermissionLauncher = componentActivity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Timber.tag("PermissionUtils").i("Location Permission Granted")
            } else {
                Timber.tag("PermissionUtils").i("Location Permission Denied")
                showPermissionDeniedMessage(componentActivity)
            }
        }

        val onExecutionPermissionLauncher = componentActivity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            // Handle permission result for execution
            onPermissionResult(isGranted)
        }

        return PermissionLaunchersDto(
            multiplePermissionLauncher,
            singlePermissionLauncher,
            enableBluetoothLauncher,
            discoverableLauncher,
            locationPermissionLauncher,
            onExecutionPermissionLauncher
        )
    }

    private fun handlePermissionResults(permissions: Map<String, Boolean>, componentActivity: ComponentActivity) {
        val allPermissionsGranted = permissions.all { it.value }

        if (allPermissionsGranted) {
            Toast.makeText(componentActivity, "All permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            permissions.forEach { (permission, isGranted) ->
                if (isGranted) {
                    Toast.makeText(componentActivity, "$permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    askSinglePermission(permission, componentActivity)
                }
            }
        }
    }

    private fun askSinglePermission(permission: String, componentActivity: ComponentActivity) {
        // This can be replaced with the launcher from the activity or a more generic implementation
        componentActivity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(componentActivity, "$permission granted", Toast.LENGTH_SHORT).show()
            } else {
                showPermissionDeniedMessage(componentActivity)
            }
        }.launch(permission)
    }

    private fun showPermissionDeniedMessage(activity: Activity) {
        Timber.tag(MY_TAG).i("Permission Denied")
        Toast.makeText(
            activity,
            "You must accept these permissions for this app to work!",
            Toast.LENGTH_LONG
        ).show()
    }

    fun requestPermissionAndExecuteAction(
        permissionLauncher: ActivityResultLauncher<String>,
        permission: String,
        activity: Activity,
        action: () -> Unit
    ) {
        permissionLauncher.launch(permission) // Launch the permission request

        // Check if the permission is already granted
        if (PermissionUtils.hasSinglePermission(permission, activity)) {
            // If granted, execute the action immediately
            action()
        } 
    }

    private fun onPermissionResult(isGranted: Boolean) {
        // Handle the permission result for specific actions
        if (isGranted) {
            Timber.tag(MY_TAG).i("Execution Permission Granted")
            // Add additional actions if needed
        } else {
            Timber.tag(MY_TAG).i("Execution Permission Denied")
        }
    }
}

// A data class to hold all the permission launchers
data class PermissionLaunchersDto(
    val multiplePermissionLauncher: ActivityResultLauncher<Array<String>>,
    val singlePermissionLauncher: ActivityResultLauncher<String>,
    val enableBluetoothLauncher: ActivityResultLauncher<Intent>,
    val discoverableLauncher: ActivityResultLauncher<Intent>,
    val locationPermissionLauncher: ActivityResultLauncher<String>,
    val onExecutionPermissionLauncher: ActivityResultLauncher<String>
)
