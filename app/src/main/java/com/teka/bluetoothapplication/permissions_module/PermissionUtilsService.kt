package com.teka.bluetoothapplication.permissions_module

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import timber.log.Timber


object PermissionUtils {

    fun askMultiplePermissions(
        multiplePermissionLauncher: ActivityResultLauncher<Array<String>>,
        requiredPermissions: Array<String>,
        context: Context,
        actionIfAlreadyGranted: (() -> Unit)? = null
    ) {
        if (!hasPermissions(requiredPermissions, context)) {
            //Launching multiple contract permission launcher for ALL the required permissions
            multiplePermissionLauncher.launch(requiredPermissions)
        } else {
            //All permissions are already granted
            if (actionIfAlreadyGranted != null) {
                actionIfAlreadyGranted()
            }
        }
    }

    fun askSinglePermission(
        singlePermissionLauncher: ActivityResultLauncher<String>,
        permission: String,
        context: Context,
        actionIfAlreadyGranted: (() -> Unit)? = null
    ) {
        if (!hasSinglePermission(permission, context)) {
            singlePermissionLauncher.launch(permission)
        } else {
            Timber.tag("Permission Utils").e(": (askSinglePermission) permission already provided")
            if (actionIfAlreadyGranted != null) {
                actionIfAlreadyGranted()
            }
        }
    }


    private var onPermissionGrantedAction: (() -> Unit)? = null

    fun requestPermissionAndExecuteAction(
        permissionLauncher: ActivityResultLauncher<String>,
        permission: String,
        context: Context,
        action: () -> Unit,
        actionIfDenied: (() -> Unit)? = null
    ) {
        onPermissionGrantedAction = action

        if (hasSinglePermission(permission, context)) {
            Timber.tag("Permission Utils").e(": permission already present")
            action()
        } else {
            Timber.tag("Permission Utils").e(": permission not granted yet")
            permissionLauncher.launch(permission)
        }
    }
    fun onPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            onPermissionGrantedAction?.invoke() // Execute the action if permission is granted
        } else {
            // Handle the case when the permission is denied
            // You can show a Toast or handle the logic as needed
        }
    }




    fun hasPermissions(permissions: Array<String>?, context: Context): Boolean {
        if (permissions != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    //Permission is not granted
                    return false
                }
                //Permission already granted
            }
            return true
        }
        return false
    }


    fun hasSinglePermission(permission: String, context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isLocationEnabled
    }

    fun enableLocation(activity: Activity) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        activity.startActivity(intent)
    }




}




