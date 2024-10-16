package com.teka.bluetoothapplication

data class BluetoothUIState(
    val connectedDevice: BluetoothDeviceModel? = null,
    val scaleData: String = "",
    val connectionState: Boolean = false
)
