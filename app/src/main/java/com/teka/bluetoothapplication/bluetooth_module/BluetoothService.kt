package com.teka.bluetoothapplication.bluetooth_module

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.os.BundleCompat
import com.teka.bluetoothapplication.BluetoothDeviceModel
import com.teka.bluetoothapplication.MainActivity
import com.teka.bluetoothapplication.R
import com.teka.bluetoothapplication.SA_TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class BluetoothService : Service() {

    companion object {
        const val CHANNEL_ID = "bluetooth_service_channel"
        const val CHANNEL_NAME = "Bluetooth Service"
    }

    private var bluetoothDevice: BluetoothDeviceModel? = null
    private lateinit var btManager: BtManager

    // Binder to bind the service with the activity if needed (for bound services)
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothService = this@BluetoothService
    }

    override fun onCreate() {
        super.onCreate()
        btManager = BtManager(this)
        // Retrieve the BluetoothDeviceModel using BundleCompat
        Timber.tag(SA_TAG).i("BT2: ${ bluetoothDevice?.name }")


        startForegroundService()
    }

    // This method is used to start the foreground service with a notification
    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(1, notification)
    }

    // Creates a basic notification for the foreground service
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bluetooth Scale")
            .setContentText("Reading data from scale")
            .setSmallIcon(R.drawable.bluetooth_searching_24)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            bluetoothDevice = BundleCompat.getParcelable(
                intent.extras ?: Bundle(),
                "bluetoothDevice",
                BluetoothDeviceModel::class.java
            )
        }

        if (bluetoothDevice == null) {
            Timber.tag(SA_TAG).e("Bluetooth device is null in onStartCommand")
        } else {
            Timber.tag(SA_TAG).i("Bluetooth device received: ${bluetoothDevice?.name}")
            // Launching the Bluetooth operation in a background coroutine
            CoroutineScope(Dispatchers.IO).launch {
                bluetoothDevice?.let { btManager.startReadingFromScale(it) }
            }
        }
        return START_STICKY // Ensuring the service keeps running
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        btManager.stopReadingFromScale()
        super.onDestroy()
    }
}
