package com.example.walkielan.localserver

import com.example.walkielan.data.ChannelSelectMessage
import com.example.walkielan.data.ChannelState
import com.example.walkielan.data.ClientRole
import com.example.walkielan.data.ClientType
import com.example.walkielan.data.CreateRoomRequest
import com.example.walkielan.data.EventEntry
import com.example.walkielan.data.EventMessage
import com.example.walkielan.data.HostEndpoint
import com.example.walkielan.data.HostStatus
import com.example.walkielan.data.JoinRoomRequest
import com.example.walkielan.data.JoinRoomResponse
import com.example.walkielan.data.PeerJoinedMessage
import com.example.walkielan.data.PeerLeftMessage
import com.example.walkielan.data.PeerCapabilities
import com.example.walkielan.data.PeerState
import com.example.walkielan.data.RoomCapabilities
import com.example.walkielan.data.RoomCodeReservation
import com.example.walkielan.data.RoomClosedMessage
import com.example.walkielan.data.RoomSnapshot
import com.example.walkielan.data.RoomSnapshotMessage
import com.example.walkielan.data.SignalMessage
import com.example.walkielan.data.SocketMessage
import com.example.walkielan.data.SyncSnapshotMessage
import com.example.walkielan.data.TalkDeniedMessage
import com.example.walkielan.data.TalkGrantedMessage
import com.example.walkielan.data.TalkReleaseRequestMessage
import com.example.walkielan.data.TalkReleasedMessage
import com.example.walkielan.data.TalkRequestMessage
import com.example.walkielan.data.TransportMode
import com.example.walkielan.data.WalkieJson
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.encodeToString

const val DEFAULT_LOCAL_PORT = 8787
const val PROTOCOL_VERSION = "2.0.0"

private const val EVENT_LOG_LIMIT = 200
private const val ROOM_CAPACITY = 10

interface LocalSocketLike {
    fun canSend(): Boolean
    fun send(text: String)
}

private data class InternalPeer(
    val peerId: String,
    val nickname: String,
    val clientType: ClientType,
    val deviceId: String,
    var selectedChannelId: String,
    val isHost: Boolean,
    val role: ClientRole,
    val capabilities: PeerCapabilities,
    var isConnected: Boolean,
    val joinedAt: String,
    var lastSeenAt: String,
    val token: String,
    var socket: LocalSocketLike? = null,
)

private data class LocalTalkLock(
    val channelId: String,
    var holderPeerId: String? = null,
    var grantedAt: String? = null,
    var queueVersion: Int = 0,
)

private data class LocalRoomRecord(
    val roomId: String,
    val roomCode: String,
    val roomName: String,
    val expiresAt: String,
    val hostSessionToken: String,
    val hostPeerId: String,
    val transportMode: TransportMode,
    val hostEndpoint: HostEndpoint,
    val roomCapabilities: RoomCapabilities,
    var hostStatus: HostStatus,
    val channels: MutableList<ChannelState>,
    val activeSpeakerByChannel: MutableMap<String, String?>,
    val talkLocks: MutableMap<String, LocalTalkLock>,
    val peers: LinkedHashMap<String, InternalPeer>,
    val eventLog: MutableList<EventEntry>,
)

data class AuthenticatedPeer(
    val roomId: String,
    val peerId: String,
)

class LocalRoomRegistry {
    private val roomsById = linkedMapOf<String, LocalRoomRecord>()
    private val roomIdByCode = linkedMapOf<String, String>()

