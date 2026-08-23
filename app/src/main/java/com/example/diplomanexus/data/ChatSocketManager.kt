package com.example.diplomanexus.data

import android.util.Log
import com.example.diplomanexus.api.MessageDto
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Singleton that implements a custom Socket.IO v4 client over raw OkHttp WebSockets.
 * This avoids external dependency download failures while maintaining identical API flows.
 */
object ChatSocketManager {

    private const val TAG = "ChatSocketManager"
    private const val SERVER_URL = "ws://10.0.2.2:5000/socket.io/?EIO=4&transport=websocket"
    private val gson = Gson()

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // Event Flows matching original signature
    private val newMessageFlow = MutableSharedFlow<MessageDto>(extraBufferCapacity = 100)
    private val typingFlow = MutableSharedFlow<TypingEvent>(extraBufferCapacity = 100)
    private val stopTypingFlow = MutableSharedFlow<StopTypingEvent>(extraBufferCapacity = 100)
    private val userOnlineFlow = MutableSharedFlow<Int>(extraBufferCapacity = 100)
    private val userOfflineFlow = MutableSharedFlow<Int>(extraBufferCapacity = 100)
    private val messagesReadFlow = MutableSharedFlow<MessagesReadEvent>(extraBufferCapacity = 100)

    fun newMessageFlow(): Flow<MessageDto> = newMessageFlow
    fun typingFlow(): Flow<TypingEvent> = typingFlow
    fun stopTypingFlow(): Flow<StopTypingEvent> = stopTypingFlow
    fun userOnlineFlow(): Flow<Int> = userOnlineFlow
    fun userOfflineFlow(): Flow<Int> = userOfflineFlow
    fun messagesReadFlow(): Flow<MessagesReadEvent> = messagesReadFlow

    data class TypingEvent(val room_id: Int, val user_id: Int, val username: String)
    data class StopTypingEvent(val room_id: Int, val user_id: Int)
    data class MessagesReadEvent(val room_id: Int, val reader_id: Int)

    // Reconnection and scope management
    private val scope = CoroutineScope(Dispatchers.Default)
    private var reconnectJob: Job? = null
    private var isClosedExplicitly = false
    private var connectionToken: String? = null

    // Typing debounce local state
    private var lastTypingEmitTime = 0L
    private var currentTypingRoomId: Int? = null

