package com.teka.bluetoothapplication

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.teka.bluetoothapplication.bluetooth_module.BtDeviceModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber

const val DS_REPOSITORY_TAG = "DS_REPOSITORY_TAG"


val Context.loggedInDataStore: DataStore<Preferences> by preferencesDataStore(name = "logged_in_pref")

class DataStoreRepository(context: Context) {
    private val primaryDataStore = context.loggedInDataStore


    private object PreferencesKey {
        val BT_DEVICE_NAME = stringPreferencesKey(name = "bt_device_name")
        val BT_DEVICE_ADDRESS = stringPreferencesKey(name = "bt_device_address")
    }

    suspend fun saveConnectedBtDevice(btDeviceModel: BtDeviceModel) {
        Timber.tag(DS_REPOSITORY_TAG).i("saveBtDevice: $btDeviceModel")
        primaryDataStore.edit { preferences ->
            preferences.remove(PreferencesKey.BT_DEVICE_NAME)
            preferences.remove(PreferencesKey.BT_DEVICE_ADDRESS)

            preferences[PreferencesKey.BT_DEVICE_NAME] = btDeviceModel.name!!
            preferences[PreferencesKey.BT_DEVICE_ADDRESS] = btDeviceModel.address
        }

        // Fetch saved data to confirm
        val savedData = primaryDataStore.data.first()
        val savedName = savedData[PreferencesKey.BT_DEVICE_NAME]
        val savedAddress = savedData[PreferencesKey.BT_DEVICE_ADDRESS]

        Timber.tag(DS_REPOSITORY_TAG).i("Confirmed saved BT Device: Name = $savedName, Address = $savedAddress")
    }

    val getConnectedBtDevice: Flow<BtDeviceModel?> = primaryDataStore.data.map { preferences ->
        val btDeviceName = preferences[PreferencesKey.BT_DEVICE_NAME]
        val btDeviceAddress = preferences[PreferencesKey.BT_DEVICE_ADDRESS]

        // Return BluetoothDeviceModel only if both values are present
        if (btDeviceName != null && btDeviceAddress != null) {
            BtDeviceModel(name = btDeviceName, address = btDeviceAddress)
        } else {
            null
        }
    }
}