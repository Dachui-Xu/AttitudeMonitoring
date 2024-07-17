import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import com.example.attitudemonitoring.bean.Animation
import com.example.attitudemonitoring.bean.DriveAnimation
import com.example.attitudemonitoring.bean.RealTimeClassifier
import com.example.attitudemonitoring.bean.SensorConditionLoader
import com.example.attitudemonitoring.bean.SensorConditionLoader.StatusFile.DRIVE_STATUS
import com.example.attitudemonitoring.bean.SensorConditionLoader.StatusFile.WORK_STATUS
import com.example.attitudemonitoring.bean.WorkAnimation
import com.example.attitudemonitoring.handler.LineChartHandler
import com.example.attitudemonitoring.handler.MultiLineChartHandler
import com.example.attitudemonitoring.util.CsvFileWriter
import com.example.attitudemonitoring.util.TimedVibrationManager
import com.example.attitudemonitoring.util.TimerManager
import com.example.attitudemonitoring.viewModel.SharedViewModel
import com.github.mikephil.charting.charts.LineChart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MultipleLineChartsViewModel(private val context: Context,private val sharedViewModel: SharedViewModel) : ViewModel() {
    private val multiLineChartHandler = MultiLineChartHandler()
    val isRunning = mutableStateOf(true)
    val conditionLoader = SensorConditionLoader(context)
    val realTimeClassifier = RealTimeClassifier(context)
    val timedVibrationManager =  TimedVibrationManager(context) // 震动
    var vibrateRunning by mutableStateOf(true) // 震动运行标志位，确保切换模式和清除数据后仍能获取一次新的震动数据
    val sensorValues: MutableMap<String, Int> = mutableMapOf(
        "sensor1" to 0,
        "sensor2" to 0,
        "sensor3" to 0,
        "sensor4" to 0,
    ) // 传感器最新值

    var status by mutableStateOf("normal")
    var _lastStatus = this.status
    private var judgeJob: Job? = null
    //var currentMode by mutableStateOf(Mode.WORK)

    private var _currentMode by mutableStateOf(Mode.WORK)
    val currentMode get() = _currentMode

    // 定时
    private val timerManager = TimerManager { formattedTime ->
        _elapsedTime.value = formattedTime
    }

    private val _elapsedTime = mutableStateOf("00:00:00")
    val duration: String
        get() = _elapsedTime.value



    enum class Mode(
        private val animations: Map<String, Animation>,
        val modeName: String
    ) {




        WORK(
            mapOf(
                "normal" to WorkAnimation.NORMAL,
                "down" to WorkAnimation.DOWN,
                "large down" to WorkAnimation.BIG_DOWN,
                "right" to WorkAnimation.RIGHT,
                "left" to WorkAnimation.LEFT,
                "rise" to WorkAnimation.RISE
            ),
            "Work"
        ),
        DRIVE(
            mapOf(
                "normal" to DriveAnimation.DRIVE,
                "left" to DriveAnimation.LEFT,
                "right" to DriveAnimation.RIGHT,
                "down" to DriveAnimation.DOWN,
                "up" to DriveAnimation.DRIVE,
                "large down" to DriveAnimation.DOWN
            ),
            "Drive"
        );

        fun getAnimation(animationKey: String): Animation {
            return animations[animationKey]?:animations.values.first()
        }
    }


    fun switchMode() {
        timerManager.stop()
        vibrateRunning = true
        if (currentMode == Mode.WORK) {
            _currentMode = Mode.DRIVE
            if (!sharedViewModel.optionModel)
            {conditionLoader.loadConditionsFromYaml(DRIVE_STATUS)}
        } else {
            _currentMode = Mode.WORK
            if (!sharedViewModel.optionModel){
            conditionLoader.loadConditionsFromYaml(WORK_STATUS)}
        }
        timerManager.start()
        if(!sharedViewModel.optionModel){
            status = conditionLoader.switchMode()
        }
    }

    init {
        val colors = listOf(
            Color.rgb(125, 174, 224),
            Color.rgb(179, 149, 189),
            Color.rgb(41, 157, 143),
            Color.rgb(233, 196, 106),
        )
        sharedViewModel.sensors.zip(colors).forEachIndexed { index, (sensor,color) ->
                multiLineChartHandler.addHandler(
                    LineChartHandler(
                        LineChart(context),
                        sensor.apply { isCollecting = true
                                                 // generateRandomSensorValue()
                                                 }, // 创建 Sensor 并启动数据收集
                        color
                    )
                )

        }
        startAll()
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
        stopJudgeStatus()
       // timerManager.reset()
        timerManager.stop()
        timedVibrationManager.stopTimedVibration()
        //sharedViewModel.readFlag = false // 停止蓝牙数据读取
    }

    fun pauseAll() {
        isRunning.value = false
        multiLineChartHandler.pauseAll()
        stopJudgeStatus()
        timerManager.pause()
        timedVibrationManager.pauseTimedVibration()
    }

    fun resumeAll() {
        isRunning.value = true
        multiLineChartHandler.resumeAll()
        startJudgeStatus()
        timerManager.resume()
        timedVibrationManager.resumeTimedVibration()
    }

    fun clearAll() {
        isRunning.value = false
        multiLineChartHandler.clearAll()
        timedVibrationManager.stopTimedVibration()
    }

    fun resetAll() {
        isRunning.value = true
        multiLineChartHandler.resetAll()
        startJudgeStatus()
        timerManager.reset()
        timerManager.start()
        vibrateRunning = true
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

    // 判断状态
    private fun startJudgeStatus() {
        // 如果已经有一个更新任务在运行，则先取消它
        judgeJob?.cancel()

        // 创建一个新的协程在 IO 线程上执行
        judgeJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) { // 使用 isActive 检查协程是否应该继续运行
                multiLineChartHandler.getHandlers().forEach {
                    sensorValues[it.sensor.getName()] = it.sensor.newValue
                    if(sharedViewModel.optionModel){
                        realTimeClassifier.updateSensorData(it.sensor.getName(), it.sensor.newValue)

                    }else{
                        conditionLoader.updateSensorData(it.sensor.getName(), it.sensor.newValue) // 把传感器数据放入一个时间窗中
                    }
                }
                if (sharedViewModel.optionModel){
                    status = realTimeClassifier.determineStatus()
                }else{
                    status = conditionLoader.determineStatus()
                }
                if (_lastStatus != status || vibrateRunning == true){
                    _lastStatus = status
                    timedVibrationManager.judgeVibrator(currentMode, status)
                    timerManager.stop()
                    timerManager.start()
                    vibrateRunning = false
                    Timber.d("Pattern：$currentMode,Status：$status")
                }
                delay(20L) // 每20毫秒更新一次
            }
        }
    }

    private fun stopJudgeStatus() {  judgeJob?.cancel() }
}

