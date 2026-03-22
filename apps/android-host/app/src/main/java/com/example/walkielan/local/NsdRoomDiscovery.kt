package com.example.walkielan.local

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo

class NsdRoomDiscovery(
    context: Context,
    private val onRoomsChanged: (List<DiscoveredRoom>) -> Unit,
) {
    private val nsdManager = context.getSystemService(NsdManager::class.java)
    private val rooms = linkedMapOf<String, DiscoveredRoom>()
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun start() {
        if (discoveryListener != null) {
            onRoomsChanged(rooms.values.toList())
            return
        }

        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != NsdRoomAdvertiser.SERVICE_TYPE) {
                    return
                }

                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit

                    override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo) {
                        val hostAddress = resolvedServiceInfo.host?.hostAddress ?: return
                        val roomCode = resolvedServiceInfo.attributes["roomCode"]?.decodeToString()
                            ?: resolvedServiceInfo.serviceName.substringAfterLast('-')
                        val roomName = resolvedServiceInfo.attributes["roomName"]?.decodeToString()
                            ?: resolvedServiceInfo.serviceName
                        val protocolVersion = resolvedServiceInfo.attributes["protocolVersion"]?.decodeToString()
                            ?: "2.0.0"
                        rooms[resolvedServiceInfo.serviceName] = DiscoveredRoom(
                            serviceName = resolvedServiceInfo.serviceName,
                            roomCode = roomCode,
                            roomName = roomName,
                            hostAddress = hostAddress,
                            port = resolvedServiceInfo.port,
                            protocolVersion = protocolVersion,
                        )
                        onRoomsChanged(rooms.values.sortedBy { it.roomName.lowercase() })
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                rooms.remove(serviceInfo.serviceName)
                onRoomsChanged(rooms.values.sortedBy { it.roomName.lowercase() })
            }
        }

        discoveryListener = listener
        rooms.clear()
        onRoomsChanged(emptyList())
        nsdManager.discoverServices(NsdRoomAdvertiser.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    fun stop() {
        discoveryListener?.let {
            runCatching {
                nsdManager.stopServiceDiscovery(it)
            }
        }
        discoveryListener = null
        rooms.clear()
        onRoomsChanged(emptyList())
    }

    fun refresh() {
        stop()
        start()
    }
}
