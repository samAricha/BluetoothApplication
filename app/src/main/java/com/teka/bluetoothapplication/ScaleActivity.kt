package com.teka.bluetoothapplication

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.BundleCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teka.bluetoothapplication.bluetooth_module.BluetoothViewModel
import com.teka.bluetoothapplication.databinding.ActivityScaleBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber


const val SA_TAG = "SA_TAG"


@AndroidEntryPoint
class ScaleActivity : AppCompatActivity(), DeviceAdapter.DeviceListener {

    private lateinit var binding: ActivityScaleBinding
    private val btViewModel: BluetoothViewModel by viewModels()

    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var connectButton: Button
    private lateinit var deviceInfoTextView: TextView




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScaleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Retrieve the BluetoothDeviceModel from the intent
        val bluetoothDevice: BluetoothDeviceModel? = intent.getParcelableExtra("bluetoothDevice")
        Timber.tag(SA_TAG).i("BT1: ${ bluetoothDevice?.name }")


        // Retrieve the BluetoothDeviceModel using BundleCompat
        val bluetoothDevice2: BluetoothDeviceModel? = BundleCompat.getParcelable(
            intent.extras ?: Bundle(),
            "bluetoothDevice",
            BluetoothDeviceModel::class.java
        )
        Timber.tag(SA_TAG).i("BT2: ${ bluetoothDevice2?.name }")

        deviceInfoTextView = binding.deviceInfoText
        connectButton = binding.connectButton


        // Display device information
        val deviceDetails = String.format("${bluetoothDevice2?.name} (${bluetoothDevice2?.address})")
        deviceInfoTextView.text = deviceDetails

        connectButton.setOnClickListener {
            Toast.makeText(this, "Connecting to $deviceDetails", Toast.LENGTH_SHORT).show()
        }


        deviceAdapter = DeviceAdapter(this, listener = this)
        setupRecyclerView()


        binding.scaleGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.isPlatform -> handlePlatformScale()
                R.id.isBridge -> handleWeighBridgeScale()
            }
        }

        binding.submit.setOnClickListener {
            submitData()
        }

        binding.change.setOnClickListener {
            changeScale()
        }

        binding.nextCan.setOnClickListener {
            nextLot()
        }

        binding.submitTest.setOnClickListener {
            submitTestData()
        }

        setupOtherViews()
        // Add mock Bluetooth devices for testing
        addMockDevices()
    }

    // Add mock Bluetooth devices to the adapter
    private fun addMockDevices() {
        val mockDevice1 = BluetoothDeviceModel("Device 1", "00:11:22:33:44:55")
        val mockDevice2 = BluetoothDeviceModel("Device 2", "AA:BB:CC:DD:EE:FF")
        val mockDevice3 = BluetoothDeviceModel("Device 3", "11:22:33:44:55:66")

        deviceAdapter.addDevice(mockDevice1)
        deviceAdapter.addDevice(mockDevice2)
        deviceAdapter.addDevice(mockDevice3)
    }


    private fun setupRecyclerView() {
        binding.recycler.layoutManager = LinearLayoutManager(this)
         binding.recycler.adapter = deviceAdapter
    }

    private fun handlePlatformScale() {
        // Logic when Platform scale is selected
    }

    private fun handleWeighBridgeScale() {
        // Logic when Weigh Bridge is selected
    }

    private fun submitData() {
        // Logic to submit data
    }

    private fun changeScale() {
        // Logic to change scale
    }

    private fun nextLot() {
        // Logic for next lot
    }

    private fun submitTestData() {
        val testData = binding.testData.text.toString()
        // Logic to handle the test data submission
    }

    private fun setupOtherViews() {
        // Initialize the progress bar, text views, or any other view-related setup
    }

    override fun onDeviceClicked(device: BluetoothDeviceModel) {
        Toast.makeText(this, "Clicked: ${device.name ?: "Unknown"}", Toast.LENGTH_SHORT).show()
    }

}