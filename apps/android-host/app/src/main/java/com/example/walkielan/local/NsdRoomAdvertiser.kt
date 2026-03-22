package com.example.walkielan.local

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.example.walkielan.localserver.PROTOCOL_VERSION

class NsdRoomAdvertiser(
    context: Context,
) {
    private val nsdManager = context.getSystemService(NsdManager::class.java)
    private var registrationListener: NsdManager.RegistrationListener? = null

    fun publish(roomCode: String, roomName: String, port: Int) {
        stop()

        val info = NsdServiceInfo().apply {
            serviceName = "WalkieLAN-$roomCode"
            serviceType = SERVICE_TYPE
            setPort(port)
            setAttribute("roomCode", roomCode)
            setAttribute("roomName", roomName)
            setAttribute("protocolVersion", PROTOCOL_VERSION)
            setAttribute("transportMode", "local_lan")
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        }

        nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun stop() {
        registrationListener?.let {
            runCatching {
                nsdManager.unregisterService(it)
            }
        }
        registrationListener = null
    }

    companion object {
        const val SERVICE_TYPE = "_walkielan._tcp."
    }
}
