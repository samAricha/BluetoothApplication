package com.teka.bluetoothapplication

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.teka.bluetoothapplication.bluetooth_module.BtDeviceModel
import com.teka.bluetoothapplication.bluetooth_module.BtService
import com.teka.bluetoothapplication.bluetooth_module.BluetoothViewModel
import com.teka.bluetoothapplication.databinding.ActivityScaleBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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
    private lateinit var loaderProgressBar: ProgressBar


    private var bluetoothDevice: BtDeviceModel? = null


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
        loaderProgressBar = binding.loader



        Timber.tag(SA_TAG).i("BT2: ${ bluetoothDevice?.name }")



        setUpScreenViews()
        startBluetoothService()
        observeUIState()
    }

    private fun observeUIState() {
        safeCollectFlow(btViewModel.uiState) { state ->
            state.connectedDevice?.let { device ->
                bluetoothDevice = device
                val deviceDetails = String.format("${bluetoothDevice?.name} (${bluetoothDevice?.address})")
                deviceInfoTextView.text = deviceDetails
                Timber.tag(SA_TAG).i("connectedDevice1 $device")
            } ?: run {
                Timber.tag(SA_TAG).i("connectedDevice2 runFunction")
                // Update UI for no connected device
//                myButton.isEnabled = false
//                myButton.text = "No Device Connected"
            }

            state.scaleData.let { weight ->
                Timber.tag(SA_TAG).i("read weight: $weight")
                quantityTxtView.text = weight
            }

            state.connectionState.let { connectionState ->
                if(connectionState == true){
                    connectButton.isEnabled = false
                    connectButton.alpha = 0.5f
                    connectButton.setTextColor(ContextCompat.getColor(this,R.color.black))
                    connectButton.setText("connected")
                    connectButton.setBackgroundColor(ContextCompat.getColor(this, R.color.disabled_button_color))
                }else{
                    connectButton.isEnabled = true
                    connectButton.alpha = 1f
                    connectButton.setTextColor(ContextCompat.getColor(this,R.color.white))
                    connectButton.setText("Connect")
                    connectButton.setBackgroundColor(ContextCompat.getColor(this@ScaleActivity, R.color.enabled_button_color))
                }
            }
            state.readingState.let {
                Timber.tag(SA_TAG).i("reading state: $it")

                if(it == true){
                    val drawable = ContextCompat.getDrawable(this, R.drawable.round)?.mutate() as GradientDrawable
                    drawable.setColor(ContextCompat.getColor(this, R.color.brownMid))
                    loaderProgressBar.visibility = View.VISIBLE
                    quantityTxtView.background = drawable
                }else{
                    val drawable = ContextCompat.getDrawable(this, R.drawable.round)?.mutate() as GradientDrawable
                    loaderProgressBar.visibility = View.GONE
                    quantityTxtView.background = drawable
                }
            }

        }
    }


    private fun setUpScreenViews(){

        submitButton.setOnClickListener {
            btViewModel.stopBtReading()
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
            Timber.tag(SA_TAG).i("connect btn clicked. Device: $bluetoothDevice")
            bluetoothDevice?.let { device ->
                lifecycleScope.launch {
                    btViewModel.startBluetoothConnection()
                }
            }
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
        val intent = Intent(this, BtService::class.java)
        intent.putExtra("bluetoothDevice", bluetoothDevice)
        ContextCompat.startForegroundService(this, intent) // Start foreground service
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBluetoothService()
    }

    private fun stopBluetoothService() {
        val intent = Intent(this, BtService::class.java)
        stopService(intent)
        btViewModel.stopBluetoothConnection()
    }

}