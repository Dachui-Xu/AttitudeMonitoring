package com.example.attitudemonitoring.handler

import com.example.attitudemonitoring.bean.Sensor

/**
 * 统一管理多个折线图
 */
class MultiLineChartHandler {
    private val handlers = mutableListOf<LineChartHandler>()
    private val sensors = mutableListOf<Sensor>()

    fun addHandler(handler: LineChartHandler) {
        handlers.add(handler)
        sensors.add(handler.sensor)
    }

    fun startAll() {
        sensors.forEach { it.isCollecting = true }
        handlers.forEach { it.startUpdatingData() }
    }

    fun stopAll() {
        sensors.forEach { it.isCollecting = false }
        handlers.forEach { it.stopUpdatingData() }
    }

    fun pauseAll() {
        sensors.forEach { it.isCollecting = false }
        handlers.forEach { it.pauseUpdatingData() }
    }

    fun resumeAll() {
        sensors.forEach { it.isCollecting = true }
        handlers.forEach { it.resumeUpdatingData() }
    }

    fun getHandlers(): List<LineChartHandler> {
        return handlers
    }

    fun clearAll() {
        sensors.forEach {
            it.isCollecting = false
            it.clearSensorData()
        }
        handlers.forEach { it.clearData() }
    }

    fun resetAll() {
        sensors.forEach { it.isCollecting = true }
        handlers.forEach { it.resetData() }
    }
}

