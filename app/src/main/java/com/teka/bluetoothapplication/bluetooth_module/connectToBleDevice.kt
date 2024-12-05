package com.teka.bluetoothapplication.bluetooth_module;

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import timber.log.Timber

@SuppressLint("MissingPermission")
private const val BLE_TAG = "BLE_TAG"


class BLEconnection(private val context: Context) {
    private var bluetoothGatt: BluetoothGatt? = null


    private fun connectToBleDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }


    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Timber.tag(BLE_TAG).i("Connected to BLE device")
                    bluetoothGatt?.discoverServices() // Discover services
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Timber.tag(BLE_TAG).i("Disconnected from BLE device")
//                    closeBtConnection()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Read from the characteristic here, if available
                // Example: readCharacteristic(characteristic)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value
                val cleanedData = String(data).filter { it.isDigit() || it == '.' }
//                _scaleData.value = cleanedData
                Timber.tag(BLE_TAG).i("Scale data: $cleanedData")
            }
        }
    }


    @SuppressLint("MissingPermission")
    fun connectToBleDeviceGatt(device: BluetoothDevice) {
        Timber.tag(BLE_TAG).i("Connecting to BLE device: ${device.name}")

        bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Timber.tag(BLE_TAG).i("Connected to GATT server.")
                        // Discover services after successful connection.
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Timber.tag(BLE_TAG).i("Disconnected from GATT server.")
                        bluetoothGatt?.close()
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Services discovered successfully, you can read/write characteristics here
                    Timber.tag(BLE_TAG).i("Services discovered: ${gatt.services}")
                } else {
                    Timber.tag(BLE_TAG).e("Failed to discover services.")
                }
            }
        })
    }
}