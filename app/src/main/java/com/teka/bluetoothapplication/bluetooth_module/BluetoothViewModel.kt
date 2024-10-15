package com.teka.bluetoothapplication.bluetooth_module

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.teka.bluetoothapplication.BluetoothDeviceModel
import com.teka.bluetoothapplication.permissions_module.MY_TAG
import dagger.hilt.android.lifecycle.HiltViewModel

import timber.log.Timber
import javax.inject.Inject

const val BT_VM_TAG = "BT_VM_TAG"


@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val applicationContext: Context,
) : ViewModel(), BluetoothListener {

    val bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter =  bluetoothManager.adapter

    private val _discoveredDevices = MutableLiveData<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: LiveData<List<BluetoothDevice>> get() = _discoveredDevices

    private val _pairedDevices = MutableLiveData<List<BluetoothDevice>>(emptyList())
    val pairedDevices: LiveData<List<BluetoothDevice>> get() = _pairedDevices


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


    @SuppressLint("MissingPermission")
    fun startBluetoothService() {
        changeStateOfConnectivity(StatesOfConnection.CLIENT_STARTED)
        bluetoothAdapter.cancelDiscovery()
        _selectedDevice.value?.let {
            ConnectThread(it, this).start()
        }
    }

    @SuppressLint("MissingPermission")
    fun getListOfPairedBluetoothDevices(): MutableList<BluetoothDeviceModel>? {
        val list = mutableListOf<String>()

        var setOfPairairedDevices: Set<BluetoothDevice> = mutableSetOf()
        var pairedDevices:  MutableList<BluetoothDeviceModel> = mutableStateListOf()
        setOfPairairedDevices = bluetoothAdapter.bondedDevices ?: return null


        if (bluetoothAdapter.isEnabled) {
            for (pairedDevice in setOfPairairedDevices) {
                list.add(pairedDevice.name + "\t" + pairedDevice.address)
                val btDeviceModel = BluetoothDeviceModel(name = pairedDevice.name, address =  pairedDevice.address)
                pairedDevices.add(btDeviceModel)
            }
        }

        Timber.tag("$BT_VM_TAG::getListOfPairedBluetoothDevices").i("paired bt devices: $list")
        return pairedDevices
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

    override fun onDeviceFound(device: BluetoothDevice) {
        Timber.tag(BT_VM_TAG).i("device found: $device")
    }

    override fun onDiscoveryFinished() {
        Timber.tag(BT_VM_TAG).i("DISCOVERY FINISHED")
    }

    override fun onBluetoothDisabled() {
        Timber.tag(BT_VM_TAG).i("BT DISABLED")
    }


}

