package com.example.walkielan.localserver

import android.content.Context
import com.example.walkielan.data.HostEndpoint
import com.example.walkielan.local.LanAddressResolver
import com.example.walkielan.local.LocalRuntimeInfo
import com.example.walkielan.local.NsdRoomAdvertiser

class LocalHostRuntime(
    context: Context,
) {
    private val advertiser = NsdRoomAdvertiser(context)
    private var runtimeInfo: LocalRuntimeInfo? = null
    private var server: LocalHostServer? = null

    fun ensureStarted(port: Int = DEFAULT_LOCAL_PORT): LocalRuntimeInfo {
        runtimeInfo?.let { return it }

        val hostAddress = LanAddressResolver.resolveIpv4Address()
            ?: throw IllegalStateException("Nao encontramos um IP local valido na rede Wi-Fi.")
        val advertisedBaseUrl = "http://$hostAddress:$port"
        val info = LocalRuntimeInfo(
            internalBaseUrl = "http://127.0.0.1:$port",
            advertisedBaseUrl = advertisedBaseUrl,
            port = port,
            hostAddress = hostAddress,
        )
        val localServer = LocalHostServer(port) {
            HostEndpoint(
                hostAddress = info.hostAddress,
                port = info.port,
                baseUrl = info.advertisedBaseUrl,
                consoleUrl = "${info.advertisedBaseUrl}/console",
            )
        }
        localServer.startServer()
        server = localServer
        runtimeInfo = info
        return info
    }

    fun publishRoom(roomCode: String, roomName: String) {
        val info = runtimeInfo ?: return
        advertiser.publish(roomCode = roomCode, roomName = roomName, port = info.port)
    }

    fun stop() {
        advertiser.stop()
        server?.stop()
        server = null
        runtimeInfo = null
    }
}