    fun createRoom(
        input: CreateRoomRequest,
        internalWsUrl: String,
        hostEndpoint: HostEndpoint,
    ): RoomCodeReservation {
        val roomId = UUID.randomUUID().toString()
        val hostPeerId = UUID.randomUUID().toString()
        val hostToken = makeToken()
        val roomCode = makeRoomCode()
        val expiresAt = Instant.now().plusSeconds(12 * 60 * 60L).toString()
        val channels = input.channelNames.ifEmpty { listOf("Geral", "Operacao", "Suporte") }
            .take(8)
            .mapIndexed { index, name ->
                ChannelState(channelId = "channel-${index + 1}", name = name)
            }
            .toMutableList()
        val activeSpeakerByChannel: MutableMap<String, String?> =
            channels.associate { channel -> channel.channelId to null as String? }.toMutableMap()
        val talkLocks = channels.associate { it.channelId to LocalTalkLock(channelId = it.channelId) }.toMutableMap()
        val roomCapabilities = RoomCapabilities(
            allowsConsoleClients = true,
            allowsExperimentalWebVoice = true,
            localFirst = true,
        )
        val hostPeer = InternalPeer(
            peerId = hostPeerId,
            nickname = input.hostNickname,
            clientType = ClientType.ANDROID_NATIVE,
            deviceId = input.hostDeviceId,
            selectedChannelId = channels.firstOrNull()?.channelId ?: "channel-1",
            isHost = true,
            role = ClientRole.FULL_VOICE,
            capabilities = capabilitiesForRole(ClientRole.FULL_VOICE, TransportMode.LOCAL_LAN),
            isConnected = false,
            joinedAt = Instant.now().toString(),
            lastSeenAt = Instant.now().toString(),
            token = hostToken,
        )
        val room = LocalRoomRecord(
            roomId = roomId,
            roomCode = roomCode,
            roomName = input.roomName,
            expiresAt = expiresAt,
            hostSessionToken = hostToken,
            hostPeerId = hostPeerId,
            transportMode = TransportMode.LOCAL_LAN,
            hostEndpoint = hostEndpoint,
            roomCapabilities = roomCapabilities,
            hostStatus = HostStatus.OFFLINE,
            channels = channels,
            activeSpeakerByChannel = activeSpeakerByChannel,
            talkLocks = talkLocks,
            peers = linkedMapOf(hostPeerId to hostPeer),
            eventLog = mutableListOf(),
        )
        appendEvent(room, hostPeerId, hostPeer.selectedChannelId, "room_created", "${input.hostNickname} criou a sala local")
        roomsById[roomId] = room
        roomIdByCode[roomCode] = roomId
        return RoomCodeReservation(
            roomId = roomId,
            roomCode = roomCode,
            expiresAt = expiresAt,
            hostSessionToken = hostToken,
            hostPeerId = hostPeerId,
            wsUrl = internalWsUrl,
            transportMode = TransportMode.LOCAL_LAN,
            hostEndpoint = hostEndpoint,
            roomCapabilities = roomCapabilities,
            pairingUrl = "${hostEndpoint.baseUrl}/console?roomCode=$roomCode",
        )
    }

    fun joinRoom(
        input: JoinRoomRequest,
        wsUrl: String,
        hostEndpoint: HostEndpoint,
    ): JoinRoomResponse {
        val room = getRoomByCodeOrThrow(input.roomCode)
        ensureRoomOpen(room)
        if (room.peers.size >= ROOM_CAPACITY) {
            throw IllegalStateException("A sala atingiu o limite de participantes.")
        }

        val peerId = UUID.randomUUID().toString()
        val peerToken = makeToken()
        val role = input.requestedRole ?: defaultRoleForClientType(input.clientType)
        val peer = InternalPeer(
            peerId = peerId,
            nickname = input.nickname,
            clientType = input.clientType,
            deviceId = input.deviceId,
            selectedChannelId = room.channels.firstOrNull()?.channelId ?: "channel-1",
            isHost = false,
            role = role,
            capabilities = capabilitiesForRole(role, TransportMode.LOCAL_LAN),
            isConnected = false,
            joinedAt = Instant.now().toString(),
            lastSeenAt = Instant.now().toString(),
            token = peerToken,
        )
        room.peers[peerId] = peer
        return JoinRoomResponse(
            roomId = room.roomId,
            peerId = peerId,
            peerToken = peerToken,
            wsUrl = wsUrl,
            clientRole = role,
            transportMode = TransportMode.LOCAL_LAN,
            hostEndpoint = hostEndpoint,
            snapshot = toSnapshot(room),
        )
    }

    fun authenticate(roomId: String, peerId: String, token: String): AuthenticatedPeer {
        val room = roomsById[roomId] ?: throw IllegalStateException("Sala inexistente.")
        val peer = room.peers[peerId] ?: throw IllegalStateException("Participante inexistente.")
        if (peer.token != token) {
            throw IllegalStateException("Credenciais invalidas.")
        }
        return AuthenticatedPeer(roomId = roomId, peerId = peerId)
    }

    fun attachSocket(roomId: String, peerId: String, socket: LocalSocketLike): RoomSnapshot {
        val room = roomsById[roomId] ?: throw IllegalStateException("Sala inexistente.")
        val peer = room.peers[peerId] ?: throw IllegalStateException("Participante inexistente.")
        peer.socket = socket
        peer.isConnected = true
        peer.lastSeenAt = Instant.now().toString()

        if (peer.isHost) {
            room.hostStatus = HostStatus.ONLINE
            appendEvent(room, peerId, peer.selectedChannelId, "host_online", "${peer.nickname} colocou o host online")
        } else {
            appendEvent(room, peerId, peer.selectedChannelId, "join", "${peer.nickname} entrou na sala")
            broadcast(room, PeerJoinedMessage(peer = publicPeer(peer)))
            room.eventLog.lastOrNull()?.let { event -> broadcast(room, EventMessage(entry = event)) }
        }

        val snapshot = toSnapshot(room)
        emitToPeer(peer, RoomSnapshotMessage(snapshot))
        broadcast(room, SyncSnapshotMessage(snapshot))
        return snapshot
    }

