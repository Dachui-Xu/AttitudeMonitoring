package com.example.attitudemonitoring.util

import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import com.example.attitudemonitoring.R
import kotlinx.coroutines.*

class TimedVibrationManager(private val context: Context) {
    private var vibrationJob: Job? = null
    private var remainingTimeMillis: Long? = null
    private var intervalMillis: Long = 0L

    fun startTimedVibration(intervalMillis: Long) {
        this.intervalMillis = intervalMillis
        // Cancel any existing vibration job
        vibrationJob?.cancel()

        // Start a new vibration job
        vibrationJob = CoroutineScope(Dispatchers.Main).launch {
            val vibrator = getVibrator()

            while (isActive) {
                delay(intervalMillis)
                performVibration(vibrator)
                showCustomToast("⏰ Please Note Your Neck Posture")
            }
        }
    }

    private fun showCustomToast(message: String) {
        val inflater = LayoutInflater.from(context)
        val layout: View = inflater.inflate(R.layout.custom_toast, null)

        // Customize the text and icon here
        val text: TextView = layout.findViewById(R.id.toast_text)
        text.text = message
        text.setTextColor(Color.RED)  // Change text color


        val toast = Toast(context)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 245)  // Set position
        toast.show()
    }


    fun stopTimedVibration() {
        vibrationJob?.cancel()
        remainingTimeMillis = null
    }

    fun pauseTimedVibration() {
        vibrationJob?.cancel()
       // remainingTimeMillis = intervalMillis - (System.currentTimeMillis() % intervalMillis)
    }

    fun resumeTimedVibration() {
        remainingTimeMillis?.let {
            vibrationJob = CoroutineScope(Dispatchers.Main).launch {
                delay(it)
                performVibration(getVibrator())

                startTimedVibration(intervalMillis) // Continue with the original interval
            }
            remainingTimeMillis = null
        }
    }

    fun judgeVibrator(mode: MultipleLineChartsViewModel.Mode, status: String) {
        val intervalMillis = calculateVibrationDuration(mode, status)
        if (intervalMillis > 0) {
            startTimedVibration(intervalMillis)
        } else {
            stopTimedVibration()
        }
    }

    private suspend fun performVibration(vibrator: Vibrator) {
        // Play notification sound
        val mediaPlayer = MediaPlayer.create(context, R.raw.notification)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(500)
        }
    }

    private fun getVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun calculateVibrationDuration(mode: MultipleLineChartsViewModel.Mode, status: String): Long {
        return when (mode) {
            MultipleLineChartsViewModel.Mode.WORK -> when (status) {
                "normal" -> 0L
                "down" -> 120000L
                "large down" -> 60000L
                "left" -> 5000L
                "right" -> 5000L
                "up" -> 4000L
                else -> 0L
            }
            MultipleLineChartsViewModel.Mode.DRIVE -> when (status) {
                "normal" -> 0L
                "left" -> 3000L
                "right" -> 3000L
                "down" -> 3000L
                "up" -> 3000L
                "large down" -> 2000L
                else -> 0L
            }
        }
    }
}

