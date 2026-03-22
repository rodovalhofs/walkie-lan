package com.example.walkielan.network

import com.example.walkielan.data.ActiveSession
import com.example.walkielan.data.HelloMessage
import com.example.walkielan.data.SocketMessage
import com.example.walkielan.data.WalkieJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class SignalingSocket(
    private val client: OkHttpClient = OkHttpClient(),
) {
    private var webSocket: WebSocket? = null

    fun connect(
        session: ActiveSession,
        onMessage: (SocketMessage) -> Unit,
        onClosed: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        val request = Request.Builder()
            .url(session.wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                send(
                    HelloMessage(
                        roomId = session.roomId,
                        peerId = session.peerId,
                        token = session.token,
                    ),
                )
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    WalkieJson.instance.decodeFromString<SocketMessage>(text)
                }.onSuccess(onMessage)
                    .onFailure(onFailure)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onClosed()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onFailure(t)
            }
        })
    }

    fun send(message: SocketMessage) {
        webSocket?.send(WalkieJson.instance.encodeToString<SocketMessage>(message))
    }

    fun disconnect() {
        webSocket?.close(1000, "user_closed")
        webSocket = null
    }
}
