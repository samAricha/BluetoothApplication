package com.teka.bluetoothapplication.bluetooth_module

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teka.bluetoothapplication.DataStoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import timber.log.Timber
import javax.inject.Inject

const val BT_VM_TAG = "BT_VM_TAG"


@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val applicationContext: Context,
    private val dataStoreRepository: DataStoreRepository,
) : ViewModel(){

    private val btManager: BtManager = BtManager(applicationContext)
    private val bluetoothAdapter =  btManager.bluetoothAdapter
    val scaleData: StateFlow<String> = btManager.scaleData
    val isReadingData: StateFlow<Boolean> = btManager.isReading


    private val _uiState = MutableStateFlow(BtUIState())
    val uiState: StateFlow<BtUIState> = _uiState

    val btDeviceConnectionState: StateFlow<Boolean> = btManager.connectionState


    init {
        observeConnectedDeviceFromDataStore()
        observeScaleData()
        observeConnectionState()
        observeReadingState()
    }


    private val _discoveredDevices = MutableLiveData<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: LiveData<List<BluetoothDevice>> get() = _discoveredDevices

    private val _pairedDevices = MutableLiveData<List<BluetoothDevice>>(emptyList())
    val pairedDevices: LiveData<List<BluetoothDevice>> get() = _pairedDevices


    private val _scanningForDevices = MutableLiveData(false)
    val scanningForDevices: LiveData<Boolean> get() = _scanningForDevices

    private val _selectedDevice = MutableLiveData<BluetoothDevice?>()
    val selectedDevice: LiveData<BluetoothDevice?> get() = _selectedDevice



    private fun observeConnectedDeviceFromDataStore() {
        viewModelScope.launch {
            dataStoreRepository.getConnectedBtDevice.collectLatest { device ->
                _uiState.value = _uiState.value.copy(connectedDevice = device)
            }
        }
    }

    private fun observeScaleData() {
        viewModelScope.launch {
            scaleData.collectLatest { scaleData ->
                Timber.tag(BT_VM_TAG).i("collecting scale data: $scaleData")
                _uiState.value = _uiState.value.copy(scaleData = scaleData)
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            btDeviceConnectionState.collectLatest { connectionState ->
                Timber.tag(BT_VM_TAG).i("connection state: $connectionState")
                _uiState.value = _uiState.value.copy(connectionState = connectionState)
            }
        }
    }

    private fun observeReadingState() {
        viewModelScope.launch {
            isReadingData.collectLatest { readingState ->
                Timber.tag(BT_VM_TAG).i("reading state: $readingState")
                _uiState.value = _uiState.value.copy(readingState = readingState)
            }
        }
    }




    fun startBluetoothConnection() {
        viewModelScope.launch {
            dataStoreRepository.getConnectedBtDevice.collectLatest { device ->
                Timber.tag(BT_VM_TAG).i("device: $device")
                if (device != null) {
                    val deviceName = device.name
                    val deviceAddress = device.address
                    Timber.tag(BT_VM_TAG).i("Device Name: $deviceName, Device Address: $deviceAddress")
                    try {
                        btManager.startReadingFromScale(device)
                    }catch (e: Exception){
                        Timber.tag(BT_VM_TAG).i("BT connection failed: ${e.localizedMessage}")
                    }
                } else {
                    Toast.makeText(applicationContext, "No Device Connected", Toast.LENGTH_SHORT).show()
                    Timber.tag(BT_VM_TAG).i("No connected Bluetooth device found in datastore.")
                }
            }
        }
    }

    suspend fun saveConnectedBtDevice(btDeviceModel: BtDeviceModel) {
            dataStoreRepository.saveConnectedBtDevice(btDeviceModel)
    }

    fun stopBluetoothConnection() {
        btManager.closeBtConnection()
        btManager.unregisterReceiver()
    }

    fun stopBtReading() {
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
    fun getListOfPairedBluetoothDevices(): MutableList<BtDeviceModel>? {
        val list = mutableListOf<String>()

        var setOfPairairedDevices: Set<BluetoothDevice> = mutableSetOf()
        var pairedDevices:  MutableList<BtDeviceModel> = mutableStateListOf()
        setOfPairairedDevices = bluetoothAdapter.bondedDevices ?: return null


        if (bluetoothAdapter.isEnabled) {
            for (pairedDevice in setOfPairairedDevices) {
                list.add(pairedDevice.name + "\t" + pairedDevice.address)
                val btDeviceModel = BtDeviceModel(name = pairedDevice.name, address =  pairedDevice.address)
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

}

