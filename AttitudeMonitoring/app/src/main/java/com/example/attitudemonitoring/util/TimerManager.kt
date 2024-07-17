package com.example.attitudemonitoring.util

import kotlinx.coroutines.*

class TimerManager(private val onUpdate: (String) -> Unit) {
    private var timerJob: Job? = null
    private var startTimeMillis: Long = 0L
    private var elapsedTimeMillis: Long = 0L
    private var pausedTimeMillis: Long = 0L
    private var isPaused: Boolean = false

    fun start() {
        timerJob?.cancel()
        if (!isPaused) {
            startTimeMillis = System.currentTimeMillis() - elapsedTimeMillis
        } else {
            pausedTimeMillis = System.currentTimeMillis() - startTimeMillis
        }
        timerJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                if (!isPaused) {
                    elapsedTimeMillis = System.currentTimeMillis() - startTimeMillis
                }
                val formattedTime = formatElapsedTime(elapsedTimeMillis)
                onUpdate(formattedTime)
                delay(1000) // Update every second
            }
        }
    }

    fun pause() {
        isPaused = true
        pausedTimeMillis = System.currentTimeMillis() - startTimeMillis
        timerJob?.cancel()
    }

    fun resume() {
        isPaused = false
        startTimeMillis = System.currentTimeMillis() - pausedTimeMillis
        start()
    }

    fun stop() {
        timerJob?.cancel()
        isPaused = false
        elapsedTimeMillis = 0L
        pausedTimeMillis = 0L
    }

    private fun formatElapsedTime(timeMillis: Long): String {
        val seconds = timeMillis / 1000 % 60
        val minutes = timeMillis / (1000 * 60) % 60
        val hours = timeMillis / (1000 * 60 * 60) % 24
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun reset() {
        stop()
        startTimeMillis = System.currentTimeMillis()
    }
}
