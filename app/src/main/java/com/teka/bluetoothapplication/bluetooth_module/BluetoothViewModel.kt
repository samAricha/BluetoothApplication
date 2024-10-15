package com.teka.bluetoothapplication.bluetooth_module

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teka.bluetoothapplication.BluetoothDeviceModel
import com.teka.bluetoothapplication.DataStoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import timber.log.Timber
import javax.inject.Inject

const val BT_VM_TAG = "BT_VM_TAG"


@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val applicationContext: Context,
    private val dataStoreRepository: DataStoreRepository,
) : ViewModel(), BluetoothListener {

    private val btManager: BtManager = BtManager(applicationContext)
    private val bluetoothAdapter =  btManager.bluetoothAdapter


    private val _discoveredDevices = MutableLiveData<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: LiveData<List<BluetoothDevice>> get() = _discoveredDevices

    private val _pairedDevices = MutableLiveData<List<BluetoothDevice>>(emptyList())
    val pairedDevices: LiveData<List<BluetoothDevice>> get() = _pairedDevices


    private val _scanningForDevices = MutableLiveData(false)
    val scanningForDevices: LiveData<Boolean> get() = _scanningForDevices

    private val _selectedDevice = MutableLiveData<BluetoothDevice?>()
    val selectedDevice: LiveData<BluetoothDevice?> get() = _selectedDevice


    private val _connectionState = MutableLiveData<StatesOfConnection>()
    val connectionState: LiveData<StatesOfConnection> get() = _connectionState



    // Expose the LiveData (or StateFlow) to the UI
    val btScaleData: LiveData<String> = btManager.scaleData


    fun startBluetoothConnection() {
        viewModelScope.launch {
            dataStoreRepository.getConnectedBtDevice.collectLatest { device ->
                if (device != null) {
                    val deviceName = device.name
                    val deviceAddress = device.address
                    Timber.tag(BT_VM_TAG).i("Device Name: $deviceName, Device Address: $deviceAddress")
                    btManager.startReadingFromScale(device)
                } else {
                    Timber.tag(BT_VM_TAG).i("No connected Bluetooth device found.")
                }
            }
        }
    }

    fun stopBluetoothConnection() {
        btManager.stopReadingFromScale()
    }


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