    fun detachSocket(roomId: String, peerId: String) {
        val room = roomsById[roomId] ?: return
        val peer = room.peers[peerId] ?: return
        peer.socket = null
        peer.isConnected = false
        peer.lastSeenAt = Instant.now().toString()

        if (peer.isHost) {
            room.hostStatus = HostStatus.OFFLINE
            appendEvent(room, peerId, peer.selectedChannelId, "host_offline", "${peer.nickname} ficou offline")
            room.eventLog.lastOrNull()?.let { event -> broadcast(room, EventMessage(entry = event)) }
            broadcast(room, SyncSnapshotMessage(toSnapshot(room)))
            return
        }

        room.peers.remove(peerId)
        appendEvent(room, peerId, peer.selectedChannelId, "leave", "${peer.nickname} saiu da sala")
        broadcast(room, PeerLeftMessage(peerId))
        room.eventLog.lastOrNull()?.let { event -> broadcast(room, EventMessage(entry = event)) }
    }

    fun handleMessage(senderPeerId: String, message: SocketMessage) {
        val room = getRoomForPeerOrThrow(senderPeerId)
        val sender = room.peers[senderPeerId] ?: throw IllegalStateException("Participante desconectado.")

        when (message) {
            is ChannelSelectMessage -> {
                sender.selectedChannelId = message.channelId
                sender.lastSeenAt = Instant.now().toString()
                appendEvent(room, senderPeerId, message.channelId, "channel_change", "${sender.nickname} foi para ${channelLabel(room, message.channelId)}")
                broadcast(room, message)
                room.eventLog.lastOrNull()?.let { event -> broadcast(room, EventMessage(entry = event)) }
            }

            is TalkRequestMessage -> emitToPeerId(room, room.hostPeerId, message)
            is TalkReleaseRequestMessage -> emitToPeerId(room, room.hostPeerId, message)

            is TalkGrantedMessage -> {
                assertHost(room, senderPeerId)
                val lock = room.talkLocks[message.channelId] ?: throw IllegalStateException("Canal invalido.")
                lock.holderPeerId = message.holderPeerId
                lock.grantedAt = message.grantedAt
                lock.queueVersion = message.queueVersion
                updateChannel(room, message.channelId, message.holderPeerId, message.queueVersion)
                appendEvent(room, message.holderPeerId, message.channelId, "speaker_start", "${peerLabel(room, message.holderPeerId)} esta falando em ${channelLabel(room, message.channelId)}")
                broadcast(room, message)
                room.eventLog.lastOrNull()?.let { event -> broadcast(room, EventMessage(entry = event)) }
            }

            is TalkDeniedMessage -> {
                assertHost(room, senderPeerId)
                broadcast(room, message)
            }

            is TalkReleasedMessage -> {
                assertHost(room, senderPeerId)
                val lock = room.talkLocks[message.channelId] ?: throw IllegalStateException("Canal invalido.")
                lock.holderPeerId = null
                lock.grantedAt = null
                lock.queueVersion = message.queueVersion
                updateChannel(room, message.channelId, null, message.queueVersion)
                appendEvent(room, message.peerId, message.channelId, "speaker_end", "${peerLabel(room, message.peerId)} liberou o canal ${channelLabel(room, message.channelId)}")
                broadcast(room, message)
                room.eventLog.lastOrNull()?.let { event -> broadcast(room, EventMessage(entry = event)) }
            }

            is SignalMessage -> emitToPeerId(room, message.toPeerId, message)

            is SyncSnapshotMessage -> {
                assertHost(room, senderPeerId)
                room.channels.clear()
                room.channels.addAll(message.snapshot.channels)
                room.activeSpeakerByChannel.clear()
                room.activeSpeakerByChannel.putAll(message.snapshot.activeSpeakerByChannel)
                room.hostStatus = message.snapshot.hostStatus
                room.eventLog.clear()
                room.eventLog.addAll(message.snapshot.eventLog.takeLast(EVENT_LOG_LIMIT))
                broadcast(room, RoomSnapshotMessage(toSnapshot(room)))
            }

            is RoomClosedMessage -> {
                assertHost(room, senderPeerId)
                broadcast(room, message)
                roomsById.remove(room.roomId)
                roomIdByCode.remove(room.roomCode)
            }

            else -> Unit
        }
    }

    fun getSnapshotByCode(roomCode: String): RoomSnapshot {
        return toSnapshot(getRoomByCodeOrThrow(roomCode))
    }

    private fun updateChannel(room: LocalRoomRecord, channelId: String, activeSpeakerPeerId: String?, queueVersion: Int) {
        val index = room.channels.indexOfFirst { it.channelId == channelId }
        if (index >= 0) {
            room.channels[index] = room.channels[index].copy(
                activeSpeakerPeerId = activeSpeakerPeerId,
                queueVersion = queueVersion,
            )
        }
        room.activeSpeakerByChannel[channelId] = activeSpeakerPeerId
    }

