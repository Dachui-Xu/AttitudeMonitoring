package com.example.attitudemonitoring.ui.widgets

import MultipleLineChartsViewModel
import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.attitudemonitoring.handler.LineChartHandler
import com.example.attitudemonitoring.viewModel.ViewModelFactory

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attitudemonitoring.R
import com.example.attitudemonitoring.util.TimedVibrationManager

/**
 *  单折线图UI
 */
@Composable
fun LineChartView(lineChartHandler: LineChartHandler, modifier: Modifier){
    AndroidView(
        modifier = modifier,
        factory = {
            lineChartHandler.lineChart
        }
    )

}

/**
 * 多折线图UI
 */
@Composable
fun MultipleLineChartsView(viewModel: MultipleLineChartsViewModel) {
    val lineChartHandlers = remember { viewModel.getHandlers() }
    val isRunning by remember { viewModel.isRunning }

    LaunchedEffect(Unit) {
        if (isRunning) {
            viewModel.resumeAll()
        }
    }
//
//    DisposableEffect(Unit) {
//        onDispose {
//            viewModel.pauseAll()
//        }
//    }

    Column(modifier = Modifier.fillMaxSize()) {

        LottieAnimationView(viewModel,
                            Modifier
                                .fillMaxWidth()
                                .weight(3f)
                                .graphicsLayer{alpha = 1f}){
            viewModel.switchMode()
        }
        Text(
            text = "${viewModel.currentMode.modeName} Status: ${viewModel.status}, ${viewModel.duration}",
            modifier = Modifier
                .wrapContentSize(Alignment.Center)
                .padding(6.dp)
        )


        LazyColumn(modifier = Modifier.weight(5f)) {
            items(lineChartHandlers) { handler ->
                LineChartView(handler, Modifier.fillMaxWidth().height(90.dp).weight(1f))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (isRunning) {
                        viewModel.pauseAll()
                    } else {
                        viewModel.resumeAll()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp) // Adjust horizontal padding as needed
            ) {
                Text(text = if (isRunning) "Pause" else "Start", fontSize = 10.sp)
            }

            Button(
                onClick = {
                    if (!isRunning) {
                        viewModel.resetAll()
                        viewModel.startAll()
                    } else {
                        viewModel.stopAll()
                        viewModel.clearAll()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp) // Adjust horizontal padding as needed
            ) {
                Text(text = if (!isRunning) "Restart" else "Clear", fontSize = 10.sp)
            }
            Button(
                onClick = {
                    viewModel.shareSensorData()
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp) // Adjust horizontal padding as needed
            ) {
                Text(text = "Export", fontSize = 10.sp)
            }
        }
    }
}




