package com.teka.bluetoothapplication

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope

/**
 * Extension function for [AppCompatActivity] to collect a Flow
 */
fun <T> AppCompatActivity.safeCollectFlow(flow: Flow<T>, result: (T) -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collectLatest { value ->
                result.invoke(value)
            }
        }
    }
}

/**
 * Extension function for [Fragment] to collect a Flow
 */
fun <T> Fragment.safeCollectFlow(flow: Flow<T>, result: (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collectLatest { value ->
                result.invoke(value)
            }
        }
    }
}

/**
 * Extension function for [ViewModel] to collect a Flow
 */
fun <T> ViewModel.safeCollectFlow(flow: Flow<T>, result: (T) -> Unit) {
    viewModelScope.launch {
        flow.collectLatest { value ->
            result.invoke(value)
        }
    }
}