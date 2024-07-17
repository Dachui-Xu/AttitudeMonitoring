package com.example.attitudemonitoring.viewModel

import androidx.compose.material3.TimeInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.attitudemonitoring.bean.Sensor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber


class SharedViewModel : ViewModel() {
    var readFlag by mutableStateOf(true)
    val sensors = List(4) { Sensor("sensor${it + 1}") }
    var optionModel by mutableStateOf(true) // 选择状态判定模式是否为模型
    val sensorsValue = IntArray(4)

    fun parseSharedData(data: String) {
        val values = data.trim().split(",").mapNotNull { it.toIntOrNull() }
        if (values.size == 4) {
            for (i in values.indices) {
                sensorsValue[i] = values[i]
            }
            updateSensors()
        }
    }

    private fun updateSensors() {
        for (i in sensorsValue.indices) {
            CoroutineScope(Dispatchers.IO).launch {
                sensors[i].addSensorData(sensorsValue[i])
            }
        }
    }



}