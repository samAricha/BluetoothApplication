package com.teka.bluetoothapplication.bluetooth_module

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.teka.bluetoothapplication.permissions_module.myUuid
import timber.log.Timber
import java.io.IOException

const val BT_CONNECTIVITY_TAG = "BT_CONNECTIVITY_TAG"

class BluetoothService(
    private val socket: BluetoothSocket,
    private val viewModel: BluetoothViewModel
) : Thread() {
    private val inputStream = socket.inputStream

    //We only need 1Byte for reading 0 or 1 from raspberry result
    private val buffer = ByteArray(1)
    override fun run() {
        // Keep listening to the InputStream until an exception occurs.
        while (true) {
            try {
                //Read from the InputStream
                inputStream.read(buffer)
            } catch (e: IOException) {
                Timber.tag(BT_CONNECTIVITY_TAG).i(e, "Input stream was disconnected")
                break
            }
            // Send the obtained bytes to the UI activity.
            val text = String(buffer)
            viewModel.changeStateOfConnectivity(
                newState = StatesOfConnection.RESPONSE_RECEIVED,
                dataReceived = text
            )
        }
    }
}


@SuppressLint("MissingPermission")
class ConnectThread(
    device: BluetoothDevice,
    private val viewModel: BluetoothViewModel
) : Thread() {
    private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
        //device.createInsecureRfcommSocketToServiceRecord(myUuid)
        device.createRfcommSocketToServiceRecord(myUuid)
    }

    override fun run() {
        mmSocket?.let { socket ->
            //Connect to the remote device through the socket.
            // This call blocks until it succeeds or throws an exception
            try {
                Timber.tag(BT_CONNECTIVITY_TAG).i("attempting connection")
                socket.connect()
                Timber.tag(BT_CONNECTIVITY_TAG).i("connection success")
            } catch (e: Exception) {
                Timber.tag(BT_CONNECTIVITY_TAG).i("connection was not successful")
                viewModel.changeStateOfConnectivity(
                    StatesOfConnection.ERROR,
                    "Error on connectivity: $e"
                )
            }
            //The connection attempt succeeded.
            //Perform work associated with the connection in a separate thread
            BluetoothService(socket, viewModel).start()
        }
    }

    // Closes the connect socket and causes the thread to finish.
    fun cancel() {
        try {
            mmSocket?.close()
        } catch (e: IOException) {
            Timber.tag(BT_CONNECTIVITY_TAG).e(e, "Could not close the connect socket")
        }
    }
}