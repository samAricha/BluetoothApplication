package com.teka.bluetoothapplication.permissions_module

import android.Manifest
import android.os.Build
import java.util.*

//For a RFComm connection to exist we use a hardcoded UUID
val myUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
//tag used for logging purposes
const val MY_TAG = "MY_TAG"


val requiredPermissionsInitialClient =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE

        )
    }