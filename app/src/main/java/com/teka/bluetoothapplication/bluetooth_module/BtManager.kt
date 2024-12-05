package com.teka.bluetoothapplication.bluetooth_module

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import com.teka.bluetoothapplication.permissions_module.myUuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.cancellation.CancellationException

private const val BT_MNGR_TAG = "BT_MNGR_TAG"


class BtManager(private val context: Context) {

    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter =  bluetoothManager.adapter

    private var readingJob: Job? = null

    private val _scaleData: MutableStateFlow<String> = MutableStateFlow("0.0")
    val scaleData: StateFlow<String> = _scaleData
    private val _isReading: MutableStateFlow<Boolean> = MutableStateFlow<Boolean>(false)
    val isReading: StateFlow<Boolean> = _isReading

    private var bluetoothClientSocket: BluetoothSocket? = null
    private var bluetoothServerSocket: BluetoothServerSocket? = null
    // Add GATT-related variables for BLE
    private var bluetoothGatt: BluetoothGatt? = null


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

        when (device.type) {
            BluetoothDevice.DEVICE_TYPE_LE -> {
                Timber.tag(BT_MNGR_TAG).i("BTDEVICE:: BLE")
//                connectToBleDevice(device)
            }
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> {
                Timber.tag(BT_MNGR_TAG).i("BTDEVICE:: CLASSIC")
                connectToClassicDevice(device)
            }
            BluetoothDevice.DEVICE_TYPE_DUAL -> {
                Timber.tag(BT_MNGR_TAG).i("BTDEVICE:: DUAL")
                // Decide how to handle dual-mode devices
                if (useBleForDualDevice(device)) {
                    Timber.tag(BT_MNGR_TAG).i("BTDEVICE:: DUAL -> BLE")
                } else {
                    Timber.tag(BT_MNGR_TAG).i("BTDEVICE:: DUAL -> CLASSIC")
                    connectToDualModeDevice(device)
                }
            }
            else -> {
                Timber.tag(BT_MNGR_TAG).e("Unknown device type, TYPE::  ${device.type}")
            }
        }



