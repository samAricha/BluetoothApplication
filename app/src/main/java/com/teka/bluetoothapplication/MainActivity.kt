package com.teka.bluetoothapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.teka.bluetoothapplication.databinding.ActivityMainBinding
import com.teka.bluetoothapplication.permissions_module.MY_TAG
import com.teka.bluetoothapplication.permissions_module.PermissionLaunchersDto
import com.teka.bluetoothapplication.permissions_module.PermissionManager
import com.teka.bluetoothapplication.permissions_module.PermissionUtils
import com.teka.bluetoothapplication.permissions_module.requiredPermissionsInitialClient
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var binding: ActivityMainBinding

    private lateinit var multiplePermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var singlePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>
    private lateinit var discoverableLauncher: ActivityResultLauncher<Intent>
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var onExecutionPermissionLauncher: ActivityResultLauncher<String>

    private lateinit var permissionLaunchersDto: PermissionLaunchersDto


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupPermissionLaunchers()
//        permissionLaunchersDto = PermissionManager.setupPermissionLaunchers(this)
        requestPermissions()

        // Initialize Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Check if device supports Bluetooth
        if (mBluetoothAdapter == null) {
            binding.out.append("Device not supported")
            return
        }

        // Set up buttons and listeners using ViewBinding
        binding.button1.setOnClickListener { enableBluetooth() }
        binding.button2.setOnClickListener { makeDiscoverable() }
        binding.button3.setOnClickListener { disableBluetooth() }
        binding.button4.setOnClickListener { startScanningBluetoothDevices() }
        binding.button5.setOnClickListener {
            multiplePermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        binding.button6.setOnClickListener {
            val intent:Intent = Intent(this@MainActivity,ScaleActivity::class.java)
            startActivity(intent)
        }
    }


    // Function to enable Bluetooth
    private fun enableBluetooth() {
        if (!mBluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        }
    }

    fun enableLocation(activity: Activity) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        activity.startActivity(intent)
    }

    private fun requestAppPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Request multiple permissions for Android 12+
            multiplePermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // Request only the coarse location permission
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    // Function to make the device discoverable
    @SuppressLint("MissingPermission")
    private fun makeDiscoverable() {
        if (!mBluetoothAdapter.isDiscovering) {
            Toast.makeText(this, "Making your device discoverable", Toast.LENGTH_LONG).show()
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300) // 5 minutes
            }
            discoverableLauncher.launch(discoverableIntent)
        }
    }

    // Function to disable Bluetooth
    @SuppressLint("MissingPermission")
    private fun disableBluetooth() {
        if (mBluetoothAdapter.isEnabled) {
            mBluetoothAdapter.disable()
            Toast.makeText(this, "Turning off Bluetooth", Toast.LENGTH_LONG).show()
        }
    }



    private fun setupPermissionLaunchers() {
        multiplePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionResults(permissions)
        }

        singlePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                // Permission granted
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                // Permission denied
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        enableBluetoothLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Bluetooth Enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show()
            }
        }

        discoverableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Device is now discoverable", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to make device discoverable", Toast.LENGTH_SHORT).show()
            }
        }

        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Timber.tag("MA::initializePermissionLaunchers").i("Permission Granted")
            } else {
                // Permission denied: Show a message
                Timber.tag("MA::initializePermissionLaunchers").i("Permission Denied")
                showPermissionDeniedMessage()
            }
        }

        onExecutionPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            // Call the utility method to handle the permission result
            PermissionUtils.onPermissionResult(isGranted)
        }

    }

    private fun requestPermissions() {
        PermissionUtils.askMultiplePermissions(
            multiplePermissionLauncher = multiplePermissionLauncher,
//            multiplePermissionLauncher = permissionLaunchersDto.multiplePermissionLauncher,
            requiredPermissions = requiredPermissionsInitialClient,
            this
        )
    }

    private fun showPermissionDeniedMessage() {
        Timber.tag(MY_TAG).i("Permission Denied")
        Toast.makeText(
            this,
            "You must accept these permissions for this app to work!",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun startScanningBluetoothDevices() {
        PermissionUtils.requestPermissionAndExecuteAction(
            onExecutionPermissionLauncher,
            Manifest.permission.BLUETOOTH,
            this,
            {
                Toast.makeText(this, "Scanning for Bluetooth devices...", Toast.LENGTH_SHORT).show()
            }
        )
    }


    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        val allPermissionsGranted = permissions.all { it.value }

        if (allPermissionsGranted) {
            // All permissions are granted
            Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            // Handle permissions individually
            permissions.forEach { (permission, isGranted) ->
                if (isGranted) {
                    Toast.makeText(this, "$permission granted", Toast.LENGTH_SHORT).show()
                    // You can add specific actions for each granted permission here
                } else {
                    PermissionUtils.askSinglePermission(
                        singlePermissionLauncher = singlePermissionLauncher,
                        permission = permission,
                        context = this
                    )
//                    Toast.makeText(this, "$permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


}