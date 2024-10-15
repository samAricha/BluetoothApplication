package com.teka.bluetoothapplication;

import android.app.Application;
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

import timber.log.Timber;

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}