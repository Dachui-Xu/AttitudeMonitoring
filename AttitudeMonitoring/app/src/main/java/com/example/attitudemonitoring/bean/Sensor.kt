package com.example.attitudemonitoring.bean

import android.annotation.SuppressLint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class Sensor(private val name: String) {
    private val sensorDataList = mutableListOf<SensorData>()
    @Volatile var isCollecting = false
    private var startTime: Long? = null
    var newValue: Int = -1
    var job : Job?= null

    @SuppressLint("DefaultLocale")
    @Synchronized
    fun addSensorData(value: Int) {
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
        newValue = newData.value
        sensorDataList.add(newData)
    }


    @Synchronized
    fun getSensorData(): List<SensorData> = sensorDataList

    fun getName(): String = name

    fun clearSensorData() {
        startTime = null
        sensorDataList.clear()
    }

    // 传感器字典更新



    // 模拟生成传感器值的方法
    @OptIn(DelicateCoroutinesApi::class)
    fun generateRandomSensorValue() {
        job = GlobalScope.launch {
            while (isActive) {
                if (isCollecting) {
                val value = (1700..5999).random()
                addSensorData(value)
                }
                delay(10) // 每100ms生成一个值
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }
}