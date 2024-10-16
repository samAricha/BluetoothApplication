package com.teka.bluetoothapplication.bluetooth_module

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import com.teka.bluetoothapplication.permissions_module.myUuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.io.InputStream

const val BT_MNGR_TAG = "BT_MNGR_TAG"


class BtManager(private val context: Context) {

    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter =  bluetoothManager.adapter

    private var readingJob: Job? = null

    private val _scaleData: MutableStateFlow<String> = MutableStateFlow("0.0")
    val scaleData: StateFlow<String> = _scaleData
    private val _isReading: MutableStateFlow<Boolean> = MutableStateFlow<Boolean>(false)
    val isReading: StateFlow<Boolean> = _isReading

    private var bluetoothSocket: BluetoothSocket? = null


    private val _connectionState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    private var btBroadcastReceiver: BtBroadcastReceiver? = null

    init {
        registerReceiver()
    }

    private fun registerReceiver() {
        btBroadcastReceiver = BtBroadcastReceiver(object : BtListener {
            @SuppressLint("MissingPermission")
            override fun onDeviceFound(device: BluetoothDevice) {
                Timber.tag(BT_MNGR_TAG).i("Device: ${device.name} found")

            }

            override fun onDiscoveryFinished() {
                Timber.tag(BT_MNGR_TAG).i("BT discovery finished")

            }

            override fun onBluetoothDisabled() {
                Timber.tag(BT_MNGR_TAG).i("BT disabled")
            }

            override fun onDeviceConnected(device: BluetoothDevice) {
                Timber.tag(BT_MNGR_TAG).i("BT device connected")
                _connectionState.value = true
            }

            @SuppressLint("MissingPermission")
            override fun onDeviceDisconnected(device: BluetoothDevice) {
                Timber.tag(BT_MNGR_TAG).i("Device disconnected: ${device.name}")
                // Emit false for connection state
                _connectionState.value = false
            }
        })

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        }
        context.registerReceiver(btBroadcastReceiver, filter)
    }

    fun unregisterReceiver() {
        btBroadcastReceiver?.let { context.unregisterReceiver(it) }
    }



    @SuppressLint("MissingPermission")
    fun startReadingFromScale(btDevice: BtDeviceModel) {
        Timber.tag(BT_MNGR_TAG).i("start reading from scale")
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Timber.tag(BT_MNGR_TAG).e("Bluetooth is not enabled or adapter is null")
            return
        }
        val device = bluetoothAdapter.getRemoteDevice(btDevice.address)
        Timber.tag(BT_MNGR_TAG).i("actual device connection: $device")
        readingJob = CoroutineScope(Dispatchers.IO).launch {

            try {
                device?.let {
                    bluetoothSocket = it.createRfcommSocketToServiceRecord(myUuid)
                    bluetoothSocket?.connect() // Establish connection
                    bluetoothSocket?.let { socket ->
                        readDataFromScale(socket) // Start reading data after connection
                    }
                }
            } catch (e: IOException) {
                Timber.tag(BT_MNGR_TAG).e("Connection failed: ${e.localizedMessage}")
                closeBtConnection()
            }
        }
    }

    private fun readDataFromScale(socket: BluetoothSocket) {
        Timber.tag(BT_MNGR_TAG).i("start reading data from scale::socket = $socket")
        _isReading.value = true

        val inputStream: InputStream
        try {
            inputStream = socket.inputStream
        } catch (e: IOException) {
            Timber.tag(BT_MNGR_TAG).e("Error getting input stream: ${e.localizedMessage}")
            return
        }

        val buffer = ByteArray(1024)
        var bytes: Int
        Timber.tag(BT_MNGR_TAG).i("inputStream = $inputStream")
        // Read data continuously in a background thread
        readingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    bytes = inputStream.read(buffer)
                    Timber.tag(BT_MNGR_TAG).i("Bytes in = $bytes")
                    // Convert the byte array to a string, handling only valid parts
                    val data = String(buffer, 0, bytes).trim()
                    Timber.tag(BT_MNGR_TAG).i("data in = $data")

                    // Filter out non-numeric characters (except for decimal points)
                    val cleanedData = data.filter { it.isDigit() || it == '.' }

                    // Check if the cleaned data looks like a valid weight
                    if (cleanedData.isNotEmpty()) {
                        Timber.tag(BT_MNGR_TAG).i("Valid data: $cleanedData")
                        _scaleData.value = cleanedData
                        Timber.tag(BT_MNGR_TAG).i("Scale data: ${scaleData.value}")
                    } else {
                        Timber.tag(BT_MNGR_TAG).i("No valid data received")
                    }



                } catch (e: IOException) {
                    Timber.tag(BT_MNGR_TAG).i("reading data error: ${ e.localizedMessage }")
                    e.printStackTrace()
                    socket.close()
                }
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

    fun closeBtConnection() {
        Timber.tag(BT_MNGR_TAG).i("Stopping scale reading")
        _isReading.value = false

        readingJob?.cancel() // Cancel the reading coroutine
        readingJob = null

        try {
            bluetoothSocket?.close() // Close Bluetooth connection
            bluetoothSocket = null
        } catch (e: IOException) {
            Timber.tag(BT_MNGR_TAG).e("Error closing socket: ${e.localizedMessage}")
        }
    }

    fun stopReadingFromScale() {
        Timber.tag(BT_MNGR_TAG).i("Stopping scale reading")

        _isReading.value = false
        // Update reading state
        readingJob?.cancel() // Cancel the reading coroutine
        readingJob = null
    }

}
