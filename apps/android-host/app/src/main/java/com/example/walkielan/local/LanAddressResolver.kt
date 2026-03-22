package com.example.walkielan.local

import java.net.Inet4Address
import java.net.NetworkInterface

object LanAddressResolver {
    fun resolveIpv4Address(): String? {
        return NetworkInterface.getNetworkInterfaces()
            ?.toList()
            ?.asSequence()
            ?.filter { it.isUp && !it.isLoopback && !it.name.startsWith("rmnet") }
            ?.flatMap { it.inetAddresses.toList().asSequence() }
            ?.filterIsInstance<Inet4Address>()
            ?.firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
            ?.hostAddress
    }
}
