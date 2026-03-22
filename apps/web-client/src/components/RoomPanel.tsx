import type { EventEntry, RoomSnapshot } from "@walkie/protocol";
import { useEffect, useRef } from "react";

interface RoomPanelProps {
  snapshot: RoomSnapshot;
  selfPeerId: string;
  roomCode: string;
  connected: boolean;
  micReady: boolean;
  isTalking: boolean;
  onEnableMic: () => Promise<void>;
  onSelectChannel: (channelId: string) => void;
  onPressToTalkStart: () => void;
  onPressToTalkEnd: () => void;
  remoteStreams: Map<string, MediaStream>;
  notice: string;
}

function AudioDock(props: { remoteStreams: Map<string, MediaStream> }) {
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
    }
    for (const audio of container.querySelectorAll<HTMLAudioElement>("audio")) {
      if (!knownIds.has(audio.dataset.peer ?? "")) {
        audio.remove();
      }
    }
  }, [props.remoteStreams]);

  return <div ref={containerRef} className="audio-dock" aria-hidden="true" />;
}

function EventList(props: { events: EventEntry[] }) {
  return (
    <div className="event-list">
      {props.events
        .slice()
        .reverse()
        .slice(0, 8)
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

  return (
    <div className="room-layout">
      <AudioDock remoteStreams={props.remoteStreams} />

      <section className="room-header-card">
        <div>
          <span className="eyebrow">Sala ativa</span>
          <h1>{props.snapshot.roomName}</h1>
          <p>
            Codigo <strong>{props.roomCode}</strong> · Host {props.snapshot.hostStatus} ·{" "}
            {props.connected ? "conectado" : "offline"}
          </p>
        </div>
        <div className="status-pill-group">
          <span className={`status-pill ${props.micReady ? "live" : ""}`}>
            {props.micReady ? "Microfone pronto" : "Microfone pendente"}
          </span>
          <span className={`status-pill ${props.isTalking ? "alert" : ""}`}>
            {props.isTalking ? "Transmitindo" : "Escuta"}
          </span>
        </div>
      </section>

      <div className="room-columns">
        <section className="ptt-card">
          <div className="channel-strip">
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

          <button className="primary-button wide-button" onClick={props.onEnableMic}>
            {props.micReady ? "Revalidar microfone" : "Habilitar microfone"}
          </button>

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
        </section>

        <section className="presence-card">
          <h2>Presenca</h2>
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
                  {member.isHost ? "Host" : member.clientType}
                </span>
              </article>
            ))}
          </div>
        </section>
      </div>

      <section className="event-card">
        <h2>Eventos recentes</h2>
        <EventList events={props.snapshot.eventLog} />
      </section>
    </div>
  );
}
