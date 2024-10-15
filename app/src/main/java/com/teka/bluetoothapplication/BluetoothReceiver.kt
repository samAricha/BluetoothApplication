package com.teka.bluetoothapplication

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter


class BluetoothReceiver(
    private val listener: BluetoothListener
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
        }
    }

    companion object {
        fun register(context: Context, listener: BluetoothListener): BluetoothReceiver {
            val receiver = BluetoothReceiver(listener)
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
            context.registerReceiver(receiver, filter)
            return receiver
        }

        fun unregister(context: Context, receiver: BluetoothReceiver) {
            context.unregisterReceiver(receiver)
        }
    }
}
