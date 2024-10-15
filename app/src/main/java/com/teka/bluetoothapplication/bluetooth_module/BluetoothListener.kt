package com.teka.bluetoothapplication.bluetooth_module

import android.bluetooth.BluetoothDevice

interface BluetoothListener {
    fun onDeviceFound(device: BluetoothDevice)
    fun onDiscoveryFinished()
    fun onBluetoothDisabled()
}
