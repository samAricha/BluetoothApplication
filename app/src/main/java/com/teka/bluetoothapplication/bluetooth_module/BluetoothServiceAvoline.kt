package com.teka.bluetoothapplication.bluetooth_module

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.regex.Pattern


class BluetoothServiceAvoline : Service() {
    fun getDataLiveData(): LiveData<Double>? {
        return mConnectedThread?.getDataLiveData()
    }
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothDevice: BluetoothDevice? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private val binder: IBinder = LocalBinder()
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private val BTMODULEUUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // "random" unique identifier
    private var mHandler // Our main handler that will receive callback notifications
            : Handler? = null
    private var mConnectedThread // bluetooth background worker thread to send and receive data
            : ConnectedThread? = null


    private val MESSAGE_READ = 2 // used in bluetooth handler to identify message update

    private val CONNECTING_STATUS = 3 // used in bluetooth handler to identify message status



    private class ConnectedThread(
        private val connectedBluetoothSocket: BluetoothSocket,
        private val scaleType: Int
    ) : Thread() {
        private val connectedInputStream: InputStream?
        private val connectedOutputStream: OutputStream?
        var platform = true
        var bridgeScale = false

        var group_number = 1
        private val dataLiveData = MutableLiveData<Double>()

        fun getDataLiveData(): LiveData<Double> {
            return dataLiveData
        }
        // private val regex = Pattern.compile("(\\d+(?:\\.\\d+)?)")//Bedan's way, but can be reverted in case of any issues
        private val regex = Pattern.compile("\\d+\\.\\d+")
        private val bridgeRegex = Pattern.compile("(\\+\\w{9}\\b)")

        init {
            var `in`: InputStream? = null
            var out: OutputStream? = null

            try {
                `in` = connectedBluetoothSocket.inputStream
                out = connectedBluetoothSocket.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }

            connectedInputStream = `in`
            connectedOutputStream = out

            if(scaleType == 0){
                platform = true
                bridgeScale = false
            }else{
                platform = false
                bridgeScale = true
            }

        }

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (true) {
                try {
                    if (platform || bridgeScale)
                        sleep(500)
                    bytes = connectedInputStream!!.read(buffer)//ST,GS,    38.0kg

                    //kq,ST, 58.0KG

                    var strReceived = String(buffer, 0, bytes)
                    Timber.tag("RECEIVED").e(strReceived)
                    Timber.tag("AFTER REGEX").e(regex.matcher(strReceived).toString())

                    if (platform){

                        if (strReceived.contains("ST") && strReceived.contains("kg")) {
                            group_number = 1
                            val list = strReceived.split("kg", ignoreCase = true).filter { it.contains("ST") }
                            if(list.isNotEmpty()){
                                val candidate = list.first()
                                strReceived = candidate
                                Timber.tag("RECEIVED CANDIDATES").e(candidate)
                            }
                        }



                        if ((strReceived.contains("GS") && strReceived.contains("KG")) ) {
                            group_number = 0
                            val list = strReceived.split("kg", ignoreCase = true).filter { it.contains("GS") }
                            if(list.isNotEmpty()){
                                val candidate = list.first()
                                strReceived = candidate
                                Timber.tag("RECEIVED CANDIDATES").e(candidate)
                            }
                        }
                    }

                    if (bridgeScale){
                        val matcher = bridgeRegex.matcher(strReceived)
                        if (matcher.find()) {
                            val nominee = matcher.group(1)
                            strReceived = nominee.substring(1, nominee.length - 3)
                            Timber.tag("RECEIVED CANDIDATES").e(strReceived)
                        }else{
                            continue
                        }
                    }


                    if(strReceived.trim().replace("\u0002","").replace("\u0003","").isNotEmpty())
                        if(strReceived.contains("\u0002") || strReceived.contains("\u0003") || strReceived.trim().isNumeric()){
                            group_number = 0
                        }
                    //runOnUiThread {
                    CoroutineScope(Dispatchers.IO).launch {
                        val matcher = regex.matcher(strReceived)
                        if (matcher.find()) {

                            if(matcher.groupCount() > 1)
                                group_number = 1

                            try {
                                val item = matcher.group(group_number)
                                // quantity.text = round(item.toDouble())
                                val value = item.toDouble()
                                dataLiveData.postValue(value)
                                Timber.tag("ACTUAL_SCALE_VALUE").d(value.toString())
                                EventBus.getDefault().post(ScaleDataStreamEvent(value))

                            }catch (e:IndexOutOfBoundsException){
                                //Do Nothing
                            }

                        }
                    }

                    //}

                } catch (e: IOException) {
                    e.printStackTrace()
//                    runOnUiThread {
//                        if(!done) Toast.makeText(this@ScaleActivity, "Connection lost", Toast.LENGTH_SHORT).show()
//                        recycler.adapter = deviceAdapter
//                        recycler.visibility = View.VISIBLE
//                    }
                    cancel()
                    break
                }

            }
        }

        fun String.isNumeric(): Boolean {
            val regex = "-?[0-9]+(\\.[0-9]+)?".toRegex()
            return this.matches(regex)
        }

