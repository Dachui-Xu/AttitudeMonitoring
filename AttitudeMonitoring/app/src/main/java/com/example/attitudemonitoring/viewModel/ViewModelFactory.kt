package com.example.attitudemonitoring.viewModel

import DataReaderViewModel
import MultipleLineChartsViewModel
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import com.zhzc0x.bluetooth.BluetoothClient
import com.zhzc0x.bluetooth.client.Characteristic
import com.zhzc0x.bluetooth.client.ClientType


class ViewModelFactory(
    private val context: Context,
    private val sharedViewModel: SharedViewModel,
    private val bluetoothType: ClientType = ClientType.CLASSIC,
    private val bluetoothClient: BluetoothClient = BluetoothClient(context,bluetoothType,null),
    private val readCharacteristic: Characteristic? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MultipleLineChartsViewModel::class.java) -> {
                MultipleLineChartsViewModel(context,sharedViewModel) as T
            }
            modelClass.isAssignableFrom(DataReaderViewModel::class.java) -> {
                DataReaderViewModel(bluetoothType, bluetoothClient, readCharacteristic,sharedViewModel) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
