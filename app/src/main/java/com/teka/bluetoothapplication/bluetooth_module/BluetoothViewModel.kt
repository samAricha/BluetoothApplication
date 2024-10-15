package com.teka.bluetoothapplication.bluetooth_module

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel

import timber.log.Timber
import javax.inject.Inject

const val BT_VM_TAG = "BT_VM_TAG"


@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val applicationContext: Context,
) : ViewModel() {

    val bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter =  bluetoothManager.adapter

    private val _discoveredDevices = MutableLiveData<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: LiveData<List<BluetoothDevice>> get() = _discoveredDevices


    private val _scanningForDevices = MutableLiveData(false)
    val scanningForDevices: LiveData<Boolean> get() = _scanningForDevices

    private val _selectedDevice = MutableLiveData<BluetoothDevice?>()
    val selectedDevice: LiveData<BluetoothDevice?> get() = _selectedDevice


    private val _displayedText = MutableLiveData<String>()
    val displayedText: LiveData<String> get() = _displayedText

    private val _buttonText = MutableLiveData<String>()
    val buttonText: LiveData<String> get() = _buttonText

    private val _image = MutableLiveData<Int>()
    val image: LiveData<Int> get() = _image

    private val _connectionState = MutableLiveData<StatesOfConnection>()
    val connectionState: LiveData<StatesOfConnection> get() = _connectionState

    private val _buttonAction = MutableLiveData<(() -> Unit)?>()
    val buttonAction: LiveData<(() -> Unit)?> get() = _buttonAction


    fun addDiscoveredDevice(device: BluetoothDevice) {
        val currentDevices = _discoveredDevices.value ?: emptyList()
        if (!currentDevices.contains(device)) {
            _discoveredDevices.value = currentDevices + device
        }
    }

    fun scanningFinished() {
        _scanningForDevices.value = false
    }


    @SuppressLint("MissingPermission")
    fun scanForDevices() {
        _scanningForDevices.value = true
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        bluetoothAdapter.startDiscovery()
        Timber.tag(BT_VM_TAG).i("discovery started")
    }

    fun selectDevice(device: BluetoothDevice) {
        _selectedDevice.value = device
    }

    /**
     * Connect to the selected device and start a server socket connection
     * and then listen to data from connected socket
     */
    @SuppressLint("MissingPermission")
    fun startBluetoothService() {
        changeStateOfConnectivity(StatesOfConnection.CLIENT_STARTED)
        bluetoothAdapter.cancelDiscovery()
        _selectedDevice.value?.let {
            ConnectThread(it, this).start()
        }
    }

    @SuppressLint("MissingPermission")
    fun getListOfPairedBluetoothDevices(): MutableList<String>? {
        val list = mutableListOf<String>()

        var mPairedDevices: Set<BluetoothDevice> = mutableSetOf()

        mPairedDevices = bluetoothAdapter.bondedDevices ?: return null

        if (bluetoothAdapter.isEnabled) {
            // put it's one to the adapter
            for (pairedDevice in mPairedDevices)
                list.add(pairedDevice.name + "\t" + pairedDevice.address)
        }

        Timber.tag("MAVM::getListOfPairedBluetoothDevices").i("paired bt devices: $list")
        return list
    }



    @SuppressLint("MissingPermission")
    fun changeStateOfConnectivity(
        newState: StatesOfConnection,
        dataReceived: String? = null
    ) {
        _connectionState.value = newState
        when (newState) {
            StatesOfConnection.CLIENT_STARTED -> {
                _displayedText.value =
                    "Listening for data from the device: ${_selectedDevice.value?.name}"
                _buttonText.value = ""
                _image.value = 0
            }

            StatesOfConnection.RESPONSE_RECEIVED -> {
                when (dataReceived) {

                    else -> {
                        _displayedText.value = "Not correct response message: $dataReceived"
                    }
                }
                _buttonText.value = ""
            }

            StatesOfConnection.ERROR -> {
                _buttonText.value = "Restart server?"
                _buttonAction.value = { startBluetoothService() }
                _displayedText.value = "An error occurred: $dataReceived"
                _image.value = 0
            }
        }
    }


    fun isBluetoothEnabled(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter?.isEnabled == true
    }

}

