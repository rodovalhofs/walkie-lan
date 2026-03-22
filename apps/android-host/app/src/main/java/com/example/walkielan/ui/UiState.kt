package com.example.walkielan.ui

import com.example.walkielan.data.ActiveSession
import com.example.walkielan.data.RoomSnapshot

data class MainUiState(
    val serverBaseUrl: String = "",
    val nickname: String = "Operador Android",
    val roomName: String = "Equipe LAN",
    val channelsInput: String = "Geral, Operacao, Suporte",
    val roomCodeInput: String = "",
    val snapshot: RoomSnapshot? = null,
    val session: ActiveSession? = null,
    val connected: Boolean = false,
    val micReady: Boolean = false,
    val isTalking: Boolean = false,
    val notice: String = "Crie ou entre em uma sala para iniciar o walkie-talkie.",
    val errorMessage: String? = null,
    val busy: Boolean = false,
)