    /**
     * Connect to the Socket.IO server.
     */
    fun connect(token: String) {
        connectionToken = token
        isClosedExplicitly = false
        if (_isConnected.value || webSocket != null) return

        val requestUrl = "$SERVER_URL&token=$token"
        val request = Request.Builder().url(requestUrl).build()

        Log.d(TAG, "Connecting to WebSocket: $requestUrl")
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket transport opened")
                // Socket.IO v4 opens Engine.IO connection first. 
                // We'll receive packet '0' containing handshake info, which triggers our response.
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    handleFrame(text)
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling frame: $text", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
                _isConnected.value = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                _isConnected.value = false
                this@ChatSocketManager.webSocket = null
                triggerReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                _isConnected.value = false
                this@ChatSocketManager.webSocket = null
                triggerReconnect()
            }
        })
    }

    /**
     * Disconnect from the server.
     */
    fun disconnect() {
        Log.d(TAG, "Disconnect requested")
        isClosedExplicitly = true
        reconnectJob?.cancel()
        webSocket?.close(1000, "Disconnect requested")
        webSocket = null
        _isConnected.value = false
        connectionToken = null
    }

    private fun triggerReconnect() {
        if (isClosedExplicitly) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(3000)
            val token = connectionToken
            if (token != null && !isClosedExplicitly) {
                Log.d(TAG, "Attempting reconnect...")
                connect(token)
            }
        }
    }

    private fun handleFrame(text: String) {
        if (text.isEmpty()) return
        val type = text[0]
        when (type) {
            '0' -> {
                // Engine.IO Open packet. Send Socket.IO connection packet (40) with auth token payload
                Log.d(TAG, "Engine.IO handshake completed. Sending Socket.IO connect...")
                val authObj = JSONObject().put("token", connectionToken)
                webSocket?.send("40$authObj")
            }
            '2' -> {
                // Engine.IO Ping request from server. Must reply with Pong (3)
                webSocket?.send("3")
            }
            '4' -> {
                // Socket.IO packets
                if (text.length < 2) return
                val subType = text[1]
                when (subType) {
                    '0' -> {
                        // Connect confirmation
                        _isConnected.value = true
                        Log.d(TAG, "Socket.IO connected successfully")
                    }
                    '1' -> {
                        // Disconnected
                        _isConnected.value = false
                        Log.d(TAG, "Socket.IO disconnected by server")
                    }
                    '2' -> {
                        // Message / Event: format starts with "42" followed by JSON array [event_name, data_object]
                        if (text.length < 3) return
                        val jsonStr = text.substring(2)
                        parseAndEmitEvent(jsonStr)
                    }
                }
            }
        }
    }

    private fun parseAndEmitEvent(jsonStr: String) {
        try {
            val jsonArray = JSONArray(jsonStr)
            if (jsonArray.length() < 2) return
            val eventName = jsonArray.getString(0)
            val dataObj = jsonArray.getJSONObject(1)

            scope.launch {
                when (eventName) {
                    "new_message" -> {
                        val msg = gson.fromJson(dataObj.toString(), MessageDto::class.java)
                        newMessageFlow.emit(msg)
                    }
                    "user_typing" -> {
                        typingFlow.emit(TypingEvent(
                            room_id = dataObj.getInt("room_id"),
                            user_id = dataObj.getInt("user_id"),
                            username = dataObj.getString("username")
                        ))
                    }
                    "user_stop_typing" -> {
                        stopTypingFlow.emit(StopTypingEvent(
                            room_id = dataObj.getInt("room_id"),
                            user_id = dataObj.getInt("user_id")
                        ))
                    }
                    "user_online" -> {
                        userOnlineFlow.emit(dataObj.getInt("user_id"))
                    }
                    "user_offline" -> {
                        userOfflineFlow.emit(dataObj.getInt("user_id"))
                    }
                    "messages_read" -> {
                        messagesReadFlow.emit(MessagesReadEvent(
                            room_id = dataObj.getInt("room_id"),
                            reader_id = dataObj.getInt("reader_id")
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing event json: $jsonStr", e)
        }
    }

    // ─── Socket Emitting Functions ───

    fun sendMessage(roomId: Int, textContent: String, messageType: String = "text", mediaUrl: String? = null) {
        val data = JSONObject().apply {
            put("room_id", roomId)
            put("text_content", textContent)
            put("message_type", messageType)
            if (mediaUrl != null) put("media_url", mediaUrl)
        }
        sendSocketEvent("send_message", data)
    }

    fun emitTyping(roomId: Int) {
        val now = System.currentTimeMillis()
        if (currentTypingRoomId == roomId && now - lastTypingEmitTime < 3000) return

        currentTypingRoomId = roomId
        lastTypingEmitTime = now
        sendSocketEvent("typing", JSONObject().put("room_id", roomId))
    }

    fun emitStopTyping(roomId: Int) {
        currentTypingRoomId = null
        sendSocketEvent("stop_typing", JSONObject().put("room_id", roomId))
    }

    fun markRead(roomId: Int) {
        sendSocketEvent("message_read", JSONObject().put("room_id", roomId))
    }

    fun joinRoom(roomId: Int) {
        sendSocketEvent("join_room", JSONObject().put("room_id", roomId))
    }

    private fun sendSocketEvent(eventName: String, data: JSONObject) {
        val packet = JSONArray().apply {
            put(eventName)
            put(data)
        }
        // Emit as type 42 Socket.IO message event
        val frame = "42$packet"
        webSocket?.send(frame)
    }
}
