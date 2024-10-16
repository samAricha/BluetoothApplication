package com.teka.bluetoothapplication.bluetooth_module

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import timber.log.Timber

const val BT_BR_TAG = "BT_BR_TAG"

class BtBroadcastReceiver(
    private val listener: BtListener
) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_FOUND -> {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device != null && device.name != null) {
                    listener.onDeviceFound(device) // Notify listener of found device
                }
            }
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                listener.onDiscoveryFinished() // Notify listener that discovery is finished
            }
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                if (BluetoothAdapter.STATE_OFF == intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    listener.onBluetoothDisabled()                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> { // Listen for disconnection events
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                Timber.tag(BT_BR_TAG).i("DEVICE: $device DISCONNECTED")
                device?.let {
                    listener.onDeviceDisconnected(it)
                }
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                Timber.tag(BT_BR_TAG).i("DEVICE: $device CONNECTED")
                device?.let {
                    listener.onDeviceConnected(it)
                }
            }
        }
    }

    companion object {
        fun register(context: Context, listener: BtListener): BtBroadcastReceiver {
            val receiver = BtBroadcastReceiver(listener)
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
            context.registerReceiver(receiver, filter)
            return receiver
        }

        fun unregister(context: Context, receiver: BtBroadcastReceiver) {
            context.unregisterReceiver(receiver)
        }
    }
}
