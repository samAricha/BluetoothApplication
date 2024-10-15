package com.teka.bluetoothapplication.bluetooth_module

class ScaleDataStreamEvent {
    private var data: Double? = null

    constructor(data: Double?) {
        this.data = data
    }

    fun getData(): Double? {
        return data
    }
}