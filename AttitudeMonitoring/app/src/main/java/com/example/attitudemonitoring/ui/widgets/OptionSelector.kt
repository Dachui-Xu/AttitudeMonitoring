package com.example.attitudemonitoring.ui.widgets

import DataReaderViewModel
import DataReaderViewModel.Option
import MultipleLineChartsViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.attitudemonitoring.ui.theme.AttitudeMonitoringTheme
import timber.log.Timber

@Composable
fun OptionSelector(viewModel: DataReaderViewModel) {
    val selectedOption by viewModel.selectedOption.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OptionButton(
                text = "CNN",
                isSelected = selectedOption == Option.MODEL,
                onClick = { viewModel.selectOption(Option.MODEL)
                            viewModel.optionModel()
                Timber.d("CNN selected")}
            )
            OptionButton(
                text = "Threshold",
                isSelected = selectedOption == Option.YAML,
                onClick = { viewModel.selectOption(Option.YAML)
                            viewModel.optionRange()
                Timber.d("Threshold selected")}
            )
        }
    }
}

@Composable
fun OptionButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .height(48.dp)
            .width(120.dp)
    ) {
        Text(text = text, color = if (isSelected) Color.White else Color.Black)
    }
}



/*
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AttitudeMonitoringTheme {
        OptionSelector()
    }
}*/