    private fun toSnapshot(room: LocalRoomRecord): RoomSnapshot {
        return RoomSnapshot(
            roomId = room.roomId,
            roomName = room.roomName,
            roomCode = room.roomCode,
            channels = room.channels.toList(),
            members = room.peers.values.map(::publicPeer),
            activeSpeakerByChannel = room.activeSpeakerByChannel.toMap(),
            hostStatus = room.hostStatus,
            transportMode = room.transportMode,
            hostEndpoint = room.hostEndpoint,
            roomCapabilities = room.roomCapabilities,
            eventLog = room.eventLog.toList(),
            capacity = ROOM_CAPACITY,
        )
    }

    private fun publicPeer(peer: InternalPeer): PeerState {
        return PeerState(
            peerId = peer.peerId,
            nickname = peer.nickname,
            clientType = peer.clientType,
            deviceId = peer.deviceId,
            selectedChannelId = peer.selectedChannelId,
            isHost = peer.isHost,
            role = peer.role,
            capabilities = peer.capabilities,
            isConnected = peer.isConnected,
            joinedAt = peer.joinedAt,
            lastSeenAt = peer.lastSeenAt,
        )
    }

    private fun appendEvent(room: LocalRoomRecord, peerId: String?, channelId: String?, type: String, summary: String) {
        room.eventLog += EventEntry(
            eventId = UUID.randomUUID().toString(),
            roomId = room.roomId,
            channelId = channelId,
            peerId = peerId,
            type = type,
            occurredAt = Instant.now().toString(),
            summary = summary,
        )
        if (room.eventLog.size > EVENT_LOG_LIMIT) {
            room.eventLog.removeAt(0)
        }
    }

    private fun emitToPeer(peer: InternalPeer, message: SocketMessage) {
        val socket = peer.socket ?: return
        if (socket.canSend()) {
            socket.send(WalkieJson.instance.encodeToString(message))
        }
    }

    private fun emitToPeerId(room: LocalRoomRecord, peerId: String, message: SocketMessage) {
        room.peers[peerId]?.let { emitToPeer(it, message) }
    }

    private fun broadcast(room: LocalRoomRecord, message: SocketMessage) {
        room.peers.values.forEach { emitToPeer(it, message) }
    }

    private fun assertHost(room: LocalRoomRecord, senderPeerId: String) {
        if (room.hostPeerId != senderPeerId) {
            throw IllegalStateException("Apenas o host pode enviar essa mensagem.")
        }
    }

    private fun getRoomForPeerOrThrow(peerId: String): LocalRoomRecord {
        return roomsById.values.firstOrNull { it.peers.containsKey(peerId) }
            ?: throw IllegalStateException("Participante nao localizado.")
    }

    private fun getRoomByCodeOrThrow(roomCode: String): LocalRoomRecord {
        val roomId = roomIdByCode[roomCode.uppercase()] ?: throw IllegalStateException("Sala nao encontrada.")
        return roomsById[roomId] ?: throw IllegalStateException("Sala nao encontrada.")
    }

    private fun ensureRoomOpen(room: LocalRoomRecord) {
        if (Instant.parse(room.expiresAt).isBefore(Instant.now())) {
            roomsById.remove(room.roomId)
            roomIdByCode.remove(room.roomCode)
            throw IllegalStateException("Sala expirada.")
        }
    }

    private fun channelLabel(room: LocalRoomRecord, channelId: String): String {
        return room.channels.firstOrNull { it.channelId == channelId }?.name ?: "Canal"
    }

    private fun peerLabel(room: LocalRoomRecord, peerId: String): String {
        return room.peers[peerId]?.nickname ?: "Participante"
    }

    private fun makeRoomCode(): String {
        var code: String
        do {
            code = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
        } while (roomIdByCode.containsKey(code))
        return code
    }

    private fun makeToken(): String {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "")
    }

    private fun defaultRoleForClientType(clientType: ClientType): ClientRole {
        return when (clientType) {
            ClientType.IOS_WEB -> ClientRole.CONSOLE_ONLY
            ClientType.ANDROID_WEB_DEBUG -> ClientRole.EXPERIMENTAL_WEB_VOICE
            ClientType.ANDROID_NATIVE -> ClientRole.FULL_VOICE
        }
    }

    private fun capabilitiesForRole(role: ClientRole, transportMode: TransportMode): PeerCapabilities {
        return PeerCapabilities(
            canTransmitAudio = role != ClientRole.CONSOLE_ONLY,
            canReceiveAudio = true,
            supportsLocalJoin = transportMode == TransportMode.LOCAL_LAN,
            supportsAdvancedWebRtc = role != ClientRole.CONSOLE_ONLY,
        )
    }
}
