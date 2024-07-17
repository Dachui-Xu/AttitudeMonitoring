package com.example.attitudemonitoring.ui.widgets

import DataReaderViewModel
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attitudemonitoring.viewModel.ViewModelFactory

@Composable
fun DataReaderScreen(modifier: Modifier = Modifier, viewModel: DataReaderViewModel) {
    val dataHistory = viewModel.dataHistory
    val scrollState = rememberLazyListState()
    val isCollecting = viewModel.isCollecting
    // Scroll data to the bottom
    LaunchedEffect(dataHistory) {
        if (dataHistory.isNotEmpty()) {
            scrollState.scrollToItem(dataHistory.size - 1)
        }
    }

    Column(modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
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

            // Start/Stop Reading Button
            Button(
                onClick = {
                    viewModel.toggleDataReading()
                },
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text(text = if (isCollecting) "Stop Collecting" else "Start Collecting")
            }

        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
//            Button(onClick = {
//                // Simulate data action
//            }) {
//                Text(text = "模拟数据")
//            }
            Button(onClick = { viewModel.clearDataHistory() }) {
                Text(text = "Clear")
            }
        }
        OptionSelector(viewModel)
    }
}


