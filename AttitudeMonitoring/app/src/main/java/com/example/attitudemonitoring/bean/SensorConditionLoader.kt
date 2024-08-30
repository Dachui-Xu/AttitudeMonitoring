package com.example.attitudemonitoring.bean

import android.content.Context
import android.widget.Toast
import com.google.gson.Gson
import org.yaml.snakeyaml.Yaml
import java.io.IOException
import java.util.LinkedList


class SensorConditionLoader(private val context: Context) {
    val sensorKeys = listOf("sensor1", "sensor2", "sensor3", "sensor4")
    enum class StatusFile(val filename: String) {
        WORK_STATUS("work_status.yaml"),
        DRIVE_STATUS("drive_status.yaml")
    }

    data class Condition(
        val sensor1: SensorRange?,
        val sensor2: SensorRange?,
        val sensor3: SensorRange?,
        val sensor4: SensorRange?
    )

    data class SensorRange(
        val min: Int?,
        val max: Int?
    )

    private var conditions: Map<String, Condition> = emptyMap()
    private var lastStatus: String = "normal"
    private val sensorWindows = mutableMapOf<String, LinkedList<Int>>()

    init {
        loadConditionsFromYaml(StatusFile.WORK_STATUS)
    }

    fun loadConditionsFromYaml(statusFile: StatusFile) {
        try {
            val inputStream = context.assets.open(statusFile.filename)
            val yamlContent = inputStream.bufferedReader().use { it.readText() }
            val yamlParser = Yaml()
            val yamlObject = yamlParser.load<Map<String, Any>>(yamlContent)
            val gson = Gson()

            conditions = yamlObject.mapValues { (_, value) ->
                gson.fromJson(gson.toJson(value), Condition::class.java)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            conditions = emptyMap()
        }
    }

    // 获得传感器的最大最小值
    private fun getSensorExtremes(sensorKey: String): Pair<Int, Int> {
        val sensorData = sensorWindows[sensorKey]
        val minSensorValue = sensorData?.minOrNull() ?: 0
        val maxSensorValue = sensorData?.maxOrNull() ?: 0
        return Pair(minSensorValue, maxSensorValue)
    }

    fun updateSensorData(sensorKey: String, newValue: Int) {
        val window = sensorWindows.getOrPut(sensorKey) { LinkedList() }
        if (window.size == 30) {
            window.removeFirst()
        }
        window.addLast(newValue)
    }

    fun determineStatus(): String {
//        val maxValues = sensorWindows.mapValues { (key, values) ->
//            values.maxOrNull() ?: 0
//        }
        val (minValue1, maxValue1) = getSensorExtremes(sensorKeys[0])
        val (minValue2, maxValue2) = getSensorExtremes(sensorKeys[1])
        val (minValue3, maxValue3) = getSensorExtremes(sensorKeys[2])
        val (minValue4, maxValue4) = getSensorExtremes(sensorKeys[3])

//
//        if((maxValue4 in (2450..2600)) || (maxValue3 in (2450..2600))){
//            return lastStatus
//        }
//        if(maxValue2 >= 3000){
//            return "large down"
//        }
        if(lastStatus == "large down"|| lastStatus == "down" || lastStatus =="right"){
            if(minValue3<=2450){
//                sensorWindows.clear()
                lastStatus = "normal"
                return lastStatus
            }
        }





//            sensorWindows.clear()
//            lastStatus = "down"
//            return lastStatus
//        }



        for ((status, condition) in conditions) {
            if (
                isWithinRange(maxValue1, condition.sensor1) &&
                isWithinRange(minValue2, condition.sensor2) &&
                isWithinRange(maxValue3, condition.sensor3) &&
                isWithinRange(maxValue4, condition.sensor4)
            ) {
                lastStatus = status


            }
        }
        return lastStatus
    }


    fun switchMode(): String {
        sensorWindows.keys.forEach { key ->
            sensorWindows[key]?.clear()
        }
        return determineStatus()
    }

    private fun isWithinRange(value: Int, range: SensorRange?): Boolean {
        if (range == null) return true
        val minCheck = range.min?.let { value >= it } ?: true
        val maxCheck = range.max?.let { value <= it } ?: true
        return minCheck && maxCheck
    }
}


