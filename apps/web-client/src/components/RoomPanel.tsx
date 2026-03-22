import type { ClientRole, EventEntry, RoomSnapshot } from "@walkie/protocol";
import { useEffect, useRef } from "react";

interface RoomPanelProps {
  snapshot: RoomSnapshot;
  selfPeerId: string;
  roomCode: string;
  clientRole: ClientRole;
  connected: boolean;
  micReady: boolean;
  isTalking: boolean;
  onEnableMic: () => Promise<void>;
  onSelectChannel: (channelId: string) => void;
  onPressToTalkStart: () => void;
  onPressToTalkEnd: () => void;
  onSelectAudioOutput: () => Promise<void>;
  onAudioOutputSinkError: (message: string) => void;
  remoteStreams: Map<string, MediaStream>;
  notice: string;
  canSelectAudioOutput: boolean;
  audioOutputBusy: boolean;
  audioOutputLabel: string;
  audioOutputMessage: string;
  selectedOutputDeviceId: string;
}

async function applySinkId(audio: HTMLAudioElement, sinkId: string, onError: (message: string) => void) {
  if (!sinkId || typeof audio.setSinkId !== "function") {
    return;
  }

  try {
    await audio.setSinkId(sinkId);
  } catch (error) {
    onError(error instanceof Error ? error.message : "Nao foi possivel trocar a saida de audio.");
  }
}

function AudioDock(props: {
  remoteStreams: Map<string, MediaStream>;
  selectedOutputDeviceId: string;
  onSinkError: (message: string) => void;
}) {
  const containerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) {
      return;
    }
    const knownIds = new Set<string>();
    for (const [peerId, stream] of props.remoteStreams.entries()) {
      knownIds.add(peerId);
      let audio = container.querySelector<HTMLAudioElement>(`audio[data-peer="${peerId}"]`);
      if (!audio) {
        audio = document.createElement("audio");
        audio.autoplay = true;
        audio.setAttribute("playsinline", "true");
        audio.dataset.peer = peerId;
        container.appendChild(audio);
      }
      audio.srcObject = stream;
      void applySinkId(audio, props.selectedOutputDeviceId, props.onSinkError);
    }
    for (const audio of container.querySelectorAll<HTMLAudioElement>("audio")) {
      if (!knownIds.has(audio.dataset.peer ?? "")) {
        audio.remove();
      }
    }
    if (props.selectedOutputDeviceId) {
      for (const audio of container.querySelectorAll<HTMLAudioElement>("audio")) {
        void applySinkId(audio, props.selectedOutputDeviceId, props.onSinkError);
      }
    }
  }, [props.onSinkError, props.remoteStreams, props.selectedOutputDeviceId]);

  return <div ref={containerRef} className="audio-dock" aria-hidden="true" />;
}

function EventList(props: { events: EventEntry[] }) {
  return (
    <div className="event-list">
      {props.events
        .slice()
        .reverse()
        .slice(0, 10)
        .map((event) => (
          <article key={event.eventId} className="event-row">
            <strong>{event.summary}</strong>
            <span>{new Date(event.occurredAt).toLocaleTimeString("pt-BR")}</span>
          </article>
        ))}
    </div>
  );
}

