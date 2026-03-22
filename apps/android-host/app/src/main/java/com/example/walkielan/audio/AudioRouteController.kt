package com.example.walkielan.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper

enum class AudioRoute {
    SPEAKER,
    EARPIECE,
    WIRED_HEADSET,
    BLUETOOTH,
}

data class AudioRouteSnapshot(
    val availableRoutes: List<AudioRoute>,
    val selectedRoute: AudioRoute,
    val supported: Boolean,
    val notice: String? = null,
)

class AudioRouteController(
    context: Context,
    private val onChanged: (AudioRouteSnapshot) -> Unit,
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val callbackHandler = Handler(Looper.getMainLooper())
    private var sessionActive = false
    private var selectedRoute = AudioRoute.SPEAKER
    private var callbackRegistered = false

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            handleDeviceTopologyChanged()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            handleDeviceTopologyChanged()
        }
    }

    fun startSession() {
        if (!sessionActive) {
            sessionActive = true
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            registerCallback()
        }
        selectRoute(AudioRoute.SPEAKER)
    }

    fun stopSession() {
        if (!sessionActive) {
            emitSnapshot(availableRoutes = emptyList(), supported = false, notice = null)
            return
        }

        sessionActive = false
        unregisterCallback()
        clearAudioRouting()
        selectedRoute = AudioRoute.SPEAKER
        emitSnapshot(availableRoutes = emptyList(), supported = false, notice = null)
    }

    fun refreshRoutes() {
        emitSnapshot(availableRoutes = availableRoutes())
    }

    fun selectRoute(route: AudioRoute) {
        if (!sessionActive) {
            emitSnapshot(
                availableRoutes = emptyList(),
                supported = false,
                notice = "Saida de audio disponivel apenas durante a sessao ativa.",
            )
            return
        }

        val routes = availableRoutes()
        if (!routes.contains(route)) {
            val fallback = if (routes.contains(AudioRoute.SPEAKER)) {
                AudioRoute.SPEAKER
            } else {
                routes.firstOrNull() ?: AudioRoute.SPEAKER
            }
            applyRoute(fallback)
            emitSnapshot(
                availableRoutes = routes,
                currentSelectedRoute = fallback,
                supported = routes.isNotEmpty(),
                notice = "A rota escolhida nao esta mais disponivel. Voltamos para ${fallback.label}.",
            )
            return
        }

        applyRoute(route)
        emitSnapshot(
            availableRoutes = routes,
            currentSelectedRoute = route,
            supported = routes.isNotEmpty(),
            notice = "Saida de audio: ${route.label}.",
        )
    }

    fun release() {
        stopSession()
    }

    private fun handleDeviceTopologyChanged() {
        val routes = availableRoutes()
        if (!routes.contains(selectedRoute)) {
            val fallback = if (routes.contains(AudioRoute.SPEAKER)) {
                AudioRoute.SPEAKER
            } else {
                routes.firstOrNull() ?: AudioRoute.SPEAKER
            }
            applyRoute(fallback)
            emitSnapshot(
                availableRoutes = routes,
                currentSelectedRoute = fallback,
                supported = routes.isNotEmpty(),
                notice = "Dispositivo removido. Voltamos para ${fallback.label}.",
            )
            return
        }

        emitSnapshot(availableRoutes = routes)
    }

    private fun applyRoute(route: AudioRoute) {
        selectedRoute = route
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            applyCommunicationDevice(route)
        } else {
            applyLegacyRoute(route)
        }
    }

    private fun applyCommunicationDevice(route: AudioRoute) {
        val devices = communicationDevices()
        val device = devices.firstOrNull { matchesRoute(it, route) }
        if (device != null) {
            audioManager.clearCommunicationDevice()
            audioManager.setCommunicationDevice(device)
            audioManager.isSpeakerphoneOn = route == AudioRoute.SPEAKER
            if (route == AudioRoute.BLUETOOTH) {
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
            } else {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
            }
            return
        }

        applyLegacyRoute(route)
    }

    private fun applyLegacyRoute(route: AudioRoute) {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        when (route) {
            AudioRoute.SPEAKER -> {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                audioManager.isSpeakerphoneOn = true
            }

            AudioRoute.EARPIECE,
            AudioRoute.WIRED_HEADSET -> {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                audioManager.isSpeakerphoneOn = false
            }

            AudioRoute.BLUETOOTH -> {
                audioManager.isSpeakerphoneOn = false
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
            }
        }
    }

    private fun clearAudioRouting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        }
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        audioManager.isSpeakerphoneOn = false
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    private fun emitSnapshot(
        availableRoutes: List<AudioRoute> = availableRoutes(),
        currentSelectedRoute: AudioRoute = selectedRoute,
        supported: Boolean = sessionActive && availableRoutes.isNotEmpty(),
        notice: String? = null,
    ) {
        onChanged(
            AudioRouteSnapshot(
                availableRoutes = availableRoutes,
                selectedRoute = currentSelectedRoute,
                supported = supported,
                notice = notice,
            ),
        )
    }

    private fun availableRoutes(): List<AudioRoute> {
        val devices = communicationDevices()
        val routes = buildList {
            if (devices.any { matchesRoute(it, AudioRoute.SPEAKER) }) {
                add(AudioRoute.SPEAKER)
            }
            if (devices.any { matchesRoute(it, AudioRoute.EARPIECE) }) {
                add(AudioRoute.EARPIECE)
            }
            if (devices.any { matchesRoute(it, AudioRoute.WIRED_HEADSET) }) {
                add(AudioRoute.WIRED_HEADSET)
            }
            if (devices.any { matchesRoute(it, AudioRoute.BLUETOOTH) }) {
                add(AudioRoute.BLUETOOTH)
            }
        }

        return if (routes.isNotEmpty()) routes else listOf(AudioRoute.SPEAKER)
    }

    private fun communicationDevices(): List<AudioDeviceInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.availableCommunicationDevices
        } else {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        }
    }

    private fun matchesRoute(device: AudioDeviceInfo, route: AudioRoute): Boolean {
        return when (route) {
            AudioRoute.SPEAKER -> device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            AudioRoute.EARPIECE -> device.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            AudioRoute.WIRED_HEADSET -> {
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_USB_DEVICE
            }

            AudioRoute.BLUETOOTH -> {
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    device.type == AudioDeviceInfo.TYPE_BLE_HEADSET
            }
        }
    }

    private fun registerCallback() {
        if (callbackRegistered) {
            return
        }
        audioManager.registerAudioDeviceCallback(deviceCallback, callbackHandler)
        callbackRegistered = true
    }

    private fun unregisterCallback() {
        if (!callbackRegistered) {
            return
        }
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
        callbackRegistered = false
    }
}

val AudioRoute.label: String
    get() = when (this) {
        AudioRoute.SPEAKER -> "Alto-falante"
        AudioRoute.EARPIECE -> "Auricular"
        AudioRoute.WIRED_HEADSET -> "Fone com fio"
        AudioRoute.BLUETOOTH -> "Bluetooth"
    }
