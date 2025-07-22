package com.tutorial.idarabi.websocketapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class WebSocketViewModel : ViewModel() {
    private val webSocketManager = StompWebSocketManager()
    
    val connectionState: StateFlow<ConnectionState> = webSocketManager.connectionState
    val messages: StateFlow<List<ChatMessage>> = webSocketManager.messages
    val errorMessage: StateFlow<String?> = webSocketManager.errorMessage

    private var connectJob: kotlinx.coroutines.Job? = null

    init {
        Log.i("idarabi","WebSocketViewModel initialized")
        // Start initial connection loop only once
        startConnectLoop()
    }
    
    private fun startConnectLoop() {
        if (connectJob?.isActive == true) return // Only start if not already running
        connectJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                if (connectionState.value == ConnectionState.DISCONNECTED) {
                    try {
                        webSocketManager.connect()
                        withContext(Dispatchers.Main) {
                            Log.i("idarabi","WebSocket connection initiated successfully")
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Log.e("idarabi","Failed to connect to WebSocket, will retry in 5 seconds", e)
                        }
                        kotlinx.coroutines.delay(5000)
                    }
                } else {
                    // Wait for state to change if already connected
                    kotlinx.coroutines.delay(1000)
                }
            }
        }
    }

    fun connect() {
        startConnectLoop()
    }

    fun disconnect() {
        Log.i("idarabi","Attempting to disconnect from WebSocket")
        viewModelScope.launch {
            try {
                webSocketManager.disconnect()
                Log.i("idarabi","WebSocket disconnection initiated successfully")
            } catch (e: Exception) {
                Log.e("idarabi","Error occurred during WebSocket disconnection", e)
                throw e
            }
        }
    }
    
    fun sendMessage(message: String) {
        Log.i("idarabi","Attempting to send message: $message")
        viewModelScope.launch {
            try {
                if (message.isBlank()) {
                    Log.w("idarabi","Attempted to send empty or blank message")
                    return@launch
                }

                webSocketManager.sendMessage(message)
                Log.i("idarabi","Message sent successfully: $message")
            } catch (e: Exception) {
                Log.e("idarabi","Failed to send message: $message", e)
                throw e
            }
        }
    }
    
    fun clearMessages() {
        Log.i("idarabi","Attempting to clear messages")
        viewModelScope.launch {
            try {
                webSocketManager.clearMessages()
                Log.i("idarabi","Messages cleared successfully")
            } catch (e: Exception) {
                Log.e("idarabi","Failed to clear messages", e)
                throw e
            }
        }
    }
    
    override fun onCleared() {
        Log.i("idarabi","WebSocketViewModel is being cleared")
        try {
            webSocketManager.disconnect()
            Log.i("idarabi","WebSocket disconnected during ViewModel cleanup")
        } catch (e: Exception) {
            Log.e("idarabi","Error during ViewModel cleanup", e)
            throw e
        } finally {
            super.onCleared()
            Log.i("idarabi","WebSocketViewModel cleanup completed")
        }
    }
}