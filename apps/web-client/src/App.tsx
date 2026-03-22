import type {
  CreateRoomRequest,
  JoinRoomResponse,
  PeerState,
  RoomCodeReservation,
  RoomSnapshot,
  SocketMessage,
} from "@walkie/protocol";
import { useEffect, useMemo, useRef, useState } from "react";
import { LandingPanel } from "./components/LandingPanel";
import { RoomPanel } from "./components/RoomPanel";
import { createRoom, joinRoom, normalizeBaseUrl } from "./lib/api";
import { MeshManager } from "./lib/meshManager";
import { SignalingClient } from "./lib/signalingClient";

type ActiveSession = {
  roomId: string;
  roomCode: string;
  peerId: string;
  token: string;
  wsUrl: string;
  isHostDebug: boolean;
};

const DEFAULT_SERVER_URL = getDefaultServerUrl();

function getDefaultServerUrl() {
  if (typeof window === "undefined") {
    return "http://localhost:8787";
  }
  const host = window.location.hostname || "localhost";
  const protocol = window.location.protocol === "https:" ? "https" : "http";
  return `${protocol}://${host}:8787`;
}

function getDeviceId() {
  const stored = localStorage.getItem("walkie-device-id");
  if (stored) {
    return stored;
  }
  const generated = createDeviceId();
  localStorage.setItem("walkie-device-id", generated);
  return generated;
}

function createDeviceId() {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }

  if (typeof crypto !== "undefined" && typeof crypto.getRandomValues === "function") {
    const bytes = crypto.getRandomValues(new Uint8Array(16));
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    const hex = [...bytes].map((value) => value.toString(16).padStart(2, "0"));
    return `${hex.slice(0, 4).join("")}-${hex.slice(4, 6).join("")}-${hex.slice(6, 8).join("")}-${hex.slice(8, 10).join("")}-${hex.slice(10, 16).join("")}`;
  }

  return `device-${Math.random().toString(36).slice(2)}-${Date.now().toString(36)}`;
}

function isInitiator(localPeerId: string, remotePeerId: string) {
  return localPeerId.localeCompare(remotePeerId) < 0;
}

