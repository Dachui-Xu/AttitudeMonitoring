package com.example.attitudemonitoring.ui.widgets

import MultipleLineChartsViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.attitudemonitoring.R
import com.example.attitudemonitoring.bean.Animation
import timber.log.Timber


@Composable
fun LottieAnimationView(viewModel: MultipleLineChartsViewModel,modifier: Modifier, onClick:()->Unit) {
    val animationResId = viewModel.currentMode.getAnimation(viewModel.status).resourceId
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animationResId))
    val progress = rememberLottieAnimatable()

    LaunchedEffect(composition) {
        if (composition != null) {
            progress.animate(
                composition,
                iterations = LottieConstants.IterateForever
            )
        }
    }

    Box(
        modifier = modifier.fillMaxWidth() .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(composition = composition,
            progress = { progress.progress },
            modifier = Modifier.fillMaxHeight()
        )
    }
}


