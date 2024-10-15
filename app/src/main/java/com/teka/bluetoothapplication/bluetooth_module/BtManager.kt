package com.teka.bluetoothapplication.bluetooth_module

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.teka.bluetoothapplication.BluetoothDeviceModel
import com.teka.bluetoothapplication.permissions_module.myUuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

const val BT_MNGR_TAG = "BT_MNGR_TAG"


class BtManager(private val context: Context) {

    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter =  bluetoothManager.adapter


    // LiveData or StateFlow to push data updates
    val scaleData: MutableLiveData<String> = MutableLiveData() // Use StateFlow if preferred

    @SuppressLint("MissingPermission")
    fun startReadingFromScale(btDevice: BluetoothDeviceModel) {
        val device = bluetoothAdapter.getRemoteDevice(btDevice.address)
        device.let {
            val socket = it.createRfcommSocketToServiceRecord(myUuid)
            socket.connect() // Establish connection
            readDataFromScale(socket)
        }
    }

    private fun readDataFromScale(socket: BluetoothSocket) {
        val inputStream = socket.inputStream
        val buffer = ByteArray(1024) // Buffer to store incoming data
        var bytes: Int

        // Read data continuously in a background thread
        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                bytes = inputStream.read(buffer)
                val data = String(buffer, 0, bytes)
                scaleData.postValue(data) // Post data to LiveData or StateFlow
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getPairedDevice(): BluetoothDevice? {
        val pairedDevices = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            if (device.name == "YourBTScaleName") {
                return device
            }
        }
        return null
    }

    fun stopReadingFromScale() {
        // Logic to close the connection and stop reading
    }
}