        /*
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
        */

    }

    @SuppressLint("MissingPermission")
    private fun useBleForDualDevice(device: BluetoothDevice): Boolean {
        // Check the device name or other characteristics to determine the preferred connection type
        val deviceName = device.name ?: ""

        return when {
            deviceName.contains("BLE", ignoreCase = true) -> {
                Timber.tag(BT_MNGR_TAG).i("Device '$deviceName' supports BLE. Using BLE connection.")
                true // Use BLE for devices explicitly indicating BLE support
            }
            deviceName.contains("Classic", ignoreCase = true) -> {
                Timber.tag(BT_MNGR_TAG).i("Device '$deviceName' indicates Classic Bluetooth. Using Classic connection.")
                false // Use Classic Bluetooth for devices indicating Classic
            }
            else -> {
                Timber.tag(BT_MNGR_TAG).i("Device '$deviceName' type unknown, defaulting to BLE.")
                false // Default to BLE if unsure
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun connectToClassicDevice(device: BluetoothDevice) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("BluetoothServer", myUuid)
                bluetoothClientSocket = device.createRfcommSocketToServiceRecord(myUuid)
                bluetoothClientSocket?.connect()
                bluetoothClientSocket?.let { socket ->
                    readDataFromScale(socket)
                }
            } catch (e: IOException) {
                Timber.tag(BT_MNGR_TAG).e("Connection failed: ${e.localizedMessage}")
                closeBtConnection()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDualModeDevice(device: BluetoothDevice) {
        Timber.tag(BT_MNGR_TAG).i("Starting Client Mode")
        var clientSocket: BluetoothSocket? = null
        try {
            clientSocket = device.createRfcommSocketToServiceRecord(myUuid)
            clientSocket.connect()
            Timber.tag(BT_MNGR_TAG).i("Connected to server: $clientSocket")
            startContinuousDataStream(clientSocket)
        } catch (e: IOException) {
            Timber.tag(BT_MNGR_TAG).e("Error in client socket: ${e.localizedMessage}")
            clientSocket?.close()
        }
    }

    private fun startContinuousDataStream(clientSocket: BluetoothSocket) {
        val outStream = clientSocket.outputStream
        val inStream = clientSocket.inputStream

        // Create a thread to continuously send the command
        Thread(Runnable {
            try {
                while (!Thread.interrupted()) {
                    // Send the 'R' command (e.g., byte 82)
                    val cmd = byteArrayOf(82)
                    outStream.write(cmd)
                    outStream.flush()
                    Timber.tag(BT_MNGR_TAG).i("Sent 'R' command to scale")
                    // Wait for a short period before sending again
                    Thread.sleep(1000)
                    listenForData(inStream)
                }
            } catch (e: IOException) {
                Timber.tag(BT_MNGR_TAG).e("Error while sending/receiving data: ${e.localizedMessage}")
            } catch (e: InterruptedException) {
                Timber.tag(BT_MNGR_TAG).e("Thread interrupted: ${e.localizedMessage}")
            }
        }).start()
    }


    private fun listenForData(inputStream: InputStream) {
        val buffer = ByteArray(1024)

        try {
            // Read the response from the input stream
            val bytesRead = inputStream.read(buffer)
            if (bytesRead > 0) {
                val receivedData = String(buffer, 0, bytesRead)
                Timber.tag(BT_MNGR_TAG).i("Received data: $receivedData")
                if (receivedData.startsWith("W")) {
                    processWeightData(receivedData)
                }
            }
        } catch (e: IOException) {
            Timber.tag(BT_MNGR_TAG).e("Error reading data: ${e.localizedMessage}")
        }
    }

    private fun processWeightData(data: String) {
        Timber.tag(BT_MNGR_TAG).i("Processing weight data: $data")
        val weight = extractWeightFromData(data)
        Timber.tag(BT_MNGR_TAG).i("Weight extracted: $weight")
        _scaleData.value = weight
    }

    private fun extractWeightFromData(data: String): String {
        // Regular expression to match the weight value (e.g., "0.41", "12.34")
        val regex = """\d+(\.\d+)?""".toRegex()

        // Find the first match of the weight pattern in the data
        val match = regex.find(data)

        // Return the matched weight, or a default value if no match is found
        return match?.value ?: "0.00"
    }



    private fun readDataFromScale(socket: BluetoothSocket) {
        Timber.tag(BT_MNGR_TAG).i("start reading data from scale::socket = $socket")
        _isReading.value = true

        val buffer = ByteArray(1024)
        var bytes: Int
        val inputStream: InputStream

        try {
            inputStream = socket.inputStream
            Timber.tag(BT_MNGR_TAG).i("inside socket bytes try/catch 1:: inputStream -> $inputStream")
            val availableBytes = inputStream.available()
            Timber.tag(BT_MNGR_TAG).i("Available bytes: $availableBytes")

        } catch (e: IOException) {
            Timber.tag(BT_MNGR_TAG).e("Error getting input stream: ${e.localizedMessage}")
            return
        }



        Timber.tag(BT_MNGR_TAG).i("inputStream = $inputStream")
        // Read data continuously in a I/0 thread

        readingJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                while (isActive) {
                    if (socket.isConnected) {
                        Timber.tag(BT_MNGR_TAG).i("Socket is still connected")
                        try {
                            Timber.tag(BT_MNGR_TAG).i("Trying to read data from input stream ...")
                            Timber.tag(BT_MNGR_TAG).i("inputStream $inputStream ...")
                            try {
                                Timber.tag(BT_MNGR_TAG)
                                    .i("buffer content: ${buffer.joinToString()}")
                                val availableBytes = inputStream.available()
                                Timber.tag(BT_MNGR_TAG).i("Available bytes: $availableBytes")
                            } catch (e: Exception) {
                                Timber.tag(BT_MNGR_TAG).e("read exception: ${e.localizedMessage}")
                                Timber.tag(BT_MNGR_TAG).e("Exception stack trace $e")
                            }

                            bytes = withTimeout(2000) { // Timeout after 2 seconds if no data
                                inputStream.read(buffer)
                            }

                            Timber.tag(BT_MNGR_TAG).i("Bytes: $bytes")
                            if (bytes > 0) {
                                val data = String(buffer, 0, bytes).trim()
                                Timber.tag(BT_MNGR_TAG).i("data in = $data")
                                val cleanedData = data.filter { it.isDigit() || it == '.' }
                                if (cleanedData.isNotEmpty()) {
                                    Timber.tag(BT_MNGR_TAG).i("Valid data: $cleanedData")
                                    _scaleData.value = cleanedData
                                    Timber.tag(BT_MNGR_TAG).i("Scale data: ${scaleData.value}")
                                } else {
                                    Timber.tag(BT_MNGR_TAG).i("No valid data received")
                                }
                            } else {
                                Timber.tag(BT_MNGR_TAG).i("Read 0 bytes, retrying...")
                                delay(500)
                            }


                        } catch (e: TimeoutCancellationException) {
                            Timber.tag(BT_MNGR_TAG).i("Read operation timed out; trying again")
                        } catch (e: Exception) {
                            Timber.tag(BT_MNGR_TAG)
                                .e("reading bytes exception: ${e.localizedMessage}")
                            Timber.tag(BT_MNGR_TAG).i("reading data error: ${e.localizedMessage}")
                            e.printStackTrace()
                            socket.close()
                        }


                    } else {
                        Timber.tag(BT_MNGR_TAG).i("Socket disconnected or closed")
                        closeBtConnection()
                        break // Exit the loop if the connection is lost
                    }
                }
            } catch (e: IOException) {
                Timber.tag(BT_MNGR_TAG).e("Error during data reading: ${e.localizedMessage}")
                socket.close()
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

    @SuppressLint("MissingPermission")
    fun closeBtConnection() {
        if (bluetoothClientSocket?.isConnected == true) {
            Timber.tag(BT_MNGR_TAG).i("Socket is still connected; delaying close")
            return
        }
        Timber.tag(BT_MNGR_TAG).i("Stopping scale reading")
        _isReading.value = false
        readingJob?.cancel()
        readingJob = null

        try {
            // Close Bluetooth Classic connection
            bluetoothClientSocket?.close() // Close Bluetooth connection
            bluetoothClientSocket = null
            // Close BLE connection
            bluetoothGatt?.close()
            bluetoothGatt = null

        } catch (e: IOException) {
            Timber.tag(BT_MNGR_TAG).e("Error closing socket: ${e.localizedMessage}")
        }
    }

    fun stopReadingFromScale() {
        Timber.tag(BT_MNGR_TAG).i("Stopping scale reading")
        _isReading.value = false
        readingJob?.cancel()
        readingJob = null
    }

}


