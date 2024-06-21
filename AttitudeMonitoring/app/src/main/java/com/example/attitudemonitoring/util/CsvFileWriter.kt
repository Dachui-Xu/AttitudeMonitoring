package com.example.attitudemonitoring.util

import android.content.Context
import android.os.Environment
import com.example.attitudemonitoring.bean.Sensor
import timber.log.Timber
import java.io.File


object CsvFileWriter {
    fun writeSensorDataToCsv(context: Context, sensors: List<Sensor>, fileName: String): File {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }
        Timber.d("文件保存在：$documentsDir")
        val csvFile = File(documentsDir, fileName)
        csvFile.printWriter().use { out ->
            val maxDataSize = sensors.map { it.getSensorData().size }.maxOrNull() ?: 0

            // Write headers
            out.print("Time,")
            sensors.forEachIndexed { index, sensor ->
                out.print(sensor.getName())
                if (index < sensors.size - 1) {
                    out.print(",")
                }
            }
            out.println()

            // Write data
            for (i in 0 until maxDataSize) {
                out.print("${sensors[1].getSensorData().getOrNull(i)?.time},")
                sensors.forEachIndexed { index, sensor ->
                    val sensorData = sensor.getSensorData().getOrNull(i)
                    out.print(sensorData?.value ?: "")
                    if (index < sensors.size - 1) {
                        out.print(",")
                    }
                }
                out.println()
            }
        }
        return csvFile
    }
}