export default function App() {
  const [serverUrl, setServerUrl] = useState(DEFAULT_SERVER_URL);
  const [nickname, setNickname] = useState("Operador");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [session, setSession] = useState<ActiveSession | null>(null);
  const [snapshot, setSnapshot] = useState<RoomSnapshot | null>(null);
  const [connected, setConnected] = useState(false);
  const [micReady, setMicReady] = useState(false);
  const [isTalking, setIsTalking] = useState(false);
  const [notice, setNotice] = useState("Segure para transmitir no canal ativo.");
  const [remoteStreams, setRemoteStreams] = useState<Map<string, MediaStream>>(new Map());

  const signalingRef = useRef<SignalingClient | null>(null);
  const meshRef = useRef<MeshManager | null>(null);
  const snapshotRef = useRef<RoomSnapshot | null>(null);

  useEffect(() => {
    snapshotRef.current = snapshot;
  }, [snapshot]);

  useEffect(() => {
    return () => {
      signalingRef.current?.disconnect();
      meshRef.current?.destroyAll();
    };
  }, []);

  const roomCode = useMemo(() => session?.roomCode ?? "", [session]);

  async function onCreateRoom(payload: { roomName: string; channelNames: string[] }) {
    setBusy(true);
    setError(null);
    try {
      const request: CreateRoomRequest = {
        roomName: payload.roomName,
        channelNames: payload.channelNames,
        hostDeviceId: getDeviceId(),
        hostNickname: nickname,
      };
      const reservation = await createRoom(serverUrl, request);
      await activateSessionFromReservation(reservation);
    } catch (caught) {
      setError(getErrorMessage(caught));
    } finally {
      setBusy(false);
    }
  }

  async function onJoinRoom(payload: { roomCode: string }) {
    setBusy(true);
    setError(null);
    try {
      const response = await joinRoom(serverUrl, {
        roomCode: payload.roomCode,
        nickname,
        clientType: "ios_web",
        deviceId: getDeviceId(),
      });
      await activateSessionFromJoin(response, payload.roomCode.toUpperCase());
    } catch (caught) {
      setError(getErrorMessage(caught));
    } finally {
      setBusy(false);
    }
  }

  async function activateSessionFromReservation(reservation: RoomCodeReservation) {
    await connectSession({
      roomId: reservation.roomId,
      roomCode: reservation.roomCode,
      peerId: reservation.hostPeerId,
      token: reservation.hostSessionToken,
      wsUrl: reservation.wsUrl,
      isHostDebug: true,
    });
  }

  async function activateSessionFromJoin(response: JoinRoomResponse, roomCodeValue: string) {
    setSnapshot(response.snapshot);
    await connectSession({
      roomId: response.roomId,
      roomCode: roomCodeValue,
      peerId: response.peerId,
      token: response.peerToken,
      wsUrl: response.wsUrl,
      isHostDebug: false,
    });
  }

  async function connectSession(nextSession: ActiveSession) {
    signalingRef.current?.disconnect();
    meshRef.current?.destroyAll();
    setRemoteStreams(new Map());
    setConnected(false);
    setMicReady(false);
    setIsTalking(false);
    setSession(nextSession);

    const signaling = new SignalingClient();
    const mesh = new MeshManager(
      nextSession.roomId,
      nextSession.peerId,
      (signal) => {
        signaling.send({
          kind: "signal",
          fromPeerId: nextSession.peerId,
          toPeerId: signal.targetPeerId,
          signal,
        });
      },
      (peerId, stream) => {
        setRemoteStreams((current) => {
          const clone = new Map(current);
          if (stream) {
            clone.set(peerId, stream);
          } else {
            clone.delete(peerId);
          }
          return clone;
        });
      },
    );

    signaling.connect({
      roomId: nextSession.roomId,
      peerId: nextSession.peerId,
      token: nextSession.token,
      wsUrl: nextSession.wsUrl,
      onMessage: (message) => {
        setConnected(true);
        handleSocketMessage(nextSession, signaling, mesh, message);
      },
      onClose: () => {
        setConnected(false);
        setNotice("Conexao encerrada.");
      },
      onError: (caught) => {
        setError(getErrorMessage(caught));
        setNotice(getErrorMessage(caught));
      },
    });

    signalingRef.current = signaling;
    meshRef.current = mesh;
    setNotice("Sessao conectando. Habilite o microfone antes de transmitir.");
  }

  function ensureMeshPeers(localSession: ActiveSession, roomSnapshot: RoomSnapshot) {
    const mesh = meshRef.current;
    if (!mesh) {
      return;
    }
    for (const member of roomSnapshot.members) {
      if (member.peerId === localSession.peerId || !member.isConnected) {
        continue;
      }
      mesh.ensurePeer(member.peerId, isInitiator(localSession.peerId, member.peerId));
    }
  }

  function patchMembers(mutator: (members: PeerState[]) => PeerState[]) {
    setSnapshot((current) => {
      if (!current) {
        return current;
      }
      return { ...current, members: mutator(current.members) };
    });
  }

  function patchSnapshot(mutator: (current: RoomSnapshot) => RoomSnapshot) {
    setSnapshot((current) => (current ? mutator(current) : current));
  }

  function handleSocketMessage(
    localSession: ActiveSession,
    signaling: SignalingClient,
    mesh: MeshManager,
    message: SocketMessage,
  ) {
    if (localSession.isHostDebug) {
      maybeHandleHostDebugAutomation(localSession, signaling, message);
    }

    switch (message.kind) {
      case "room_snapshot":
        setSnapshot(message.snapshot);
        ensureMeshPeers(localSession, message.snapshot);
        break;
      case "peer_joined":
        patchMembers((members) => {
          const filtered = members.filter((member) => member.peerId !== message.peer.peerId);
          return [...filtered, message.peer];
        });
        mesh.ensurePeer(message.peer.peerId, isInitiator(localSession.peerId, message.peer.peerId));
        break;
      case "peer_left":
        patchMembers((members) => members.filter((member) => member.peerId !== message.peerId));
        mesh.destroyPeer(message.peerId);
        break;
      case "channel_select":
        patchMembers((members) =>
          members.map((member) =>
            member.peerId === message.peerId ? { ...member, selectedChannelId: message.channelId } : member,
          ),
        );
        if (isTalking) {
          void refreshOutboundRouting(localSession.peerId);
        }
        break;
      case "talk_request":
      case "talk_release_request":
        break;
      case "talk_granted":
        patchSnapshot((current) => ({
          ...current,
          channels: current.channels.map((channel) =>
            channel.channelId === message.channelId
              ? {
                  ...channel,
                  activeSpeakerPeerId: message.holderPeerId,
                  queueVersion: message.queueVersion,
                }
              : channel,
          ),
          activeSpeakerByChannel: {
            ...current.activeSpeakerByChannel,
            [message.channelId]: message.holderPeerId,
          },
        }));
        if (message.holderPeerId === localSession.peerId) {
          setIsTalking(true);
          mesh.setMicrophoneEnabled(true);
          void refreshOutboundRouting(localSession.peerId);
          setNotice("Transmitindo para os pares do mesmo canal.");
        }
        break;
      case "talk_denied":
        if (message.peerId === localSession.peerId) {
          setNotice(message.reason);
        }
        break;
      case "talk_released":
        patchSnapshot((current) => ({
          ...current,
          channels: current.channels.map((channel) =>
            channel.channelId === message.channelId
              ? {
                  ...channel,
                  activeSpeakerPeerId: null,
                  queueVersion: message.queueVersion,
                }
              : channel,
          ),
          activeSpeakerByChannel: {
            ...current.activeSpeakerByChannel,
            [message.channelId]: null,
          },
        }));
        if (message.peerId === localSession.peerId) {
          setIsTalking(false);
          mesh.setMicrophoneEnabled(false);
          void mesh.updateOutboundAudio([]);
          setNotice("Canal liberado.");
        }
        break;
      case "signal":
        mesh.ensurePeer(message.fromPeerId, isInitiator(localSession.peerId, message.fromPeerId));
        mesh.handleSignal(message.fromPeerId, message.signal);
        break;
      case "event":
        patchSnapshot((current) => ({
          ...current,
          eventLog: [...current.eventLog, message.entry].slice(-200),
        }));
        break;
      case "room_closed":
        setNotice(message.reason);
        setConnected(false);
        break;
      case "error":
        setNotice(message.message);
        break;
      case "hello":
        break;
      case "sync_snapshot":
        setSnapshot(message.snapshot);
        break;
    }
  }

  function maybeHandleHostDebugAutomation(
    localSession: ActiveSession,
    signaling: SignalingClient,
    message: SocketMessage,
  ) {
    const current = snapshotRef.current;
    if (!current) {
      return;
    }

    if (message.kind === "talk_request") {
      const currentHolder = current.activeSpeakerByChannel[message.channelId];
      const version =
        (current.channels.find((channel) => channel.channelId === message.channelId)?.queueVersion ?? 0) + 1;
      if (!currentHolder) {
        signaling.send({
          kind: "talk_granted",
          channelId: message.channelId,
          holderPeerId: message.peerId,
          grantedAt: new Date().toISOString(),
          queueVersion: version,
        });
      } else {
        signaling.send({
          kind: "talk_denied",
          channelId: message.channelId,
          peerId: message.peerId,
          reason: "Outro participante esta falando neste canal.",
        });
      }
    }

    if (message.kind === "talk_release_request") {
      const version =
        (current.channels.find((channel) => channel.channelId === message.channelId)?.queueVersion ?? 0) + 1;
      signaling.send({
        kind: "talk_released",
        channelId: message.channelId,
        peerId: message.peerId,
        queueVersion: version,
      });
      if (message.peerId === localSession.peerId) {
        setIsTalking(false);
      }
    }
  }

  async function onEnableMic() {
    try {
      await meshRef.current?.ensureMicrophone();
      setMicReady(true);
      setNotice("Microfone pronto para PTT.");
    } catch (caught) {
      setError(getErrorMessage(caught));
    }
  }

  async function refreshOutboundRouting(localPeerId: string) {
    const roomSnapshot = snapshotRef.current;
    const mesh = meshRef.current;
    if (!roomSnapshot || !mesh) {
      return;
    }
    const self = roomSnapshot.members.find((member) => member.peerId === localPeerId);
    if (!self) {
      return;
    }
    const eligiblePeerIds = roomSnapshot.members
      .filter(
        (member) =>
          member.peerId !== localPeerId &&
          member.isConnected &&
          member.selectedChannelId === self.selectedChannelId,
      )
      .map((member) => member.peerId);
    await mesh.updateOutboundAudio(eligiblePeerIds);
  }

  function onSelectChannel(channelId: string) {
    if (!session) {
      return;
    }
    signalingRef.current?.send({
      kind: "channel_select",
      peerId: session.peerId,
      channelId,
    });
  }

  function onPressToTalkStart() {
    const currentSession = session;
    const currentSnapshot = snapshotRef.current;
    if (!currentSession || !currentSnapshot || !micReady) {
      setNotice("Habilite o microfone antes de falar.");
      return;
    }
    const self = currentSnapshot.members.find((member) => member.peerId === currentSession.peerId);
    if (!self) {
      return;
    }
    signalingRef.current?.send({
      kind: "talk_request",
      peerId: currentSession.peerId,
      channelId: self.selectedChannelId,
    });
    setNotice("Solicitando vez no canal...");
  }

  function onPressToTalkEnd() {
    const currentSession = session;
    const currentSnapshot = snapshotRef.current;
    if (!currentSession || !currentSnapshot || !isTalking) {
      return;
    }
    const self = currentSnapshot.members.find((member) => member.peerId === currentSession.peerId);
    if (!self) {
      return;
    }
    signalingRef.current?.send({
      kind: "talk_release_request",
      peerId: currentSession.peerId,
      channelId: self.selectedChannelId,
    });
    setNotice("Liberando o canal...");
  }

  if (!session || !snapshot) {
    return (
      <main className="page-shell">
        <LandingPanel
          serverUrl={serverUrl}
          setServerUrl={(value) => setServerUrl(normalizeBaseUrl(value))}
          nickname={nickname}
          setNickname={setNickname}
          onCreateRoom={onCreateRoom}
          onJoinRoom={onJoinRoom}
          busy={busy}
          error={error}
        />
      </main>
    );
  }

  return (
    <main className="page-shell">
      <RoomPanel
        snapshot={snapshot}
        selfPeerId={session.peerId}
        roomCode={roomCode}
        connected={connected}
        micReady={micReady}
        isTalking={isTalking}
        onEnableMic={onEnableMic}
        onSelectChannel={onSelectChannel}
        onPressToTalkStart={onPressToTalkStart}
        onPressToTalkEnd={onPressToTalkEnd}
        remoteStreams={remoteStreams}
        notice={notice}
      />
    </main>
  );
}

function getErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }
  return "Falha inesperada.";
}
