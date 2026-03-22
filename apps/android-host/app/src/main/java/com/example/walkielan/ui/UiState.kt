package com.example.walkielan.ui

import com.example.walkielan.audio.AudioRoute
import com.example.walkielan.data.ActiveSession
import com.example.walkielan.data.RoomSnapshot
import com.example.walkielan.local.DiscoveredRoom

enum class SetupMode {
    SIMPLE,
    ADVANCED,
}

data class MainUiState(
    val serverBaseUrl: String = "",
    val nickname: String = "Operador Android",
    val roomName: String = "Equipe LAN",
    val channelsInput: String = "Geral, Operacao, Suporte",
    val roomCodeInput: String = "",
    val setupMode: SetupMode = SetupMode.SIMPLE,
    val discoveredRooms: List<DiscoveredRoom> = emptyList(),
    val discoveryActive: Boolean = false,
    val localHostBaseUrl: String? = null,
    val localConsoleUrl: String? = null,
    val pairingUrl: String? = null,
    val snapshot: RoomSnapshot? = null,
    val session: ActiveSession? = null,
    val connected: Boolean = false,
    val micReady: Boolean = false,
    val isTalking: Boolean = false,
    val selectedAudioRoute: AudioRoute = AudioRoute.SPEAKER,
    val availableAudioRoutes: List<AudioRoute> = emptyList(),
    val audioRouteSupported: Boolean = false,
    val audioRouteNotice: String? = null,
    val isAudioRoutePickerVisible: Boolean = false,
    val notice: String = "Crie ou entre em uma sala para iniciar o walkie-talkie.",
    val errorMessage: String? = null,
    val busy: Boolean = false,
)
