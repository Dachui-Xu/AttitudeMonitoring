package com.example.attitudemonitoring.viewModel

import MultipleLineChartsViewModel
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MultipleLineChartsViewModel::class.java)) {
            return MultipleLineChartsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

