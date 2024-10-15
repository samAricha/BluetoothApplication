package com.teka.bluetoothapplication

import android.bluetooth.BluetoothDevice

interface BluetoothListener {
    fun onDeviceFound(device: BluetoothDevice)
    fun onDiscoveryFinished()
    fun onBluetoothDisabled()
}
