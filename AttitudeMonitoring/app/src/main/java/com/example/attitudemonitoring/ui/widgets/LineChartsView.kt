package com.example.attitudemonitoring.ui.widgets

import MultipleLineChartsViewModel
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import com.example.attitudemonitoring.bean.Sensor
import com.example.attitudemonitoring.handler.LineChartHandler
import com.example.attitudemonitoring.handler.MultiLineChartHandler
import com.example.attitudemonitoring.viewModel.ViewModelFactory
import com.github.mikephil.charting.charts.LineChart
import timber.log.Timber

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider

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
fun MultipleLineChartsView() {
    val context = LocalContext.current
    val viewModel: MultipleLineChartsViewModel = viewModel(factory = ViewModelFactory(context))
    val lineChartHandlers = remember { viewModel.getHandlers() }
    val isRunning by remember { viewModel.isRunning }


    LaunchedEffect(Unit) {
        if (isRunning) {
            viewModel.resumeAll()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.pauseAll()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        lineChartHandlers.forEach { handler ->
            LineChartView(handler,
                Modifier
                    .fillMaxWidth()
                    .weight(1f))
        }

        Row( modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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
                modifier = Modifier.weight(1f)
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
                modifier = Modifier.weight(1f)
            ) {
                Text(text = if (!isRunning) "Restart" else "Clear", fontSize = 10.sp)
               // Timber.d("--清除，isRunning：$isRunning,isCleared: $isCleared")
            }

            Button(onClick = {
                // Save action here
            }) {
                Text(text = "Send", fontSize = 10.sp)

            }

            Button(onClick = {
                // Export action here
                viewModel.shareSensorData()
            }) {
                Text(text = "Export", fontSize = 10.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MultipleLineChartsPreview() {
    MultipleLineChartsView()
}



