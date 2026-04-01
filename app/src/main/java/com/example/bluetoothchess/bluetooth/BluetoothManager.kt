package com.example.bluetoothchess.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothManager(private val bluetoothAdapter: BluetoothAdapter?) {
    private val APP_UUID: UUID = UUID.fromString("0a64cb9a-ce25-4529-847e-97ec5aa02525")
    private val APP_NAME = "BluetoothChess"
    
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val _incomingMessages = MutableSharedFlow<String>()
    val incomingMessages: SharedFlow<String> = _incomingMessages

    private val _connectionState = MutableSharedFlow<ConnectionState>()
    val connectionState: SharedFlow<ConnectionState> = _connectionState

    enum class ConnectionState { CONNECTED, DISCONNECTED, ERROR }

    @SuppressLint("MissingPermission")
    suspend fun startHost() = withContext(Dispatchers.IO) {
        Log.d("BTManager", "Starting host...")
        try {
            serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID)
            clientSocket = serverSocket?.accept()
            manageConnectedSocket(clientSocket)
        } catch (e: Exception) {
            _connectionState.emit(ConnectionState.ERROR)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(device: BluetoothDevice) = withContext(Dispatchers.IO) {
        Log.d("BTManager", "Connecting to device ${device.name}")
        try {
            clientSocket = device.createRfcommSocketToServiceRecord(APP_UUID)
            bluetoothAdapter?.cancelDiscovery()
            clientSocket?.connect()
            manageConnectedSocket(clientSocket)
        } catch (e: Exception) {
            _connectionState.emit(ConnectionState.ERROR)
        }
    }

    private suspend fun manageConnectedSocket(socket: BluetoothSocket?) {
        socket?.let {
            try {
                inputStream = it.inputStream
                outputStream = it.outputStream
                _connectionState.emit(ConnectionState.CONNECTED)
                
                val buffer = ByteArray(1024)
                var bytes: Int
                while (true) {
                    bytes = inputStream?.read(buffer) ?: -1
                    if (bytes > 0) {
                        val message = String(buffer, 0, bytes)
                        _incomingMessages.emit(message)
                    } else if (bytes == -1) {
                        break
                    }
                }
            } catch (e: Exception) {
                // Ignore disconnect exceptions gracefully
            }
        }
        _connectionState.emit(ConnectionState.DISCONNECTED)
    }

    suspend fun sendMessage(message: String) = withContext(Dispatchers.IO) {
        try {
            outputStream?.write(message.toByteArray())
        } catch (e: Exception) {
            _connectionState.emit(ConnectionState.DISCONNECTED)
        }
    }

    fun cleanup() {
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        try {
            clientSocket?.close()
        } catch (e: Exception) {}
        serverSocket = null
        clientSocket = null
        inputStream = null
        outputStream = null
    }
}
