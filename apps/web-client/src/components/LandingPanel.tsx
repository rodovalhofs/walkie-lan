import { DEFAULT_CHANNELS } from "@walkie/protocol";
import { useMemo, useState } from "react";

interface LandingPanelProps {
  serverUrl: string;
  setServerUrl: (value: string) => void;
  nickname: string;
  setNickname: (value: string) => void;
  onCreateRoom: (payload: { roomName: string; channelNames: string[] }) => Promise<void>;
  onJoinRoom: (payload: { roomCode: string }) => Promise<void>;
  busy: boolean;
  error: string | null;
}

export function LandingPanel(props: LandingPanelProps) {
  const [roomName, setRoomName] = useState("Equipe LAN");
  const [roomCode, setRoomCode] = useState("");
  const [channelsInput, setChannelsInput] = useState(DEFAULT_CHANNELS.join(", "));
  const channelPreview = useMemo(
    () =>
      channelsInput
        .split(",")
        .map((entry) => entry.trim())
        .filter(Boolean)
        .slice(0, 8),
    [channelsInput],
  );

  return (
    <div className="shell-grid">
      <section className="hero-card">
        <span className="eyebrow">Walkie-Talkie LAN Hibrido</span>
        <h1>Android hospeda. iPhone entra pelo navegador.</h1>
        <p>
          Fluxo local para operacao em Wi-Fi com codigo curto, canais fixos, PTT e audio em
          tempo real por WebRTC.
        </p>
        <ul className="feature-strip">
          <li>Codigo curto para entrada</li>
          <li>Web app com foco em Safari/iOS</li>
          <li>PTT com host autoritativo</li>
        </ul>
      </section>

      <section className="control-card">
        <label>
          <span>URL do servidor</span>
          <input
            value={props.serverUrl}
            onChange={(event) => props.setServerUrl(event.target.value)}
            placeholder="https://seu-servidor"
          />
        </label>
        <label>
          <span>Seu apelido</span>
          <input
            value={props.nickname}
            onChange={(event) => props.setNickname(event.target.value)}
            placeholder="Ana"
          />
        </label>
        <div className="action-columns">
          <div className="action-pane">
            <h2>Criar sala</h2>
            <label>
              <span>Nome da sala</span>
              <input value={roomName} onChange={(event) => setRoomName(event.target.value)} />
            </label>
            <label>
              <span>Canais</span>
              <textarea
                rows={4}
                value={channelsInput}
                onChange={(event) => setChannelsInput(event.target.value)}
              />
            </label>
            <p className="microcopy">Canais previstos: {channelPreview.join(" / ")}</p>
            <button
              className="primary-button"
              disabled={props.busy}
              onClick={() => props.onCreateRoom({ roomName, channelNames: channelPreview })}
            >
              Host no navegador
            </button>
            <p className="microcopy">
              Modo de debug para validar o ecossistema antes do APK Android final.
            </p>
          </div>

          <div className="action-pane">
            <h2>Entrar por codigo</h2>
            <label>
              <span>Codigo da sala</span>
              <input
                value={roomCode}
                onChange={(event) => setRoomCode(event.target.value.toUpperCase())}
                placeholder="AB12CD"
                maxLength={8}
              />
            </label>
            <button
              className="secondary-button"
              disabled={props.busy}
              onClick={() => props.onJoinRoom({ roomCode })}
            >
              Entrar da web app
            </button>
            <p className="microcopy">
              Ideal para Safari no iPhone com pagina em HTTPS e uso em primeiro plano.
            </p>
          </div>
        </div>
        {props.error ? <p className="error-banner">{props.error}</p> : null}
      </section>
    </div>
  );
}