export function RoomPanel(props: RoomPanelProps) {
  const self = props.snapshot.members.find((member) => member.peerId === props.selfPeerId);
  const selectedChannelId = self?.selectedChannelId ?? props.snapshot.channels[0]?.channelId;
  const selectedChannel = props.snapshot.channels.find((channel) => channel.channelId === selectedChannelId);
  const consoleOnly = props.clientRole === "console_only" || self?.capabilities.canTransmitAudio === false;
  const experimentalVoice = props.clientRole === "experimental_web_voice";

  return (
    <div className="room-shell">
      <AudioDock
        remoteStreams={props.remoteStreams}
        selectedOutputDeviceId={props.selectedOutputDeviceId}
        onSinkError={props.onAudioOutputSinkError}
      />

      <section className="room-hero">
        <div>
          <span className="eyebrow eyebrow-dark">
            {consoleOnly ? "Console auxiliar" : experimentalVoice ? "Web experimental" : "Sala ativa"}
          </span>
          <h1>{props.snapshot.roomName}</h1>
          <p>
            Codigo <strong>{props.roomCode}</strong> | Modo{" "}
            {props.snapshot.transportMode === "local_lan" ? "Local LAN" : "Compatibilidade"} |{" "}
            {props.connected ? "conectado" : "offline"}
          </p>
          <p className="microcopy">
            Host {props.snapshot.hostStatus}
            {props.snapshot.hostEndpoint ? ` | Endpoint ${props.snapshot.hostEndpoint.baseUrl}` : ""}
          </p>
        </div>
        <div className="status-badges">
          <span className={`status-pill ${props.connected ? "live" : ""}`}>
            {props.connected ? "Conectado" : "Offline"}
          </span>
          <span className={`status-pill ${props.micReady ? "live" : ""}`}>
            {props.micReady ? "Microfone pronto" : "Microfone pendente"}
          </span>
          <span className={`status-pill ${props.isTalking ? "alert" : ""}`}>
            {props.isTalking ? "Transmitindo" : "Escuta"}
          </span>
        </div>
      </section>

      <section className="room-grid">
        <article className="control-panel">
          <h2>Painel da sessao</h2>
          <p className="microcopy">
            Canal atual: <strong>{selectedChannel?.name ?? "Sem canal"}</strong>
          </p>

          <div className="channel-grid">
            {props.snapshot.channels.map((channel) => (
              <button
                key={channel.channelId}
                className={`channel-chip ${selectedChannelId === channel.channelId ? "active" : ""}`}
                onClick={() => props.onSelectChannel(channel.channelId)}
              >
                <span>{channel.name}</span>
                <small>
                  {channel.activeSpeakerPeerId
                    ? props.snapshot.members.find((member) => member.peerId === channel.activeSpeakerPeerId)
                        ?.nickname ?? "Falando"
                    : "Livre"}
                </small>
              </button>
            ))}
          </div>

          {consoleOnly ? (
            <div className="console-note">
              <strong>Este navegador entrou como console auxiliar.</strong>
              <p>
                Ele acompanha participantes, eventos e audio recebido, mas nao e o caminho oficial
                para falar. Use o APK Android para PTT.
              </p>
            </div>
          ) : (
            <>
              <button className="primary-button wide-button" onClick={props.onEnableMic}>
                {props.micReady ? "Revalidar microfone" : "Habilitar microfone"}
              </button>
              {props.canSelectAudioOutput ? (
                <button
                  className="secondary-button wide-button"
                  disabled={props.audioOutputBusy}
                  onClick={props.onSelectAudioOutput}
                >
                  {props.audioOutputBusy
                    ? "Abrindo seletor de audio..."
                    : `Saida de audio: ${props.audioOutputLabel}`}
                </button>
              ) : null}
              <p className="microcopy">{props.audioOutputMessage}</p>
              <button
                className={`ptt-button ${props.isTalking ? "pressed" : ""}`}
                disabled={!props.micReady}
                onMouseDown={props.onPressToTalkStart}
                onMouseUp={props.onPressToTalkEnd}
                onMouseLeave={props.onPressToTalkEnd}
                onTouchStart={props.onPressToTalkStart}
                onTouchEnd={props.onPressToTalkEnd}
              >
                <span>Aperte para falar</span>
                <small>{props.notice}</small>
              </button>
            </>
          )}
        </article>

        <article className="info-card">
          <h2>Participantes</h2>
          <div className="member-list">
            {props.snapshot.members.map((member) => (
              <article key={member.peerId} className="member-row">
                <div>
                  <strong>{member.nickname}</strong>
                  <span>
                    {props.snapshot.channels.find((channel) => channel.channelId === member.selectedChannelId)?.name}
                  </span>
                </div>
                <span className={`member-badge ${member.isConnected ? "live" : ""}`}>
                  {member.isHost ? "Host" : member.role}
                </span>
              </article>
            ))}
          </div>
        </article>
      </section>

      <section className="info-card">
        <h2>Eventos recentes</h2>
        <EventList events={props.snapshot.eventLog} />
      </section>
    </div>
  );
}
