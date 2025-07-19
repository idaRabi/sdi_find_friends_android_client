package com.tutorial.idarabi.websocketapp

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WebSocketManager {
    private var webSocket: WebSocket? = null
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    companion object {
        private const val TAG = "WebSocketManager"
        private const val SERVER_URL = "ws://10.0.2.2:8080/ws" // For Android emulator
        // Use "ws://YOUR_ACTUAL_IP:8080/ws" for real device
        private const val CONNECTION_TIMEOUT = 10000L // 10 seconds
    }

    suspend fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTING ||
            _connectionState.value == ConnectionState.CONNECTED) {
            return
        }

        try {
            _connectionState.value = ConnectionState.CONNECTING
            _errorMessage.value = null

            val request = Request.Builder()
                .url(SERVER_URL)
                .build()

            // Use suspendCancellableCoroutine to make it coroutine-compatible
            withTimeout(CONNECTION_TIMEOUT) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val listener = object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            Log.d(TAG, "WebSocket connected")
                            _connectionState.value = ConnectionState.CONNECTED
                            addMessage(ChatMessage("System", "Connected to server", MessageType.SYSTEM))
                            continuation.resume(Unit)
                        }

                        override fun onMessage(webSocket: WebSocket, text: String) {
                            Log.d(TAG, "Message received: $text")
                            addMessage(ChatMessage("Server", text, MessageType.RECEIVED))
                        }

                        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                            Log.d(TAG, "Binary message received: ${bytes.hex()}")
                            addMessage(ChatMessage("Server", "Binary message received", MessageType.RECEIVED))
                        }

                        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                            Log.d(TAG, "WebSocket closing: $reason")
                            webSocket.close(1000, null)
                        }

                        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                            Log.d(TAG, "WebSocket closed: $reason")
                            _connectionState.value = ConnectionState.DISCONNECTED
                            addMessage(ChatMessage("System", "Disconnected from server", MessageType.SYSTEM))
                        }

                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            Log.e(TAG, "WebSocket error: ${t.message}", t)
                            _connectionState.value = ConnectionState.DISCONNECTED
                            _errorMessage.value = t.message ?: "Unknown error"
                            addMessage(ChatMessage("System", "Error: ${t.message}", MessageType.ERROR))

                            if (continuation.isActive) {
                                continuation.resumeWithException(t)
                            }
                        }
                    }

                    webSocket = okHttpClient.newWebSocket(request, listener)

                    continuation.invokeOnCancellation {
                        webSocket?.close(1000, "Cancelled")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect: ${e.message}", e)
            _connectionState.value = ConnectionState.DISCONNECTED
            _errorMessage.value = e.message
            throw e
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun sendMessage(message: String) {
        if (_connectionState.value == ConnectionState.CONNECTED && message.isNotBlank()) {
            val success = webSocket?.send(message) ?: false
            if (success) {
                addMessage(ChatMessage("You", message, MessageType.SENT))
                Log.d(TAG, "Message sent: $message")
            } else {
                Log.e(TAG, "Failed to send message: $message")
                addMessage(ChatMessage("System", "Failed to send message", MessageType.ERROR))
            }
        }
    }

    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    fun cleanup() {
        webSocket?.close(1000, "Cleanup")
        okHttpClient.dispatcher.executorService.shutdown()
    }
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

data class ChatMessage(
    val sender: String,
    val content: String,
    val type: MessageType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageType {
    SENT,
    RECEIVED,
    SYSTEM,
    ERROR
}