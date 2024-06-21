package com.example.attitudemonitoring.bean

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date



data class LogoutInfo(
    val msg: String,
    val priority: Int = Log.DEBUG,
    val times: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val lineNumber: Int = Throwable().stackTrace[1].lineNumber,
    val fileName: String = Throwable().stackTrace[1].fileName
)


