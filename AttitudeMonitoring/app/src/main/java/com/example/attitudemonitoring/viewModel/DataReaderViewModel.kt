import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attitudemonitoring.viewModel.SharedViewModel
import com.zhzc0x.bluetooth.BluetoothClient
import com.zhzc0x.bluetooth.client.Characteristic
import com.zhzc0x.bluetooth.client.ClientType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class DataReaderViewModel (
    private val bluetoothType: ClientType,
    private val bluetoothClient: BluetoothClient,
    private var readCharacteristic: Characteristic?,
    private var sharedViewModel: SharedViewModel
) : ViewModel() {
    var isCollecting by mutableStateOf(false)
    val dataHistory = mutableStateListOf<String>()
    private var dataReadingJob: Job? = null
    private val _selectedOption = MutableStateFlow<Option>(Option.YAML)
    val selectedOption: StateFlow<Option> = _selectedOption

    fun selectOption(option: Option) {
        _selectedOption.value = option
    }

    enum class Option { MODEL, YAML }
    fun toggleDataReading() {
        isCollecting = !isCollecting
        if (isCollecting) {
            startDataReading()
        } else {
            stopDataReading()
        }
    }
    fun optionModel(){
        sharedViewModel.optionModel = true
    }
    fun optionRange(){
        sharedViewModel.optionModel = false
    }
    fun clearDataHistory() {
        dataHistory.clear()
    }

    private fun startDataReading() {
        stopDataReading()
        dataReadingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val dataList = readData()
                if (dataList.isNotEmpty()) {
                    dataList.forEach { data ->
                        dataHistory.add(data)
                        sharedViewModel.parseSharedData(data)
                    }
                }
                if(sharedViewModel.readFlag == false){
                    stopDataReading()
                }
                delay(1) // 根据设备的数据发送频率调整
            }
        }
    }

    private fun stopDataReading() {
        dataReadingJob?.cancel()
    }

    private suspend fun readData(): List<String> {
        if (bluetoothType == ClientType.BLE && readCharacteristic == null) {
            return emptyList()
        }
        val result = CompletableDeferred<List<String>>()
        bluetoothClient.readData(readCharacteristic?.uuid) { success, data ->
            if (success) {
                val dataStr = String(data!!)
                result.complete(dataStr.split("\n").filter { it.isNotBlank() })
            } else {
                result.complete(emptyList())
            }
        }
        return result.await()
    }



}

