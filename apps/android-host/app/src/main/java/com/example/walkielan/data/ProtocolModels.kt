package com.example.walkielan.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
enum class ClientType {
    @SerialName("android_native")
    ANDROID_NATIVE,

    @SerialName("ios_web")
    IOS_WEB,

    @SerialName("android_web_debug")
    ANDROID_WEB_DEBUG,
}

@Serializable
enum class HostStatus {
    @SerialName("online")
    ONLINE,

    @SerialName("offline")
    OFFLINE,

    @SerialName("closed")
    CLOSED,
}

@Serializable
data class CreateRoomRequest(
    val roomName: String,
    val channelNames: List<String>,
    val hostDeviceId: String,
    val hostNickname: String,
)

@Serializable
data class RoomCodeReservation(
    val roomId: String,
    val roomCode: String,
    val expiresAt: String,
    val hostSessionToken: String,
    val hostPeerId: String,
    val wsUrl: String,
)

@Serializable
data class JoinRoomRequest(
    val roomCode: String,
    val nickname: String,
    val clientType: ClientType,
    val deviceId: String,
)

@Serializable
data class PeerState(
    val peerId: String,
    val nickname: String,
    val clientType: ClientType,
    val deviceId: String,
    val selectedChannelId: String,
    val isHost: Boolean,
    val isConnected: Boolean,
    val joinedAt: String,
    val lastSeenAt: String,
)

@Serializable
data class ChannelState(
    val channelId: String,
    val name: String,
    val activeSpeakerPeerId: String? = null,
    val queueVersion: Int = 0,
)

@Serializable
data class EventEntry(
    val eventId: String,
    val roomId: String,
    val channelId: String? = null,
    val peerId: String? = null,
    val type: String,
    val occurredAt: String,
    val summary: String,
)

@Serializable
data class RoomSnapshot(
    val roomId: String,
    val roomName: String,
    val roomCode: String,
    val channels: List<ChannelState>,
    val members: List<PeerState>,
    val activeSpeakerByChannel: Map<String, String?>,
    val hostStatus: HostStatus,
    val eventLog: List<EventEntry>,
    val capacity: Int,
)

@Serializable
data class JoinRoomResponse(
    val roomId: String,
    val peerId: String,
    val peerToken: String,
    val wsUrl: String,
    val snapshot: RoomSnapshot,
)

@Serializable
data class SignalEnvelope(
    val roomId: String,
    val peerId: String,
    val targetPeerId: String,
    val type: String,
    val sdp: String? = null,
    val iceCandidate: String? = null,
)

data class ActiveSession(
    val roomId: String,
    val roomCode: String,
    val peerId: String,
    val token: String,
    val wsUrl: String,
    val isHost: Boolean,
)

@JsonClassDiscriminator("kind")
@Serializable
sealed interface SocketMessage

@Serializable
@SerialName("hello")
data class HelloMessage(
    val roomId: String,
    val peerId: String,
    val token: String,
) : SocketMessage

@Serializable
@SerialName("room_snapshot")
data class RoomSnapshotMessage(val snapshot: RoomSnapshot) : SocketMessage

@Serializable
@SerialName("peer_joined")
data class PeerJoinedMessage(val peer: PeerState) : SocketMessage

@Serializable
@SerialName("peer_left")
data class PeerLeftMessage(val peerId: String) : SocketMessage

@Serializable
@SerialName("channel_select")
data class ChannelSelectMessage(
    val peerId: String,
    val channelId: String,
) : SocketMessage

@Serializable
@SerialName("talk_request")
data class TalkRequestMessage(
    val peerId: String,
    val channelId: String,
) : SocketMessage

@Serializable
@SerialName("talk_release_request")
data class TalkReleaseRequestMessage(
    val peerId: String,
    val channelId: String,
) : SocketMessage

@Serializable
@SerialName("talk_granted")
data class TalkGrantedMessage(
    val channelId: String,
    val holderPeerId: String,
    val grantedAt: String,
    val queueVersion: Int,
) : SocketMessage

@Serializable
@SerialName("talk_denied")
data class TalkDeniedMessage(
    val channelId: String,
    val peerId: String,
    val reason: String,
) : SocketMessage

@Serializable
@SerialName("talk_released")
data class TalkReleasedMessage(
    val channelId: String,
    val peerId: String,
    val queueVersion: Int,
) : SocketMessage

@Serializable
@SerialName("signal")
data class SignalMessage(
    val fromPeerId: String,
    val toPeerId: String,
    val signal: SignalEnvelope,
) : SocketMessage

@Serializable
@SerialName("event")
data class EventMessage(val entry: EventEntry) : SocketMessage

@Serializable
@SerialName("room_closed")
data class RoomClosedMessage(val reason: String) : SocketMessage

@Serializable
@SerialName("sync_snapshot")
data class SyncSnapshotMessage(val snapshot: RoomSnapshot) : SocketMessage

@Serializable
@SerialName("error")
data class ErrorMessage(val message: String) : SocketMessage

@OptIn(ExperimentalSerializationApi::class)
object WalkieJson {
    val instance = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "kind"
        encodeDefaults = true
        explicitNulls = false
    }
}
