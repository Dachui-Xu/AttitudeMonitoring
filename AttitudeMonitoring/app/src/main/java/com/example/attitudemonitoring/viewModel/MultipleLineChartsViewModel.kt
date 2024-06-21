import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attitudemonitoring.bean.Sensor
import com.example.attitudemonitoring.handler.LineChartHandler
import com.example.attitudemonitoring.handler.MultiLineChartHandler
import com.example.attitudemonitoring.util.CsvFileWriter
import com.github.mikephil.charting.charts.LineChart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MultipleLineChartsViewModel(private val context: Context) : ViewModel() {
    private val multiLineChartHandler = MultiLineChartHandler()
    val isRunning = mutableStateOf(true)

    init {
        val sensors = listOf(
            Sensor("sensor1"),
            Sensor("sensor2"),
            Sensor("sensor3"),
            Sensor("sensor4"),
            Sensor("sensor5"),
            Sensor("sensor6"),
            Sensor("sensor7"),
            Sensor("sensor8")
        ).onEach {
            it.isCollecting = true
            it.generateRandomSensorValue()
        }

        val colors = listOf(
            Color.rgb(234, 131, 121),
            Color.rgb(125, 174, 224),
            Color.rgb(179, 149, 189),
            Color.rgb(41, 157, 143),
            Color.rgb(233, 196, 106),
            Color.rgb(69, 105, 144),
            Color.rgb(202, 139, 168),
            Color.rgb(165, 181, 93)
        )

        val lineChartHandlers = sensors.zip(colors).map { (sensor, color) ->
            LineChartHandler(LineChart(context), sensor, color)
        }

        lineChartHandlers.forEach {
            multiLineChartHandler.addHandler(it)
            startAll()
        }
    }

    fun getHandlers(): List<LineChartHandler> {
        return multiLineChartHandler.getHandlers()
    }

    fun startAll() {
        isRunning.value = true
        multiLineChartHandler.startAll()
    }

    fun stopAll() {
        isRunning.value = false
        multiLineChartHandler.stopAll()
    }

    fun pauseAll() {
        isRunning.value = false
        multiLineChartHandler.pauseAll()
    }

    fun resumeAll() {
        isRunning.value = true
        multiLineChartHandler.resumeAll()
    }

    fun clearAll() {
        isRunning.value = false
        multiLineChartHandler.clearAll()
    }

    fun resetAll() {
        isRunning.value = true
        multiLineChartHandler.resetAll()
    }

    fun saveSensorData(): File {
        val sensors = multiLineChartHandler.getHandlers().map { it.sensor }
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val currentTime = dateFormat.format(Date())
        val fileName = "sensor_data_$currentTime.csv"
        return CsvFileWriter.writeSensorDataToCsv(context, sensors, fileName)
    }

    fun shareSensorData() {
        val file = saveSensorData()
        shareFile(context, file)
    }

    private fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share sensor data")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (shareIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(chooser)
        } else {
            Toast.makeText(context, "No app can perform this action", Toast.LENGTH_SHORT).show()
        }
    }
}

