import type {
  ClientRole,
  CreateRoomRequest,
  JoinRoomResponse,
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
  clientRole: ClientRole;
  isHostDebug: boolean;
};

const DEFAULT_SERVER_URL = getDefaultServerUrl();
const DEFAULT_AUDIO_OUTPUT_LABEL = "Padrao do sistema";

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

function canSelectAudioOutputInBrowser() {
  if (typeof window === "undefined" || !window.isSecureContext || typeof document === "undefined") {
    return false;
  }

  const mediaDevices = navigator.mediaDevices;
  const audio = document.createElement("audio");
  return typeof mediaDevices?.selectAudioOutput === "function" && typeof audio.setSinkId === "function";
}

function defaultAudioOutputMessage(canSelectAudioOutput: boolean) {
  return canSelectAudioOutput
    ? `Saida atual: ${DEFAULT_AUDIO_OUTPUT_LABEL}.`
    : "O navegador esta usando a saida padrao do aparelho.";
}

function getErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }
  return "Falha inesperada.";
}

export default function App() {
  const [serverUrl, setServerUrl] = useState(DEFAULT_SERVER_URL);
  const [nickname, setNickname] = useState("Operador");
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [session, setSession] = useState<ActiveSession | null>(null);
  const [snapshot, setSnapshot] = useState<RoomSnapshot | null>(null);
  const [connected, setConnected] = useState(false);
  const [micReady, setMicReady] = useState(false);
  const [isTalking, setIsTalking] = useState(false);
  const [notice, setNotice] = useState(
    "Use o APK Android para criar a sala. O navegador entra como console auxiliar ou laboratorio experimental.",
  );
  const [remoteStreams, setRemoteStreams] = useState<Map<string, MediaStream>>(new Map());
  const [canSelectAudioOutput, setCanSelectAudioOutput] = useState(() => canSelectAudioOutputInBrowser());
  const [selectedOutputDeviceId, setSelectedOutputDeviceId] = useState("");
  const [selectedOutputLabel, setSelectedOutputLabel] = useState(DEFAULT_AUDIO_OUTPUT_LABEL);
  const [audioOutputBusy, setAudioOutputBusy] = useState(false);
  const [audioOutputMessage, setAudioOutputMessage] = useState(() =>
    defaultAudioOutputMessage(canSelectAudioOutputInBrowser()),
  );

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

  useEffect(() => {
    const supported = canSelectAudioOutputInBrowser();
    setCanSelectAudioOutput(supported);
    setAudioOutputMessage(defaultAudioOutputMessage(supported));
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
      setNotice("Host web experimental conectado.");
    } catch (caught) {
      setError(getErrorMessage(caught));
    } finally {
      setBusy(false);
    }
  }

  async function onJoinRoom(payload: { roomCode: string; clientType: "ios_web" | "android_web_debug" }) {
    setBusy(true);
    setError(null);
    try {
      const response = await joinRoom(serverUrl, {
        roomCode: payload.roomCode,
        nickname,
        clientType: payload.clientType,
        deviceId: getDeviceId(),
        requestedRole: payload.clientType === "android_web_debug" ? "experimental_web_voice" : "console_only",
      });
      await activateSessionFromJoin(response, payload.roomCode.toUpperCase());
      setAdvancedOpen(payload.clientType === "android_web_debug");
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
      clientRole: "experimental_web_voice",
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
      clientRole: response.clientRole,
      isHostDebug: response.clientRole === "experimental_web_voice",
    });
  }

  async function connectSession(nextSession: ActiveSession) {
    signalingRef.current?.disconnect();
    meshRef.current?.destroyAll();
    setRemoteStreams(new Map());
    setConnected(false);
    setMicReady(false);
    setIsTalking(false);
    setSelectedOutputDeviceId("");
    setSelectedOutputLabel(DEFAULT_AUDIO_OUTPUT_LABEL);
    setAudioOutputMessage(defaultAudioOutputMessage(canSelectAudioOutputInBrowser()));
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
        teardownSession("Conexao encerrada.");
      },
      onError: (caught) => {
        setError(getErrorMessage(caught));
        setNotice(getErrorMessage(caught));
      },
    });

    signalingRef.current = signaling;
    meshRef.current = mesh;
    setNotice(
      nextSession.clientRole === "console_only"
        ? "Console auxiliar conectado. Este navegador acompanha a sala, sem PTT oficial."
        : "Sessao conectando. Habilite o microfone apenas se estiver no laboratorio experimental.",
    );
  }

  function teardownSession(message: string) {
    signalingRef.current?.disconnect();
    signalingRef.current = null;
    meshRef.current?.destroyAll();
    meshRef.current = null;
    setSession(null);
    setSnapshot(null);
    setConnected(false);
    setMicReady(false);
    setIsTalking(false);
    setRemoteStreams(new Map());
    setSelectedOutputDeviceId("");
    setSelectedOutputLabel(DEFAULT_AUDIO_OUTPUT_LABEL);
    setAudioOutputMessage(defaultAudioOutputMessage(canSelectAudioOutputInBrowser()));
    setNotice(message);
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

  function patchMembers(mutator: (members: RoomSnapshot["members"]) => RoomSnapshot["members"]) {
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
      maybeHandleHostDebugAutomation(signaling, message);
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
          setNotice("Transmitindo para os participantes do mesmo canal.");
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
        teardownSession(message.reason);
        break;
      case "error":
        setNotice(message.message);
        break;
      case "hello":
        break;
      case "sync_snapshot":
        setSnapshot(message.snapshot);
        ensureMeshPeers(localSession, message.snapshot);
        break;
    }
  }

  function maybeHandleHostDebugAutomation(signaling: SignalingClient, message: SocketMessage) {
    const current = snapshotRef.current;
    const currentSession = session;
    if (!current || !currentSession) {
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
      if (message.peerId === currentSession.peerId) {
        setIsTalking(false);
      }
    }
  }

  async function onEnableMic() {
    if (session?.clientRole === "console_only") {
      setNotice("Este navegador entrou apenas como console auxiliar.");
      return;
    }
    try {
      await meshRef.current?.ensureMicrophone();
      setMicReady(true);
      setNotice("Microfone pronto para o laboratorio web.");
    } catch (caught) {
      setError(getErrorMessage(caught));
    }
  }

  async function onSelectAudioOutput() {
    if (!canSelectAudioOutput || typeof navigator === "undefined" || typeof navigator.mediaDevices?.selectAudioOutput !== "function") {
      setAudioOutputMessage("O navegador esta usando a saida padrao do aparelho.");
      return;
    }

    setAudioOutputBusy(true);
    try {
      const device = await navigator.mediaDevices.selectAudioOutput();
      const label = device.label || "Dispositivo selecionado";
      setSelectedOutputDeviceId(device.deviceId);
      setSelectedOutputLabel(label);
      setAudioOutputMessage(`Saida atual: ${label}.`);
    } catch (caught) {
      if (caught instanceof DOMException && caught.name === "AbortError") {
        setAudioOutputMessage(`Saida atual: ${selectedOutputLabel}.`);
      } else {
        setAudioOutputMessage("Nao foi possivel trocar a saida de audio. O navegador segue na saida padrao.");
      }
    } finally {
      setAudioOutputBusy(false);
    }
  }

  function onAudioOutputSinkError() {
    setSelectedOutputDeviceId("");
    setSelectedOutputLabel(DEFAULT_AUDIO_OUTPUT_LABEL);
    setAudioOutputMessage("A saida escolhida nao esta disponivel. O navegador voltou para a saida padrao.");
  }

  async function refreshOutboundRouting(localPeerId: string) {
    const roomSnapshot = snapshotRef.current;
    const mesh = meshRef.current;
    if (!roomSnapshot || !mesh) {
      return;
    }
    const self = roomSnapshot.members.find((member) => member.peerId === localPeerId);
    if (!self || self.capabilities.canTransmitAudio === false) {
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
    if (currentSession.clientRole === "console_only") {
      setNotice("Este navegador entrou apenas como console auxiliar.");
      return;
    }
    const self = currentSnapshot.members.find((member) => member.peerId === currentSession.peerId);
    if (!self || self.capabilities.canTransmitAudio === false) {
      setNotice("Esta sessao web nao pode transmitir audio.");
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
          nickname={nickname}
          setNickname={setNickname}
          advancedOpen={advancedOpen}
          onToggleAdvanced={() => setAdvancedOpen((current) => !current)}
          serverUrl={serverUrl}
          setServerUrl={(value) => setServerUrl(normalizeBaseUrl(value))}
          onCreateRoom={onCreateRoom}
          onJoinRoom={onJoinRoom}
          busy={busy}
          error={error}
          statusMessage={notice}
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
        clientRole={session.clientRole}
        connected={connected}
        micReady={micReady}
        isTalking={isTalking}
        onEnableMic={onEnableMic}
        onSelectChannel={onSelectChannel}
        onPressToTalkStart={onPressToTalkStart}
        onPressToTalkEnd={onPressToTalkEnd}
        onSelectAudioOutput={onSelectAudioOutput}
        onAudioOutputSinkError={onAudioOutputSinkError}
        remoteStreams={remoteStreams}
        notice={notice}
        canSelectAudioOutput={canSelectAudioOutput}
        audioOutputBusy={audioOutputBusy}
        audioOutputLabel={selectedOutputLabel}
        audioOutputMessage={audioOutputMessage}
        selectedOutputDeviceId={selectedOutputDeviceId}
      />
    </main>
  );
}
