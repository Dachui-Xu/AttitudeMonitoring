package com.example.attitudemonitoring.ui


import MultipleLineChartsViewModel
import com.example.attitudemonitoring.bean.LogoutInfo
import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.attitudemonitoring.R
import com.example.attitudemonitoring.bean.Sensor
import com.example.attitudemonitoring.ui.theme.AttitudeMonitoringTheme
import com.example.attitudemonitoring.ui.theme.ColorLogoutDebug
import com.example.attitudemonitoring.ui.theme.ColorLogoutError
import com.example.attitudemonitoring.ui.widgets.ChangeMtuDialog
import com.example.attitudemonitoring.ui.widgets.LineChartView
import com.example.attitudemonitoring.ui.widgets.MultipleLineChartsView
import com.example.attitudemonitoring.ui.widgets.ScanDeviceDialog

import com.example.attitudemonitoring.ui.widgets.TopBar
import com.example.attitudemonitoring.util.getVersionName
import com.example.attitudemonitoring.util.toHex
import com.zhzc0x.bluetooth.BluetoothClient
import com.zhzc0x.bluetooth.client.Characteristic
import com.zhzc0x.bluetooth.client.ClientState
import com.zhzc0x.bluetooth.client.ClientType
import com.zhzc0x.bluetooth.client.ConnectState
import com.zhzc0x.bluetooth.client.Device
import com.zhzc0x.bluetooth.client.Service
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date

class MainActivity : ComposeBaseActivity() {

    private var bluetoothType by mutableStateOf(ClientType.BLE)
    private lateinit var bluetoothClient: BluetoothClient
    private var deviceName by mutableStateOf("")
    private val serviceList = ArrayList<Service>()
    private val receiveCharacteristicList = ArrayList<Characteristic>()
    private val sendCharacteristicList = ArrayList<Characteristic>()
    private val readCharacteristicList = ArrayList<Characteristic>()
    private var service: Service? by mutableStateOf(null)
    private var receiveCharacteristic: Characteristic? by mutableStateOf(null)
    private var sendCharacteristic: Characteristic? by mutableStateOf(null)
    private var readCharacteristic: Characteristic? by mutableStateOf(null)
    private var readDataStr: String by mutableStateOf("")
    @Volatile
    private var receivePackets = 0
    private var mtu by mutableStateOf(88)
    private var showChangeMtuDialog by mutableStateOf(false)

    private val logoutList = mutableStateListOf<LogoutInfo>()
    private val dataHistory = mutableStateListOf<String>()
    private var readFlag: Boolean by mutableStateOf<Boolean>(false)
    private var scrollToBottom by mutableStateOf(false)
    private var showScanDialog by mutableStateOf(false)
    private var scanning = mutableStateOf(true)
    private var scanDeviceList = mutableStateListOf<Device>()
    private var appTitle = ""


