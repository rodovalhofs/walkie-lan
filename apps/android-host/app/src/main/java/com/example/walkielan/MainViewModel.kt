package com.example.walkielan

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.walkielan.data.ActiveSession
import com.example.walkielan.data.ChannelSelectMessage
import com.example.walkielan.data.ClientType
import com.example.walkielan.data.CreateRoomRequest
import com.example.walkielan.data.ErrorMessage
import com.example.walkielan.data.EventMessage
import com.example.walkielan.data.JoinRoomRequest
import com.example.walkielan.data.PeerJoinedMessage
import com.example.walkielan.data.PeerLeftMessage
import com.example.walkielan.data.RoomClosedMessage
import com.example.walkielan.data.RoomSnapshot
import com.example.walkielan.data.RoomSnapshotMessage
import com.example.walkielan.data.SignalEnvelope
import com.example.walkielan.data.SignalMessage
import com.example.walkielan.data.SocketMessage
import com.example.walkielan.data.SyncSnapshotMessage
import com.example.walkielan.data.TalkDeniedMessage
import com.example.walkielan.data.TalkGrantedMessage
import com.example.walkielan.data.TalkReleaseRequestMessage
import com.example.walkielan.data.TalkReleasedMessage
import com.example.walkielan.data.TalkRequestMessage
import com.example.walkielan.network.SignalingApi
import com.example.walkielan.network.SignalingSocket
import com.example.walkielan.rtc.WebRtcController
import com.example.walkielan.service.WalkieSessionService
import com.example.walkielan.ui.MainUiState
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val api = SignalingApi()
    private val socket = SignalingSocket()
    private val rtc = WebRtcController(application.applicationContext)
    private val deviceId = UUID.randomUUID().toString()

    private val _uiState = MutableStateFlow(MainUiState(serverBaseUrl = defaultServerBaseUrl()))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun updateServerBaseUrl(value: String) = _uiState.update { it.copy(serverBaseUrl = value, errorMessage = null) }
    fun updateNickname(value: String) = _uiState.update { it.copy(nickname = value, errorMessage = null) }
    fun updateRoomName(value: String) = _uiState.update { it.copy(roomName = value, errorMessage = null) }
    fun updateChannelsInput(value: String) = _uiState.update { it.copy(channelsInput = value, errorMessage = null) }
    fun updateRoomCodeInput(value: String) = _uiState.update { it.copy(roomCodeInput = value.uppercase(), errorMessage = null) }

    fun createRoom() {
        val state = _uiState.value
        viewModelScope.launch {
            setBusy(true)
            runCatching {
                val baseUrl = requireValidServerBaseUrl(state.serverBaseUrl)
                api.createRoom(
                    baseUrl = baseUrl,
                    payload = CreateRoomRequest(
                        roomName = state.roomName,
                        channelNames = parseChannelNames(state.channelsInput),
                        hostDeviceId = deviceId,
                        hostNickname = state.nickname,
                    ),
                )
            }.onSuccess { reservation ->
                connectSession(
                    ActiveSession(
                        roomId = reservation.roomId,
                        roomCode = reservation.roomCode,
                        peerId = reservation.hostPeerId,
                        token = reservation.hostSessionToken,
                        wsUrl = reservation.wsUrl,
                        isHost = true,
                    ),
                    initialSnapshot = null,
                )
            }.onFailure(::handleFailure)
            setBusy(false)
        }
    }

    fun joinRoom() {
        val state = _uiState.value
        viewModelScope.launch {
            setBusy(true)
            runCatching {
                val baseUrl = requireValidServerBaseUrl(state.serverBaseUrl)
                api.joinRoom(
                    baseUrl = baseUrl,
                    payload = JoinRoomRequest(
                        roomCode = state.roomCodeInput,
                        nickname = state.nickname,
                        clientType = ClientType.ANDROID_NATIVE,
                        deviceId = deviceId,
                    ),
                )
            }.onSuccess { join ->
                connectSession(
                    ActiveSession(
                        roomId = join.roomId,
                        roomCode = state.roomCodeInput,
                        peerId = join.peerId,
                        token = join.peerToken,
                        wsUrl = join.wsUrl,
                        isHost = false,
                    ),
                    initialSnapshot = join.snapshot,
                )
            }.onFailure(::handleFailure)
            setBusy(false)
        }
    }

    fun enableMicrophone() {
        runCatching {
            rtc.ensureMicrophone()
            _uiState.update {
                it.copy(
                    micReady = true,
                    notice = "Microfone pronto para PTT.",
                    errorMessage = null,
                )
            }
        }.onFailure(::handleFailure)
    }

    fun selectChannel(channelId: String) {
        val session = _uiState.value.session ?: return
        sendMessage(
            ChannelSelectMessage(
                peerId = session.peerId,
                channelId = channelId,
            ),
        )
    }

    fun startTalking() {
        val state = _uiState.value
        val session = state.session ?: return
        val snapshot = state.snapshot ?: return
        if (!state.micReady) {
            _uiState.update { it.copy(notice = "Habilite o microfone antes de falar.") }
            return
        }
        val self = snapshot.members.firstOrNull { it.peerId == session.peerId } ?: return
        sendMessage(TalkRequestMessage(peerId = session.peerId, channelId = self.selectedChannelId))
        _uiState.update { it.copy(notice = "Solicitando vez no canal...") }
    }

    fun stopTalking() {
        val state = _uiState.value
        val session = state.session ?: return
        val snapshot = state.snapshot ?: return
        if (!state.isTalking) return
        val self = snapshot.members.firstOrNull { it.peerId == session.peerId } ?: return
        sendMessage(TalkReleaseRequestMessage(peerId = session.peerId, channelId = self.selectedChannelId))
        _uiState.update { it.copy(notice = "Liberando o canal...") }
    }

    fun disconnect() {
        socket.disconnect()
        rtc.resetSession()
        WalkieSessionService.stop(getApplication())
        _uiState.value = MainUiState(
            serverBaseUrl = _uiState.value.serverBaseUrl,
            nickname = _uiState.value.nickname,
            roomName = _uiState.value.roomName,
            channelsInput = _uiState.value.channelsInput,
            roomCodeInput = _uiState.value.roomCodeInput,
        )
    }

    private fun connectSession(
        session: ActiveSession,
        initialSnapshot: RoomSnapshot?,
    ) {
        socket.disconnect()
        rtc.resetSession()
        _uiState.update {
            it.copy(
                session = session,
                snapshot = initialSnapshot,
                connected = false,
                micReady = false,
                isTalking = false,
                notice = "Sessao conectando...",
                errorMessage = null,
            )
        }
        initialSnapshot?.let(::ensurePeerConnections)

        socket.connect(
            session = session,
            onMessage = { message ->
                viewModelScope.launch {
                    runCatching {
                        maybeHandleHostAutomation(message)
                        handleSocketMessage(message)
                    }.onFailure(::handleFailure)
                }
            },
            onClosed = {
                _uiState.update { it.copy(connected = false, notice = "Conexao encerrada.") }
                WalkieSessionService.stop(getApplication())
            },
            onFailure = ::handleFailure,
        )
    }

    private fun handleSocketMessage(message: SocketMessage) {
        _uiState.update { it.copy(connected = true) }
        when (message) {
            is RoomSnapshotMessage -> {
                _uiState.update { it.copy(snapshot = message.snapshot, notice = "Sala sincronizada.") }
                ensurePeerConnections(message.snapshot)
                WalkieSessionService.start(getApplication(), message.snapshot.roomName)
            }

            is PeerJoinedMessage -> {
                _uiState.update { state ->
                    val snapshot = state.snapshot ?: return@update state
                    val members = snapshot.members.filterNot { it.peerId == message.peer.peerId } + message.peer
                    state.copy(snapshot = snapshot.copy(members = members))
                }
                _uiState.value.snapshot?.let(::ensurePeerConnections)
            }

            is PeerLeftMessage -> {
                rtc.destroyPeer(message.peerId)
                _uiState.update { state ->
                    val snapshot = state.snapshot ?: return@update state
                    state.copy(snapshot = snapshot.copy(members = snapshot.members.filterNot { it.peerId == message.peerId }))
                }
            }

            is EventMessage -> {
                _uiState.update { state ->
                    val snapshot = state.snapshot ?: return@update state
                    state.copy(snapshot = snapshot.copy(eventLog = (snapshot.eventLog + message.entry).takeLast(200)))
                }
            }

            is ChannelSelectMessage -> {
                _uiState.update { state ->
                    val snapshot = state.snapshot ?: return@update state
                    val members = snapshot.members.map { member ->
                        if (member.peerId == message.peerId) member.copy(selectedChannelId = message.channelId) else member
                    }
                    state.copy(snapshot = snapshot.copy(members = members))
                }
                refreshOutboundRouting()
            }

            is TalkGrantedMessage -> {
                _uiState.update { state ->
                    val snapshot = state.snapshot ?: return@update state
                    val channels = snapshot.channels.map { channel ->
                        if (channel.channelId == message.channelId) {
                            channel.copy(
                                activeSpeakerPeerId = message.holderPeerId,
                                queueVersion = message.queueVersion,
                            )
                        } else {
                            channel
                        }
                    }
                    state.copy(
                        snapshot = snapshot.copy(
                            channels = channels,
                            activeSpeakerByChannel = snapshot.activeSpeakerByChannel + (message.channelId to message.holderPeerId),
                        ),
                        isTalking = message.holderPeerId == state.session?.peerId,
                        notice = if (message.holderPeerId == state.session?.peerId) {
                            "Transmitindo para os pares do mesmo canal."
                        } else {
                            state.notice
                        },
                    )
                }
                if (message.holderPeerId == _uiState.value.session?.peerId) {
                    rtc.setMicrophoneEnabled(true)
                    refreshOutboundRouting()
                }
            }

            is TalkDeniedMessage -> {
                if (message.peerId == _uiState.value.session?.peerId) {
                    _uiState.update { it.copy(notice = message.reason) }
                }
            }

            is TalkReleasedMessage -> {
                _uiState.update { state ->
                    val snapshot = state.snapshot ?: return@update state
                    val channels = snapshot.channels.map { channel ->
                        if (channel.channelId == message.channelId) {
                            channel.copy(activeSpeakerPeerId = null, queueVersion = message.queueVersion)
                        } else {
                            channel
                        }
                    }
                    state.copy(
                        snapshot = snapshot.copy(
                            channels = channels,
                            activeSpeakerByChannel = snapshot.activeSpeakerByChannel + (message.channelId to null),
                        ),
                        isTalking = if (message.peerId == state.session?.peerId) false else state.isTalking,
                        notice = if (message.peerId == state.session?.peerId) "Canal liberado." else state.notice,
                    )
                }
                if (message.peerId == _uiState.value.session?.peerId) {
                    rtc.setMicrophoneEnabled(false)
                    refreshOutboundRouting()
                }
            }

            is SignalMessage -> {
                ensureSignalPeer(message.fromPeerId)
                rtc.handleSignal(message.fromPeerId, message.signal) { answerEnvelope ->
                    sendSignal(answerEnvelope)
                }
            }

            is RoomClosedMessage -> {
                _uiState.update { it.copy(connected = false, notice = message.reason) }
                WalkieSessionService.stop(getApplication())
            }

            is ErrorMessage -> {
                _uiState.update { it.copy(errorMessage = message.message, notice = message.message) }
            }

            is SyncSnapshotMessage -> {
                _uiState.update { it.copy(snapshot = message.snapshot) }
                ensurePeerConnections(message.snapshot)
            }

            is TalkRequestMessage,
            is TalkReleaseRequestMessage -> Unit
            else -> Unit
        }
    }

    private fun maybeHandleHostAutomation(message: SocketMessage) {
        val state = _uiState.value
        val session = state.session ?: return
        val snapshot = state.snapshot ?: return
        if (!session.isHost) return

        when (message) {
            is TalkRequestMessage -> {
                val currentHolder = snapshot.activeSpeakerByChannel[message.channelId]
                val version = snapshot.channels.firstOrNull { it.channelId == message.channelId }?.queueVersion?.plus(1) ?: 1
                if (currentHolder == null) {
                    sendMessage(
                        TalkGrantedMessage(
                            channelId = message.channelId,
                            holderPeerId = message.peerId,
                            grantedAt = java.time.Instant.now().toString(),
                            queueVersion = version,
                        ),
                    )
                } else {
                    sendMessage(
                        TalkDeniedMessage(
                            channelId = message.channelId,
                            peerId = message.peerId,
                            reason = "Outro participante esta falando neste canal.",
                        ),
                    )
                }
            }

            is TalkReleaseRequestMessage -> {
                val version = snapshot.channels.firstOrNull { it.channelId == message.channelId }?.queueVersion?.plus(1) ?: 1
                sendMessage(
                    TalkReleasedMessage(
                        channelId = message.channelId,
                        peerId = message.peerId,
                        queueVersion = version,
                    ),
                )
            }

            else -> Unit
        }
    }

    private fun ensurePeerConnections(snapshot: RoomSnapshot) {
        val session = _uiState.value.session ?: return
        snapshot.members.filter { it.peerId != session.peerId && it.isConnected }.forEach { member ->
            rtc.ensurePeerConnection(
                roomId = snapshot.roomId,
                localPeerId = session.peerId,
                remotePeerId = member.peerId,
                onSignal = ::sendSignal,
                onRemoteAudio = { /* Audio playout is handled by WebRTC on Android. */ },
            )
        }
    }

    private fun ensureSignalPeer(remotePeerId: String) {
        val session = _uiState.value.session ?: return
        val snapshot = _uiState.value.snapshot ?: return
        rtc.ensurePeerConnection(
            roomId = snapshot.roomId,
            localPeerId = session.peerId,
            remotePeerId = remotePeerId,
            onSignal = ::sendSignal,
            onRemoteAudio = { },
        )
    }

    private fun refreshOutboundRouting() {
        val state = _uiState.value
        val session = state.session ?: return
        val snapshot = state.snapshot ?: return
        val self = snapshot.members.firstOrNull { it.peerId == session.peerId } ?: return
        val eligiblePeerIds = if (state.isTalking) {
            snapshot.members
                .filter {
                    it.peerId != session.peerId &&
                        it.isConnected &&
                        it.selectedChannelId == self.selectedChannelId
                }
                .map { it.peerId }
        } else {
            emptyList()
        }
        rtc.updateOutboundAudio(
            roomId = snapshot.roomId,
            localPeerId = session.peerId,
            eligiblePeerIds = eligiblePeerIds,
            onSignal = ::sendSignal,
        )
    }

    private fun sendSignal(envelope: SignalEnvelope) {
        val session = _uiState.value.session ?: return
        sendMessage(
            SignalMessage(
                fromPeerId = session.peerId,
                toPeerId = envelope.targetPeerId,
                signal = envelope,
            ),
        )
    }

    private fun sendMessage(message: SocketMessage) {
        socket.send(message)
    }

    private fun parseChannelNames(input: String): List<String> {
        return input.split(",")
            .map(String::trim)
            .filter(String::isNotBlank)
            .ifEmpty { listOf("Geral", "Operacao", "Suporte") }
            .take(8)
    }

    private fun requireValidServerBaseUrl(input: String): String {
        val normalized = input.trim().trimEnd('/')
        require(normalized.isNotEmpty()) {
            "Preencha o endereco do servidor. No celular, use o IP do seu computador, por exemplo: http://192.168.0.15:8787"
        }
        require(normalized.startsWith("http://") || normalized.startsWith("https://")) {
            "O endereco do servidor precisa comecar com http:// ou https://"
        }
        return normalized
    }

    private fun defaultServerBaseUrl(): String {
        return if (isProbablyEmulator()) {
            "http://10.0.2.2:8787"
        } else {
            ""
        }
    }

    private fun isProbablyEmulator(): Boolean {
        return Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
            Build.MODEL.contains("Emulator", ignoreCase = true) ||
            Build.MODEL.contains("Android SDK built for", ignoreCase = true) ||
            Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
            Build.PRODUCT.contains("sdk", ignoreCase = true) ||
            Build.HARDWARE.contains("ranchu", ignoreCase = true)
    }

    private fun setBusy(value: Boolean) {
        _uiState.update { it.copy(busy = value) }
    }

    private fun handleFailure(throwable: Throwable) {
        _uiState.update {
            it.copy(
                busy = false,
                connected = false,
                errorMessage = throwable.message ?: "Falha inesperada.",
                notice = throwable.message ?: "Falha inesperada.",
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        socket.disconnect()
        rtc.release()
        WalkieSessionService.stop(getApplication())
    }
}
