package com.teka.bluetoothapplication

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teka.bluetoothapplication.bluetooth_module.BluetoothService
import com.teka.bluetoothapplication.bluetooth_module.BluetoothViewModel
import com.teka.bluetoothapplication.databinding.ActivityScaleBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber


const val SA_TAG = "SA_TAG"


@AndroidEntryPoint
class ScaleActivity : AppCompatActivity(){

    private lateinit var binding: ActivityScaleBinding
    private val btViewModel: BluetoothViewModel by viewModels()


    private lateinit var deviceInfoTextView: TextView
    private lateinit var quantityTxtView: TextView
    private lateinit var connectButton: Button
    private lateinit var submitButton: Button
    private lateinit var changeButton: Button
    private lateinit var nextCanButton: Button
    private lateinit var submitTestButton: Button
    private lateinit var scaleGroupRadioButtonGroup: RadioGroup

    private var bluetoothDevice: BluetoothDeviceModel? = null






    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScaleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        deviceInfoTextView = binding.deviceInfoText
        quantityTxtView = binding.quantityTxtView
        connectButton = binding.connectButton
        submitButton = binding.submit
        changeButton = binding.change
        nextCanButton = binding.nextCan
        submitTestButton = binding.submitTest
        scaleGroupRadioButtonGroup = binding.scaleGroup



        // Retrieve the BluetoothDeviceModel using BundleCompat
        bluetoothDevice = BundleCompat.getParcelable(
            intent.extras ?: Bundle(),
            "bluetoothDevice",
            BluetoothDeviceModel::class.java
        )
        Timber.tag(SA_TAG).i("BT2: ${ bluetoothDevice?.name }")


        // Observe Bluetooth scale data
        btViewModel.btScaleData.observe(this, Observer { data ->
//            textView.text = data // Display data from scale on the screen/
            quantityTxtView.text = data
            Timber.tag(SA_TAG).i("BT3: $data")
        })

        // Display device information
        val deviceDetails = String.format("${bluetoothDevice?.name} (${bluetoothDevice?.address})")
        deviceInfoTextView.text = deviceDetails

        setUpScreenViews()
        startBluetoothService()
    }


    fun setUpScreenViews(){

        submitButton.setOnClickListener {
            submitData()
        }

        changeButton.setOnClickListener {
            changeScale()
        }

        nextCanButton.setOnClickListener {
            nextLot()
        }

        submitTestButton.setOnClickListener {
            submitTestData()
        }

        connectButton.setOnClickListener {
            btViewModel.startBluetoothConnection()
        }


        scaleGroupRadioButtonGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.isPlatform -> handlePlatformScale()
                R.id.isBridge -> handleWeighBridgeScale()
            }
        }
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



    private fun startBluetoothService() {
        val intent = Intent(this, BluetoothService::class.java)
        intent.putExtra("bluetoothDevice", bluetoothDevice)
        ContextCompat.startForegroundService(this, intent) // Start foreground service
    }

    override fun onDestroy() {
        super.onDestroy()
        // Optionally stop Bluetooth service if needed
        stopBluetoothService()
    }

    private fun stopBluetoothService() {
        val intent = Intent(this, BluetoothService::class.java)
        stopService(intent)
    }

}