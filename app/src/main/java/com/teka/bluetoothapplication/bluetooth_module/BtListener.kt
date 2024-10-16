package com.teka.bluetoothapplication.bluetooth_module

import android.bluetooth.BluetoothDevice

interface BtListener {
    fun onDeviceFound(device: BluetoothDevice)
    fun onDiscoveryFinished()
    fun onBluetoothDisabled()
    fun onDeviceConnected(device: BluetoothDevice)
    fun onDeviceDisconnected(device: BluetoothDevice)
}