    override fun initData() {
        val logFlow = channelFlow{
            Timber.plant(object: Timber.Tree(){
                private val date = Date()
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    date.time = System.currentTimeMillis()
                    trySend(LogoutInfo(message, priority))
                }
            })
            awaitClose{
                Timber.d("logChannelFLow closed")
            }
        }
        lifecycleScope.launch {
            logFlow.collect {
                println("collect=$it")
                while(logoutList.size >= 1000){
                    logoutList.removeAt(0)
                }
                logoutList.add(it)
                scrollToBottom = !scrollToBottom
            }
        }
        appTitle = "${getString(R.string.app_name)}v${getVersionName(this@MainActivity)}"
    }

    @ExperimentalMaterial3Api
    @Composable
    override fun Content() {
        Scaffold(Modifier.fillMaxSize(), topBar = {
            TopBar(title=appTitle, showBackButton = false)
        }){ paddingValues ->
            Column(Modifier.padding(start = 14.dp, top=paddingValues.calculateTopPadding(), end=14.dp)) {
                Row(
                    Modifier
                        .padding(top = 12.dp)
                        .height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                    var expanded by remember {
                        mutableStateOf(false)
                    }
                    Text(text = "蓝牙类型：$bluetoothType",
                        Modifier
                            .clickable {
                                expanded = true
                            }
                            .padding(8.dp), fontSize = 16.sp, textAlign = TextAlign.Center)
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        ClientType.entries.forEach { clientType ->
                            DropdownMenuItem(text = {
                                Text(text = "$clientType", fontSize = 16.sp)
                            }, onClick = {
                                if(bluetoothType != clientType){
                                    bluetoothClient.release()
                                    bluetoothType = clientType
                                }
                                expanded = false
                            })
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    if(deviceName.isEmpty()){
                        Button(onClick = {
                            when(bluetoothClient.checkState()){
                                ClientState.NOT_SUPPORT -> {
                                    showToast("当前设备不支持蓝牙功能！")
                                }
                                ClientState.DISABLE -> {
                                    showToast("请先开启蓝牙！")
                                }
                                ClientState.ENABLE -> {
                                    showScanDialog = true
                                }
                                else  -> {}
                            }
                        }) {
                            Text(text = "扫描", fontSize = 16.sp)
                        }
                    } else {
                        Button(onClick = {
                            bluetoothClient.disconnect()
                        }) {
                            Text(text = "断开", fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = deviceName)
                }
                var selectedTabIndex by remember {
                    mutableStateOf(0)
                }
                TabRow(selectedTabIndex, Modifier) {
                    Tab(selectedTabIndex == 0, onClick = {
                        selectedTabIndex = 0
                    }, Modifier.height(40.dp),  text = {
                        Text(text = "蓝牙服务", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }, unselectedContentColor = Color.Gray)
                    Tab(selectedTabIndex == 1, onClick = {
                        selectedTabIndex = 1
                    }, Modifier.height(40.dp), text = {
                        Text(text = "实时日志", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }, unselectedContentColor = Color.Gray)
                    Tab(selectedTabIndex == 2, onClick = {
                        selectedTabIndex = 2
                    }, Modifier.height(40.dp), text = {
                        Text(text = "实时数据", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }, unselectedContentColor = Color.Gray)
                    Tab(selectedTabIndex == 3, onClick = {
                        selectedTabIndex = 3
                    }, Modifier.height(40.dp), text = {
                        Text(text = "绘图", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }, unselectedContentColor = Color.Gray)
                }
                val tabPageModifier = remember {
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .weight(1f) }
                when(selectedTabIndex){
                    0 -> ServiceChoose(tabPageModifier)
                    1 -> RealtimeLogout(tabPageModifier)
                    2 -> DataReaderScreen(tabPageModifier)
                    3 -> {
                        MultipleLineChartsView()
                    }
                }
                if(deviceName.isNotEmpty()){
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .wrapContentHeight(),
                        verticalAlignment = Alignment.CenterVertically){
                        var sendText by remember { mutableStateOf("") }
                        var sendError by remember { mutableStateOf(false) }
                        TextField(sendText, onValueChange = { inputText ->
                            sendText = inputText
                            sendError = false
                        },
                            Modifier
                                .fillMaxWidth()
                                .weight(1f), singleLine=true, label = {
                            Text(text = "数据格式：任意字符")
                        }, isError = sendError)
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(onClick = {
                            if(sendText.isNotEmpty()){
                                sendData(sendText.toByteArray())
                            } else {
                                sendError = true
                            }
                        }) {
                            Text(text = "发送")
                        }
                    }
                }
            }
        }
        if(showScanDialog){
            ScanDeviceDialog(scanning, scanDeviceList, ::startScanDevice, ::stopScanDevice, onCancel={
                showScanDialog = false
                stopScanDevice()
            }, ::connectDevice)
        }
        LaunchedEffect(bluetoothType){
           // val serviceUUID = UUID.fromString("cc4d67e2-42ca-4db3-9629-0c27f39082c6") // 替换为你的UUID
            bluetoothClient = BluetoothClient(this@MainActivity, bluetoothType, null)
            bluetoothClient.setSwitchReceive(turnOn = {
                showScanDialog = true
            }, turnOff = {
                scanning.value = false
                stopScanDevice()
            })
        }
    }
    @Preview(showBackground = true)
    @Composable
    fun ChoosePreview() {
        AttitudeMonitoringTheme {
            ServiceChoose(Modifier.fillMaxWidth())
        }
    }

    @Composable
    private fun ServiceChoose(modifier: Modifier) = Column(modifier){
        if(deviceName.isNotEmpty()){
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                var expanded by remember { mutableStateOf(false) }
                Text(text = "ServiceUid:", fontSize = 14.sp)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            expanded = serviceList.isNotEmpty()
                        }, contentAlignment = Alignment.Center){
                    Text(text = "${service?.uuid ?: ""}", fontSize = 14.sp, lineHeight = 14.5.sp)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
                    offset = DpOffset(80.dp, 0.dp)) {
                    serviceList.forEach {
                        DropdownMenuItem(text = {
                            Column {
                                Text(text = "${it.uuid}", fontSize = 14.sp, lineHeight = 14.5.sp)
                                Text(text = "${it.type}", color=Color.Gray, fontSize = 12.sp)
                                Spacer(Modifier.height(4.dp))
                            }
                        }, onClick = {
                            service = it
                            assignService(service)
                            expanded = false
                        })
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                var expanded by remember { mutableStateOf(false) }
                Text(text = "ReceiveUid:", fontSize = 14.sp)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            expanded = receiveCharacteristicList.isNotEmpty()
                        }, contentAlignment = Alignment.Center){
                    Text(text = "${receiveCharacteristic?.uuid ?: ""}", fontSize = 14.sp, lineHeight = 14.5.sp)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
                    offset = DpOffset(80.dp, 0.dp)) {
                    receiveCharacteristicList.forEach {
                        DropdownMenuItem(text = {
                            Column {
                                Text(text = "${it.uuid}", fontSize = 14.sp, lineHeight = 14.5.sp)
                                Text(text = "${it.properties}", color=Color.Gray, fontSize = 12.sp)
                                Spacer(Modifier.height(4.dp))
                            }
                        }, onClick = {
                            receiveCharacteristic = it
                            expanded = false
                        })
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Receive每秒包数：", fontSize = 14.sp)
                var packetsRate by remember { mutableStateOf(0) }
                Text(text = "$packetsRate", Modifier.defaultMinSize(32.dp), fontSize = 14.sp)
                LaunchedEffect(deviceName){
                    while(deviceName.isNotEmpty()){
                        receivePackets = 0
                        delay(1000)
                        packetsRate = receivePackets
                    }
                }
                Box(
                    Modifier
                        .padding(start = 12.dp)
                        .fillMaxHeight()
                        .clickable {
                            showChangeMtuDialog = true
                        }
                        .padding(start = 8.dp, end = 8.dp), contentAlignment = Alignment.Center){
                    Text(text = "修改mtu($mtu)", fontSize = 14.sp)
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                var expanded by remember { mutableStateOf(false) }
                Text(text = "SendUid:", fontSize = 14.sp)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            expanded = sendCharacteristicList.isNotEmpty()
                        }, contentAlignment = Alignment.Center){
                    Text(text = "${sendCharacteristic?.uuid ?: ""}", fontSize = 14.sp, lineHeight = 14.5.sp)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
                    offset = DpOffset(60.dp, 0.dp)) {
                    sendCharacteristicList.forEach {
                        DropdownMenuItem(text = {
                            Column {
                                Text(text = "${it.uuid}", fontSize = 14.sp, lineHeight = 14.5.sp)
                                Text(text = "${it.properties}", color=Color.Gray, fontSize = 12.sp)
                                Spacer(Modifier.height(4.dp))
                            }
                        }, onClick = {
                            sendCharacteristic = it
                            expanded = false
                        })
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                var expanded by remember { mutableStateOf(false) }
                Text(text = "ReadUid:", fontSize = 14.sp)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            expanded = readCharacteristicList.isNotEmpty()
                        }, contentAlignment = Alignment.Center){
                    Text(text = "${readCharacteristic?.uuid ?: ""}", fontSize = 14.sp, lineHeight = 14.5.sp)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
                    offset = DpOffset(60.dp, 0.dp)) {
                    readCharacteristicList.forEach {
                        DropdownMenuItem(text = {
                            Column {
                                Text(text = "${it.uuid}", fontSize = 14.sp, lineHeight = 14.5.sp)
                                Text(text = "${it.properties}", color=Color.Gray, fontSize = 12.sp)
                                Spacer(Modifier.height(4.dp))
                            }
                        }, onClick = {
                            readCharacteristic = it
                            expanded = false
                        })
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    readData()
                }, Modifier.height(40.dp)) {
                    Text(text = "读取数据")
                }
                Text(text=readDataStr, Modifier.padding(start=6.dp))
            }
            if(showChangeMtuDialog){
                ChangeMtuDialog({
                    showChangeMtuDialog = false
                }){ inputMtu ->
                    if(bluetoothClient.changeMtu(inputMtu)){
                        mtu = inputMtu
                        showToast("mtu修改成功！")
                    } else {
                        showToast("mtu修改失败！")
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "请先连接设备", fontSize = 16.sp)
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AttitudeMonitoringTheme {
        Content()
    }
}

    @Composable
    private fun RealtimeLogout(modifier: Modifier){
        val state = rememberLazyListState()
        LaunchedEffect(scrollToBottom){
            if(logoutList.isNotEmpty()){
                state.scrollToItem(logoutList.size - 1)
            }
        }
        LazyColumn(modifier, state){
            items(logoutList) { logout ->
                Row(Modifier.padding(top=2.dp)){
                    Text(logout.times, color=Color.Gray, fontSize = 12.sp, lineHeight = 12.5.sp)
                    Text(text = logout.msg, Modifier.padding(start=4.dp),
                        color = if(logout.priority == Log.ERROR){
                            ColorLogoutError
                        } else {
                            ColorLogoutDebug
                        }, fontSize = 12.sp, lineHeight = 12.5.sp)
                }
            }
        }
    }

    @Composable
    fun DataReaderScreen(modifier: Modifier) = Column(modifier) {
        var readDataStr by remember { mutableStateOf("") }
        var isCollecting by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val scrollState = rememberLazyListState()

        // 数据滚动到底部
        LaunchedEffect(isCollecting) {
            if (dataHistory.isNotEmpty()) {
                scrollState.scrollToItem(dataHistory.size - 1)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .padding(16.dp)
                .border(2.dp, Color.Gray),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(dataHistory) { data ->
                    Text(data, modifier = Modifier.padding(4.dp))
                }
            }

            // 开始/停止读取按钮
            Button(
                onClick = {
                    readFlag =!readFlag
                    isCollecting = !isCollecting
                    if (isCollecting) {
                        scope.launch {
                            while (readFlag && isCollecting) {
                                readData { success, data ->
                                    if (success) {
                                        dataHistory.add(data)
                                        readDataStr = data  // 保留最新数据
                                    }
                                }
                                delay(100) // 每秒读取一次数据
                            }
                        }
                    }
                },
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text(text = if (isCollecting) "停止读取" else "开始读取")
            }
        }
    }

@Preview(showBackground = true)
@Composable
fun ReadPreview() {
    AttitudeMonitoringTheme {
        DataReaderScreen(Modifier.fillMaxSize())
    }
}




    private fun readData(callback: (Boolean, String) -> Unit) {
        if (bluetoothType == ClientType.BLE && readCharacteristic == null) {
            showToast("请选择ReadUid!")
            callback(false, "")
            return
        }
        bluetoothClient.readData(readCharacteristic?.uuid) { success, data ->
            if (success) {
                val dataStr = String(data!!)
                callback(true, dataStr)
            } else {
                showToast("数据读取失败！")
                callback(false, "")
            }
        }
    }


    private fun startScanDevice(){
        try {
            bluetoothClient.startScan(30000, onEndScan = {
                scanning.value = false
            }) { device ->
                if(!scanDeviceList.contains(device)){
                    scanDeviceList.add(device)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to start Bluetooth scan due to an exception.")
            showToast("$e")
        }
    }



    private fun stopScanDevice(){
        bluetoothClient.stopScan()
    }

    private fun connectDevice(device: Device){
        stopScanDevice()
        bluetoothClient.connect(device, mtu){ connectState ->
            Timber.d("MainActivity --> connectState: $connectState")
            if(connectState == ConnectState.CONNECTING){
                showLoading(false)
            } else {
                hideLoading()
            }
            if(connectState == ConnectState.CONNECTED){
                showScanDialog = false
                stopScanDevice()
                deviceName = device.name ?: device.address
                showToast("连接成功！")
                supportedServices()
            } else if(connectState == ConnectState.DISCONNECTED){
                deviceName = ""
                assignService(null)
            }
        }
    }

    private fun supportedServices(){
        serviceList.clear()
        val services = bluetoothClient.supportedServices()
        if(services != null){
            serviceList.addAll(services)
        }
        val service = if(serviceList.isNotEmpty()){
            serviceList.first()
        } else {
            null
        }
        assignService(service)
    }

    private fun assignService(service: Service?){
        this.service = service
        receiveCharacteristicList.clear()
        sendCharacteristicList.clear()
        readCharacteristicList.clear()
        if(service != null){
            bluetoothClient.assignService(service)
            service.characteristics?.forEach { characteristic ->
                if(characteristic.properties.contains(Characteristic.Property.NOTIFY)){
                    receiveCharacteristicList.add(characteristic)
                } else if(characteristic.properties.contains(Characteristic.Property.WRITE)){
                    sendCharacteristicList.add(characteristic)
                } else if(characteristic.properties.contains(Characteristic.Property.READ)){
                    readCharacteristicList.add(characteristic)
                }
            }
            receiveCharacteristic = if(receiveCharacteristicList.isNotEmpty()){
                receiveCharacteristicList[0]
            } else {
                null
            }
            sendCharacteristic = if(sendCharacteristicList.isNotEmpty()){
                sendCharacteristicList[0]
            } else {
                null
            }
            readCharacteristic = if(readCharacteristicList.isNotEmpty()){
                readCharacteristicList[0]
            } else {
                null
            }
        } else {
            receiveCharacteristic = null
            sendCharacteristic = null
            readCharacteristic = null
            readDataStr = ""
        }
        if(receiveCharacteristic != null){
            receiveData()
        }
    }

    private fun receiveData(){
        if(bluetoothType == ClientType.BLE && receiveCharacteristic == null){
            return
        }
        bluetoothClient.receiveData(receiveCharacteristic?.uuid) { data ->
            receivePackets++
            Timber.d("receiveData: ${data.toHex()}")
        }
    }

    private fun sendData(data: ByteArray){
        if(bluetoothType == ClientType.BLE && sendCharacteristic == null){
            showToast("请选择SendUid!")
            return
        }
        bluetoothClient.sendData(sendCharacteristic?.uuid, data){ success, _ ->
            if(success){
                showToast("数据发送成功！")
            } else {
                showToast("数据发送失败！")
            }
        }
    }

    private fun readData(){
        if(bluetoothType == ClientType.BLE && readCharacteristic == null){
            showToast("请选择ReadUid!")
            return
        }
        bluetoothClient.readData(readCharacteristic?.uuid){ success, data ->
            if(success){
                readDataStr = String(data!!)
                showToast("数据读取成功！")
            } else {
                showToast("数据读取失败！")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothClient.release()
    }
}

