package com.teka.bluetoothapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.teka.bluetoothapplication.databinding.ActivityScaleBinding


class ScaleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScaleBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScaleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

}