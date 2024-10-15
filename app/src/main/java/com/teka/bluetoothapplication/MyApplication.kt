package com.teka.bluetoothapplication;

import android.app.Application;
import android.content.Context

import timber.log.Timber;

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}