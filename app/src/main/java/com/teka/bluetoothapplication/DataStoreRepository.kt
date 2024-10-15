package com.teka.bluetoothapplication

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


val Context.loggedInDataStore: DataStore<Preferences> by preferencesDataStore(name = "logged_in_pref")

class DataStoreRepository(context: Context) {
    private val primaryDataStore = context.loggedInDataStore


    private object PreferencesKey {
        val BT_DEVICE_NAME = stringPreferencesKey(name = "bt_device_name")
        val BT_DEVICE_ADDRESS = stringPreferencesKey(name = "bt_device_address")
    }

    suspend fun saveConnectedBtDevice(btDeviceModel: BluetoothDeviceModel) {
        primaryDataStore.edit { preferences ->
            preferences[PreferencesKey.BT_DEVICE_ADDRESS] = btDeviceModel.name!!
            preferences[PreferencesKey.BT_DEVICE_ADDRESS] = btDeviceModel.address
        }
    }

    val getConnectedBtDevice: Flow<BluetoothDeviceModel> = primaryDataStore.data.map { preferences ->
        val btDeviceName = preferences[PreferencesKey.BT_DEVICE_NAME] ?: ""
        val btDeviceAddress = preferences[PreferencesKey.BT_DEVICE_ADDRESS] ?: ""

        BluetoothDeviceModel(name = btDeviceName, address = btDeviceAddress)
    }
}