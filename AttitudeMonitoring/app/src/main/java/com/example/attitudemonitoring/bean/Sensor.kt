package com.example.attitudemonitoring.bean

import android.annotation.SuppressLint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class Sensor(private val name: String) {
    private val sensorDataList = mutableListOf<SensorData>()
    @Volatile var isCollecting = false
    private var startTime: Long? = null

    @SuppressLint("DefaultLocale")
    @Synchronized
    fun addSensorData(value: Int) {
        if (isCollecting) {
            if (startTime == null) {
                startTime = System.currentTimeMillis()
            }
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - startTime!!
            val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime) - TimeUnit.MINUTES.toSeconds(minutes)
            val milliseconds = elapsedTime - TimeUnit.MINUTES.toMillis(minutes) - TimeUnit.SECONDS.toMillis(seconds)
            val relativeTime = String.format("%02d:%02d:%03d", minutes, seconds, milliseconds)

            val newData = SensorData(relativeTime, value)
            sensorDataList.add(newData)
        }
    }


    @Synchronized
    fun getSensorData(): List<SensorData> = sensorDataList

    fun getName(): String = name

    // 模拟生成传感器值的方法
    @OptIn(DelicateCoroutinesApi::class)
    fun generateRandomSensorValue() {
        GlobalScope.launch {
            while (isActive) {
                if (isCollecting) {
                val value = (0..1000).random()
                addSensorData(value)
                }
                delay(100) // 每100ms生成一个值
            }
        }
    }
    fun clearSensorData() {
        startTime = null
        sensorDataList.clear()
    }
}