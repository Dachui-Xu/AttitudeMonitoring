package com.example.attitudemonitoring.handler

import android.graphics.Color

import com.example.attitudemonitoring.bean.Sensor
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.*
import timber.log.Timber

/**
 * 动态折线图：
 *          动态获取传感器数据，并显示。
 */
class LineChartHandler(val lineChart: LineChart,
                       val sensor: Sensor,
                       private val lineColor: Int = Color.rgb(140, 234, 255)) {
    private val dataSet: LineDataSet
    private val scope = CoroutineScope(Dispatchers.Main)
    private var isPaused = false
    private var job: Job? = null

    init {
        val entries = ArrayList<Entry>()
        dataSet = LineDataSet(entries, "").apply {
            color = lineColor
            setDrawValues(false)
            setDrawCircles(false)
            setDrawIcons(false)
        }

        lineChart.apply {
            data = LineData(dataSet)
            legend.isEnabled = false
            axisRight.isEnabled = false
            setBackgroundColor(Color.TRANSPARENT)
            setTouchEnabled(true)
            setPinchZoom(true)
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 2200f
                granularity = 1f
                setLabelCount(3, true)
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                axisMinimum = 0f
                granularity = 1f
            }
            description.apply {
                text = sensor.getName()
                textColor = Color.RED
            }
        }

        lineChart.invalidate()
    }

    private fun addEntry() {
        synchronized(sensor) {
            val sensorDataList = sensor.getSensorData()
            dataSet.clear()
            var minY = 1500f
            var maxY = 1700f
            for ((index, sensorData) in sensorDataList.withIndex()) {
                val value = sensorData.value.toFloat()
                dataSet.addEntry(Entry(index.toFloat(), value))
                if (value < minY) minY = value
                if (value > maxY) maxY = value
            }
            lineChart.data.notifyDataChanged()
            lineChart.notifyDataSetChanged()
            lineChart.xAxis.axisMaximum = sensorDataList.size.toFloat() - 1
            lineChart.xAxis.axisMinimum = 0f
            // Update Y axis range
            lineChart.axisLeft.axisMinimum = minY - 1f  // adding some padding
           lineChart.axisLeft.axisMaximum = maxY + 50f  // adding some padding
            lineChart.invalidate()
        }
    }


    fun startUpdatingData() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                if (!isPaused) {
                    addEntry()
                }
                delay(20)
            }
        }
    }

    fun stopUpdatingData() {
        job?.cancel()
    }

    fun pauseUpdatingData() {
        isPaused = true
    }

    fun resumeUpdatingData() {
        isPaused = false
    }

    fun clearData() {
        dataSet.clear()
        lineChart.data = LineData(dataSet)
        lineChart.invalidate()
    }

    fun resetData() {
        clearData()
        startUpdatingData()
    }
}