        fun write(buffer: ByteArray) {
            try {
                connectedOutputStream!!.write(buffer)
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        fun cancel() {
            try {
                connectedBluetoothSocket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }



    inner class LocalBinder : Binder() {
        val service: BluetoothServiceAvoline
            get() = this@BluetoothServiceAvoline
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
       // toast("Receiving")
        val deviceAddress = intent.getStringExtra("device_address")
        val scale_type = intent.getIntExtra("scale_type",0)
        connectToDevice(deviceAddress,scale_type)
        return START_STICKY
    }

    private fun connectToDevice(deviceAddress: String?, scale_type: Int) {
        bluetoothDevice = bluetoothAdapter?.getRemoteDevice(deviceAddress);

        mHandler = @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(msg: Message) {
                if (msg.what == MESSAGE_READ) {
                    var readMessage: String? = null
                    try {
                        readMessage = String((msg.obj as ByteArray), StandardCharsets.UTF_8)
                        Log.i("handleMessage: ", "" + msg.obj as ByteArray)
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                    }
                    if (readMessage != null) {
                        //Sapration(readMessage)
                    }
                }
                if (msg.what == CONNECTING_STATUS) {
                    if (msg.arg1 == 1) {
                        val toast = Toast.makeText(
                            applicationContext,
                            "Successfully connected to Device",
                            Toast.LENGTH_LONG
                        )
                        toast.show()
                    } else {
                        val toast = Toast.makeText(
                            applicationContext,
                            "Connection Failed",
                            Toast.LENGTH_LONG
                        )
                        toast.show()
                    }
                    //mBluetoothStatus.setText("Connection Failed");
                }
            }
        }

        // Spawn a new thread to avoid blocking the GUI one
        object : Thread() {
            @SuppressLint("MissingPermission")
            override fun run() {
                var fail = false
                val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(deviceAddress)
                try {
                    bluetoothSocket = device?.let { createBluetoothSocket(it) }
                } catch (e: IOException) {
                    fail = true
                    Handler(Looper.getMainLooper()).post {
                        // TODO: Without Handler Looper, you get this error: java.lang.NullPointerException: Can't toast on a thread that has not called Looper.prepare() 
                        Toast.makeText(baseContext, "Socket creation failed", Toast.LENGTH_SHORT).show()
                    }
                }
                // Establish the Bluetooth socket connection.
                try {
                    bluetoothSocket?.connect()
                } catch (e: IOException) {
                    try {
                        fail = true
                        bluetoothSocket?.close()
                        mHandler?.obtainMessage(CONNECTING_STATUS, -1, -1)
                            ?.sendToTarget()
                    } catch (e2: IOException) {
                        //insert code to deal with this
                        Toast.makeText(baseContext, "Socket creation failed", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                if (fail == false) {
                    mConnectedThread =
                        bluetoothSocket?.let { ConnectedThread(it, scale_type) }
                    mConnectedThread?.start()
                    mHandler?.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                        ?.sendToTarget()
                }
            }
        }.start()

    }

    private fun connectToDevice2(deviceAddress: String?, scale_type: Int) {
        bluetoothDevice = bluetoothAdapter?.getRemoteDevice(deviceAddress)

        // Check for Bluetooth connect permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // Handle permission request (this part should ideally be in your Activity)
            return
        }

        // mHandler for handling UI thread updates
        mHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (msg.what == MESSAGE_READ) {
                    val readMessage: String? = try {
                        String((msg.obj as ByteArray), StandardCharsets.UTF_8)
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                        null
                    }
                    readMessage?.let {
                        //Sapration(readMessage)
                    }
                }
                if (msg.what == CONNECTING_STATUS) {
                    val toastMessage = if (msg.arg1 == 1) {
                        "Successfully connected to Device"
                    } else {
                        "Connection Failed"
                    }
                    Toast.makeText(applicationContext, toastMessage, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Coroutine to manage threading
        CoroutineScope(Dispatchers.IO).launch {
            var fail = false
            val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(deviceAddress)

            // Create Bluetooth socket
            try {
                bluetoothSocket = device?.let { createBluetoothSocket(it) }
            } catch (e: IOException) {
                fail = true
                // Use mHandler to post UI changes on the main thread
                mHandler?.post {
                    Toast.makeText(baseContext, "Socket creation failed", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            // Establish the Bluetooth socket connection
            try {
                bluetoothSocket?.connect()
            } catch (e: IOException) {
                fail = true
                try {
                    bluetoothSocket?.close()
                } catch (e2: IOException) {
                    mHandler?.post {
                        Toast.makeText(baseContext, "Failed to close socket", Toast.LENGTH_SHORT).show()
                    }
                }

                // Notify failure
                mHandler?.obtainMessage(CONNECTING_STATUS, -1, -1)?.sendToTarget()
                return@launch
            }

            // If connection successful, start the connected thread
            if (!fail) {
                mConnectedThread = bluetoothSocket?.let { ConnectedThread(it, scale_type) }
                mConnectedThread?.start()
                mHandler?.obtainMessage(CONNECTING_STATUS, 1, -1, deviceAddress)?.sendToTarget()
            }
        }
    }



    @SuppressLint("MissingPermission")
    @kotlin.jvm.Throws(IOException::class)
    private fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket? {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID)
        //creates secure outgoing connection with BT device using UUID
    }

    fun sendData(data: ByteArray?) {
        try {
            outputStream!!.write(data)
        } catch (e: IOException) {
            Timber.tag(TAG).e("Error sending data: %s", e.message)
            // Handle data send failure
        }
    }

    fun receiveData(): ByteArray? {
        val buffer = ByteArray(1024)
        val bytesRead: Int
        try {
            bytesRead = inputStream!!.read(buffer)
            return buffer
        } catch (e: IOException) {
            Log.e(TAG, "Error receiving data: " + e.message)
            // Handle data receive failure
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectFromDevice()
    }

    private fun disconnectFromDevice() {
        try {
            if (inputStream != null) inputStream!!.close()
            if (outputStream != null) outputStream!!.close()
            if (bluetoothSocket != null) bluetoothSocket!!.close()
            Log.d(TAG, "Disconnected from device")
        } catch (e: IOException) {
            Log.e(TAG, "Error closing Bluetooth connection: " + e.message)
        }
    }

    companion object {
        private const val TAG = "BluetoothService"
    }
}
