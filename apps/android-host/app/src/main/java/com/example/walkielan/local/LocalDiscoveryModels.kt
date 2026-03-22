package com.example.walkielan.local

data class DiscoveredRoom(
    val serviceName: String,
    val roomCode: String,
    val roomName: String,
    val hostAddress: String,
    val port: Int,
    val protocolVersion: String,
) {
    val baseUrl: String
        get() = "http://$hostAddress:$port"
}

data class LocalRuntimeInfo(
    val internalBaseUrl: String,
    val advertisedBaseUrl: String,
    val port: Int,
    val hostAddress: String,
)
