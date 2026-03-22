import type { SignalEnvelope } from "@walkie/protocol";
import type { Instance as PeerInstance, SignalData } from "simple-peer";
import Peer from "simple-peer/simplepeer.min.js";

type RemoteStreamHandler = (peerId: string, stream: MediaStream | null) => void;

function normalizeIceCandidatePayload(signal: unknown): Record<string, unknown> | null {
  if (typeof signal !== "object" || signal === null) {
    return null;
  }

  const candidateSignal = signal as Record<string, unknown>;
  const nestedCandidate = candidateSignal.candidate;

  if (typeof nestedCandidate === "object" && nestedCandidate !== null) {
    return nestedCandidate as Record<string, unknown>;
  }

  if (typeof nestedCandidate === "string" && nestedCandidate.length > 0) {
    return {
      candidate: nestedCandidate,
      sdpMid:
        typeof candidateSignal.sdpMid === "string" ? candidateSignal.sdpMid : null,
      sdpMLineIndex:
        typeof candidateSignal.sdpMLineIndex === "number" ? candidateSignal.sdpMLineIndex : 0,
    };
  }

  if (typeof candidateSignal.sdp === "string" && candidateSignal.sdp.length > 0) {
    return {
      candidate: candidateSignal.sdp,
      sdpMid:
        typeof candidateSignal.sdpMid === "string" ? candidateSignal.sdpMid : null,
      sdpMLineIndex:
        typeof candidateSignal.sdpMLineIndex === "number" ? candidateSignal.sdpMLineIndex : 0,
    };
  }

  return null;
}

function signalToEnvelope(
  roomId: string,
  peerId: string,
  targetPeerId: string,
  signal: SignalData,
): SignalEnvelope | null {
  const iceCandidate = normalizeIceCandidatePayload(signal);
  if (iceCandidate) {
    return {
      roomId,
      peerId,
      targetPeerId,
      type: "ice-candidate",
      iceCandidate: JSON.stringify(iceCandidate),
    };
  }

  if (typeof signal !== "object" || !signal || !("type" in signal)) {
    return null;
  }
  if (signal.type !== "offer" && signal.type !== "answer") {
    return null;
  }

  return {
    roomId,
    peerId,
    targetPeerId,
    type: signal.type === "answer" ? "answer" : "offer",
    sdp: "sdp" in signal ? signal.sdp : undefined,
  };
}

function envelopeToSignal(signal: SignalEnvelope): SignalData | null {
  if (signal.type === "ice-candidate") {
    const candidate = normalizeIceCandidatePayload(JSON.parse(signal.iceCandidate ?? "{}"));
    if (!candidate) {
      return null;
    }
    return {
      type: "candidate",
      candidate,
    } as unknown as SignalData;
  }
  return {
    type: signal.type,
    sdp: signal.sdp,
  } as SignalData;
}

export class MeshManager {
  private readonly peers = new Map<string, PeerInstance>();
  private readonly attachedPeers = new Set<string>();
  private localStream: MediaStream | null = null;

  constructor(
    private readonly roomId: string,
    private readonly peerId: string,
    private readonly onSignal: (signal: SignalEnvelope) => void,
    private readonly onRemoteStream: RemoteStreamHandler,
  ) {}

  async ensureMicrophone() {
    if (this.localStream) {
      return this.localStream;
    }

    this.localStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true,
      },
      video: false,
    });
    for (const track of this.localStream.getAudioTracks()) {
      track.enabled = false;
    }
    return this.localStream;
  }

  ensurePeer(remotePeerId: string, initiator: boolean) {
    if (this.peers.has(remotePeerId)) {
      return;
    }

    const peer = new Peer({
      initiator,
      trickle: true,
    });
    peer.on("signal", (signal: SignalData) => {
      const envelope = signalToEnvelope(this.roomId, this.peerId, remotePeerId, signal);
      if (envelope) {
        this.onSignal(envelope);
      }
    });
    peer.on("stream", (stream: MediaStream) => {
      this.onRemoteStream(remotePeerId, stream);
    });
    peer.on("close", () => {
      this.attachedPeers.delete(remotePeerId);
      this.peers.delete(remotePeerId);
      this.onRemoteStream(remotePeerId, null);
    });
    peer.on("error", () => {
      this.onRemoteStream(remotePeerId, null);
    });
    this.peers.set(remotePeerId, peer);
  }

  async updateOutboundAudio(eligiblePeerIds: string[]) {
    const stream = await this.ensureMicrophone();
    const track = stream.getAudioTracks()[0];
    if (!track) {
      return;
    }

    for (const [remotePeerId, peer] of this.peers.entries()) {
      const shouldAttach = eligiblePeerIds.includes(remotePeerId);
      const alreadyAttached = this.attachedPeers.has(remotePeerId);
      if (shouldAttach && !alreadyAttached) {
        peer.addTrack(track, stream);
        this.attachedPeers.add(remotePeerId);
      }
      if (!shouldAttach && alreadyAttached) {
        peer.removeTrack(track, stream);
        this.attachedPeers.delete(remotePeerId);
      }
    }
  }

  setMicrophoneEnabled(enabled: boolean) {
    for (const track of this.localStream?.getAudioTracks() ?? []) {
      track.enabled = enabled;
    }
  }

  handleSignal(fromPeerId: string, signal: SignalEnvelope) {
    const peerSignal = envelopeToSignal(signal);
    if (peerSignal) {
      this.peers.get(fromPeerId)?.signal(peerSignal);
    }
  }

  destroyPeer(peerId: string) {
    this.peers.get(peerId)?.destroy();
    this.attachedPeers.delete(peerId);
    this.peers.delete(peerId);
    this.onRemoteStream(peerId, null);
  }

  destroyAll() {
    for (const peer of this.peers.values()) {
      peer.destroy();
    }
    this.peers.clear();
    this.attachedPeers.clear();
    for (const track of this.localStream?.getTracks() ?? []) {
      track.stop();
    }
    this.localStream = null;
  }
}
