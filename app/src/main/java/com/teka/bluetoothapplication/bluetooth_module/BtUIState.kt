package com.teka.bluetoothapplication.bluetooth_module

data class BtUIState(
    val connectedDevice: BtDeviceModel? = null,
    val scaleData: String = "",
    val connectionState: Boolean = false,
    val readingState: Boolean = false
)
