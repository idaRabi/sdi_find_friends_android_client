package com.tutorial.idarabi.websocketapp

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.StompHeader
import kotlin.coroutines.resume

class StompWebSocketManager {
    private var stompClient: StompClient? = null
    private var isConnected = false
    private var currentUsername: String? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    companion object {
        private const val TAG = "StompWebSocketManager"
        private const val SERVER_URL = "ws://10.0.2.2:8080/ws" // For Android emulator
        // Use "ws://YOUR_ACTUAL_IP:8080/ws" for real device
        private const val CONNECTION_TIMEOUT = 10000L // 10 seconds

        // STOMP destinations
        private const val MESSAGE_TOPIC = "/topic/message-topic"
        private const val APP_DESTINATION = "/app/hello-message-mapping"
//        private const val USER_ADD_DESTINATION = "/app/chat.addUser"
    }

    suspend fun connect(username: String? = null) {
        if (_connectionState.value == ConnectionState.CONNECTING ||
            _connectionState.value == ConnectionState.CONNECTED) {
            return
        }

        try {
            _connectionState.value = ConnectionState.CONNECTING
            _errorMessage.value = null

            // Configure connection headers if needed
            val headers = mutableListOf<StompHeader>()
            username?.let {
                headers.add(StompHeader("username", it))
            }
            withTimeout(CONNECTION_TIMEOUT) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    // Create STOMP client
                    stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, SERVER_URL)
                    stompClient?.connect()
                    isConnected = true
                    _connectionState.value = ConnectionState.CONNECTED
                    subscribeToMessageTopic()
                    continuation.resume(Unit);
                    continuation.invokeOnCancellation {
                        _connectionState.value = ConnectionState.DISCONNECTED
                        stompClient?.disconnect()
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
        try {
            stompClient?.disconnect()
            isConnected = false
            currentUsername = null
            _connectionState.value = ConnectionState.DISCONNECTED
            addMessage(ChatMessage("System", "Disconnected from STOMP server", MessageType.SYSTEM))
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect: ${e.message}", e)
        }
    }

    fun sendMessage(message: String, sender: String = "You") {
        if (isConnected && message.isNotBlank()) {
            try {
                // Create message payload - adjust format based on your Spring Boot backend
                val payload = createMessagePayload(sender, message)

                val result = stompClient?.send(APP_DESTINATION, payload)
                result?.subscribe(
                    {
                        // On complete: This block is executed if the operation was successful.
                        println("✅ STOMP message sent successfully!")
                        // You can perform further actions here, e.g., update UI
                    },
                    { error ->
                        // On error: This block is executed if the operation failed.
                        println("❌ Failed to send STOMP message: ${error.message}")
                        error.printStackTrace()
                        // You can handle the error here, e.g., display an error message to the user
                    }
                )?.dispose()
                Log.d(TAG, "Message sent: $message")
                addMessage(ChatMessage(sender, message, MessageType.SENT))
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message: ${e.message}", e)
                addMessage(ChatMessage("System", "Error sending message: ${e.message}", MessageType.ERROR))
            }
        } else {
            Log.w(TAG, "Cannot send message - not connected or message is blank")
            addMessage(ChatMessage("System", "Cannot send message - not connected", MessageType.ERROR))
        }
    }

    private fun subscribeToMessageTopic() {
        stompClient?.topic(MESSAGE_TOPIC)?.subscribe { topicMessage ->
            Log.d(TAG, topicMessage.payload)
            addMessage(ChatMessage("System", topicMessage.payload, MessageType.RECEIVED))
        }
    }

    /*private fun addUser(username: String) {
        try {
            val payload = createUserPayload(username)
            stompClient?.send(USER_ADD_DESTINATION, payload)
            Log.d(TAG, "User added: $username")
            addMessage(ChatMessage("System", "User $username joined the chat", MessageType.SYSTEM))
        } catch (e: Exception) {
            Log.e(TAG, "Error adding user: ${e.message}", e)
            addMessage(ChatMessage("System", "Error adding user: ${e.message}", MessageType.ERROR))
        }
    }*/

    private fun createMessagePayload(sender: String, content: String): String {
        // Create JSON payload for Spring Boot STOMP backend
        // Adjust this based on your backend message format
        return """
        {
            "sender": "$sender",
            "content": "$content",
            "type": "CHAT"
        }
        """.trimIndent()
    }

    private fun createUserPayload(username: String): String {
        // Create JSON payload for adding user
        return """
        {
            "sender": "$username",
            "type": "JOIN"
        }
        """.trimIndent()
    }

    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    fun cleanup() {
        disconnect()
        stompClient = null
    }

    // Additional STOMP-specific methods

/*
    fun subscribeToUserDestination(username: String) {
        stompClient?.let { client ->
            try {
                val userDestination = "/user/$username/queue/reply"
                client.subscribe(userDestination)
                Log.d(TAG, "Successfully subscribed to user destination: $userDestination")
            } catch (e: Exception) {
                Log.e(TAG, "Error subscribing to user destination: ${e.message}", e)
                addMessage(ChatMessage("System", "Error subscribing to user destination: ${e.message}", MessageType.ERROR))
            }
        }
    }
*/

    fun sendPrivateMessage(recipient: String, message: String, sender: String = "You") {
        if (isConnected && message.isNotBlank()) {
            try {
                val payload = """
                {
                    "sender": "$sender",
                    "content": "$message",
                    "recipient": "$recipient",
                    "type": "PRIVATE"
                }
                """.trimIndent()

                stompClient?.send("/app/chat.sendPrivateMessage", payload)
                Log.d(TAG, "Private message sent to $recipient")
                addMessage(ChatMessage("$sender → $recipient", message, MessageType.SENT))
            } catch (e: Exception) {
                Log.e(TAG, "Error sending private message: ${e.message}", e)
                addMessage(ChatMessage("System", "Error sending private message: ${e.message}", MessageType.ERROR))
            }
        } else {
            Log.w(TAG, "Cannot send private message - not connected or message is blank")
        }
    }
}
