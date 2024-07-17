package com.example.attitudemonitoring.bean

import android.content.Context
import androidx.compose.material3.TimeInput
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayDeque
import java.util.LinkedList

/**
* 采用cnn模型对状态分类
* */
class RealTimeClassifier(private val context: Context) {

    private var lastStatus: String = "normal"
    private val sensorWindows = mutableMapOf<String, LinkedList<Float>>()
    private lateinit var tflite: Interpreter
    val sensorKeys = listOf("sensor1", "sensor2", "sensor3", "sensor4")
    init {
        "model.tflite".loadModel()
    }

    private fun String.loadModel() {
        val assetFileDescriptor = context.assets.openFd(this)
        val inputStream = assetFileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val modelBuffer = fileChannel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }
        tflite = Interpreter(modelBuffer, options)
    }


    // 获得传感器的最大最小值
    private fun getSensorExtremes(sensorKey: String): Pair<Float, Float> {
        val sensorData = sensorWindows[sensorKey]
        val minSensorValue = sensorData?.minOrNull() ?: 0f
        val maxSensorValue = sensorData?.maxOrNull() ?: 0f
        return Pair(minSensorValue, maxSensorValue)
    }


    fun updateSensorData(sensorKey: String, newValue: Int) {
        val window = sensorWindows.getOrPut(sensorKey) { LinkedList() }
        if (window.size == 76) {
            window.removeFirst()
        }
        if(newValue>=0) {
        window.addLast(newValue.toFloat())
        }
    }

    fun determineStatus(): String {
        val window = sensorWindows.getOrPut("sensor1") { LinkedList() }
        if (window.size != 76) return lastStatus

        val (minValue1, maxValue1) = getSensorExtremes(sensorKeys[0])
        val (minValue2, maxValue2) = getSensorExtremes(sensorKeys[1])
        val (minValue3, maxValue3) = getSensorExtremes(sensorKeys[2])
        val (minValue4, maxValue4) = getSensorExtremes(sensorKeys[3])

        if(minValue2< 2200 || minValue2 <=2200){
            return "normal"
        }
        if(maxValue4 in 2200.0..2400.0  && maxValue4 in 2200.0..2400.0){
            return lastStatus
        }
        if(maxValue2 >= 3000){
            return "large down"
        }


        val inputBuffer = ByteBuffer.allocateDirect(4 * 76 * sensorWindows.size).order(ByteOrder.nativeOrder()) // Assuming each sensor contributes one float per time point
        sensorWindows.values.forEach { queue ->
            queue.forEach { value ->
                inputBuffer.putFloat(value)
            }
        }

        val output = Array(1) { FloatArray(4) } // 修改这里以匹配输出形状 [1, 4]
        tflite.run(inputBuffer, output)
        lastStatus = processOutput(output[0]) // 处理输出，注意 output[0] 提取了内部数组
        Timber.d("Model sensor:$lastStatus")
        return lastStatus

    }


    private fun processOutput(output: FloatArray): String {
        // Example: convert output floats to probabilities and determine the maximum index
        val statusLabels = arrayOf("down", "left", "right", "up")
        val maxIndex = output.indices.maxByOrNull { output[it] } ?: -1
        return if (maxIndex == -1) "large down" else statusLabels[maxIndex]
    }
}
