package com.example.walkielan.rtc

import android.content.Context
import android.util.Log
import com.example.walkielan.data.SignalEnvelope
import org.json.JSONObject
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpSender
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.audio.JavaAudioDeviceModule

class WebRtcController(
    context: Context,
) {
    companion object {
        private const val TAG = "WebRtcController"
    }

    private val factory: PeerConnectionFactory
    private val audioDeviceModule = JavaAudioDeviceModule.builder(context).createAudioDeviceModule()
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    private val peerConnections = mutableMapOf<String, PeerConnection>()
    private val outboundSenders = mutableMapOf<String, RtpSender>()
    private val activeRecipients = mutableSetOf<String>()

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions(),
        )
        factory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .createPeerConnectionFactory()
    }

    fun ensureMicrophone() {
        if (localAudioTrack != null) {
            return
        }

        audioSource = factory.createAudioSource(MediaConstraints())
        localAudioTrack = factory.createAudioTrack("local-audio", audioSource)
        localAudioTrack?.setEnabled(false)
    }

    fun hasMicrophoneReady(): Boolean = localAudioTrack != null

    fun ensurePeerConnection(
        roomId: String,
        localPeerId: String,
        remotePeerId: String,
        onSignal: (SignalEnvelope) -> Unit,
        onRemoteAudio: (String) -> Unit,
    ) {
        if (peerConnections.containsKey(remotePeerId)) {
            return
        }

        val rtcConfig = PeerConnection.RTCConfiguration(emptyList())
        val connection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                onSignal(
                    SignalEnvelope(
                        roomId = roomId,
                        peerId = localPeerId,
                        targetPeerId = remotePeerId,
                        type = "ice-candidate",
                        iceCandidate = encodeCandidate(candidate),
                    ),
                )
            }

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) = Unit
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) = Unit
            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) = Unit
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
            override fun onAddStream(stream: MediaStream?) = Unit
            override fun onRemoveStream(stream: MediaStream?) = Unit
            override fun onDataChannel(dataChannel: org.webrtc.DataChannel?) = Unit
            override fun onRenegotiationNeeded() = Unit
            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                receiver?.track()?.setEnabled(true)
                onRemoteAudio(remotePeerId)
            }
        }) ?: return

        peerConnections[remotePeerId] = connection
    }

    fun updateOutboundAudio(
        roomId: String,
        localPeerId: String,
        eligiblePeerIds: List<String>,
        onSignal: (SignalEnvelope) -> Unit,
    ) {
        ensureMicrophone()
        val track = localAudioTrack ?: return
        val nextRecipients = eligiblePeerIds.toSet()

        for ((peerId, connection) in peerConnections) {
            val isActive = activeRecipients.contains(peerId)
            val shouldBeActive = nextRecipients.contains(peerId)

            if (shouldBeActive && !isActive) {
                val sender = connection.addTrack(track, listOf("walkie-stream"))
                if (sender != null) {
                    outboundSenders[peerId] = sender
                    activeRecipients.add(peerId)
                    renegotiate(connection, roomId, localPeerId, peerId, onSignal)
                }
            }

            if (!shouldBeActive && isActive) {
                outboundSenders.remove(peerId)?.let(connection::removeTrack)
                activeRecipients.remove(peerId)
                renegotiate(connection, roomId, localPeerId, peerId, onSignal)
            }
        }
    }

    fun setMicrophoneEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun handleSignal(
        fromPeerId: String,
        envelope: SignalEnvelope,
        onAnswer: (SignalEnvelope) -> Unit,
    ) {
        val connection = peerConnections[fromPeerId]
            ?: return

        when (envelope.type) {
            "offer" -> {
                connection.setRemoteDescription(
                    SimpleSdpObserver(),
                    SessionDescription(SessionDescription.Type.OFFER, envelope.sdp.orEmpty()),
                )
                connection.createAnswer(object : SimpleSdpObserver() {
                    override fun onCreateSuccess(description: SessionDescription?) {
                        if (description == null) return
                        connection.setLocalDescription(SimpleSdpObserver(), description)
                        onAnswer(
                            SignalEnvelope(
                                roomId = envelope.roomId,
                                peerId = envelope.targetPeerId,
                                targetPeerId = envelope.peerId,
                                type = "answer",
                                sdp = description.description,
                            ),
                        )
                    }
                }, MediaConstraints())
            }

            "answer" -> {
                connection.setRemoteDescription(
                    SimpleSdpObserver(),
                    SessionDescription(SessionDescription.Type.ANSWER, envelope.sdp.orEmpty()),
                )
            }

            "ice-candidate" -> {
                val candidate = decodeCandidate(envelope.iceCandidate.orEmpty())
                if (candidate != null) {
                    connection.addIceCandidate(candidate)
                } else {
                    Log.w(TAG, "ICE candidate ignorado por payload invalido.")
                }
            }
        }
    }

    fun destroyPeer(peerId: String) {
        outboundSenders.remove(peerId)
        activeRecipients.remove(peerId)
        peerConnections.remove(peerId)?.dispose()
    }

    fun resetSession() {
        peerConnections.keys.toList().forEach(::destroyPeer)
        localAudioTrack?.dispose()
        localAudioTrack = null
        audioSource?.dispose()
        audioSource = null
    }

    fun release() {
        resetSession()
        factory.dispose()
        audioDeviceModule.release()
    }

    private fun renegotiate(
        connection: PeerConnection,
        roomId: String,
        localPeerId: String,
        targetPeerId: String,
        onSignal: (SignalEnvelope) -> Unit,
    ) {
        connection.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(description: SessionDescription?) {
                if (description == null) return
                connection.setLocalDescription(SimpleSdpObserver(), description)
                onSignal(
                    SignalEnvelope(
                        roomId = roomId,
                        peerId = localPeerId,
                        targetPeerId = targetPeerId,
                        type = "offer",
                        sdp = description.description,
                    ),
                )
            }
        }, MediaConstraints())
    }

    private fun encodeCandidate(candidate: IceCandidate): String {
        return JSONObject()
            .put("candidate", candidate.sdp)
            .put("sdpMid", candidate.sdpMid)
            .put("sdpMLineIndex", candidate.sdpMLineIndex)
            .put("sdp", candidate.sdp)
            .toString()
    }

    private fun decodeCandidate(raw: String): IceCandidate? {
        val json = JSONObject(raw)
        val candidateJson = when (val nested = json.opt("candidate")) {
            is JSONObject -> nested
            else -> json
        }
        val candidateLine = candidateJson.optString("candidate")
            .ifBlank { candidateJson.optString("sdp") }
            .ifBlank { return null }
        val sdpMid = when {
            candidateJson.has("sdpMid") && !candidateJson.isNull("sdpMid") -> candidateJson.getString("sdpMid")
            candidateJson.has("mid") && !candidateJson.isNull("mid") -> candidateJson.getString("mid")
            else -> null
        }
        val sdpMLineIndex = when {
            candidateJson.has("sdpMLineIndex") -> candidateJson.optInt("sdpMLineIndex", 0)
            candidateJson.has("mLineIndex") -> candidateJson.optInt("mLineIndex", 0)
            else -> 0
        }
        return IceCandidate(
            sdpMid,
            sdpMLineIndex,
            candidateLine,
        )
    }
}

open class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(description: SessionDescription?) = Unit
    override fun onSetSuccess() = Unit
    override fun onCreateFailure(error: String?) = Unit
    override fun onSetFailure(error: String?) = Unit
}
