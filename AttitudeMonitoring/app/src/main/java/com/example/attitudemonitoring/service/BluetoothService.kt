package com.example.attitudemonitoring.service

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

const val MESSAGE_READ: Int = 0
const val MESSAGE_WRITE: Int = 1
const val MESSAGE_TOAST: Int = 2

class BluetoothService : Service() {
    private val bluetoothManager: BluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var connectThread: ConnectThread? = null
    private lateinit var handler: Handler

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val deviceAddress = intent?.getStringExtra("device_address")
        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        device?.let {
            if (checkBluetoothPermissions()) {
                connectToDevice(it)
            } else {
                // Handling the case where permission is not granted
            }
        }
        return START_NOT_STICKY
    }

    private fun checkBluetoothPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }

    private fun connectToDevice(device: BluetoothDevice) {
        connectThread = ConnectThread(device, handler).apply {
            start()
        }
    }

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(private val device: BluetoothDevice, private val handler: Handler) : Thread() {
        private val socket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
        }
        private val mmBuffer: ByteArray = ByteArray(1024) // Buffer store for the stream

        override fun run() {
            bluetoothAdapter?.cancelDiscovery()
            try {
                socket?.connect()
                manageConnectedSocket(socket)
            } catch (e: IOException) {
                socket?.close()
                return
            }
        }

        private fun manageConnectedSocket(socket: BluetoothSocket?) {
            val inputStream: InputStream = socket!!.inputStream
            val outputStream: OutputStream = socket.outputStream

            while (true) {
                try {
                    val bytes = inputStream.read(mmBuffer)
                    handler.obtainMessage(MESSAGE_READ, bytes, -1, mmBuffer.copyOf(bytes)).sendToTarget()
                } catch (e: IOException) {
                    break
                }
            }
        }

        fun write(input: String) {
            val bytes = input.toByteArray()
            try {
                socket!!.outputStream.write(bytes)
                handler.obtainMessage(MESSAGE_WRITE, -1, -1, bytes).sendToTarget()
            } catch (e: IOException) {
                handler.obtainMessage(MESSAGE_TOAST).sendToTarget()
            }
        }

        fun cancel() {
            try {
                socket?.close()
            } catch (_: IOException) {
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connectThread?.cancel()
    }
}
