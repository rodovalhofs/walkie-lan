import { DEFAULT_CHANNELS } from "@walkie/protocol";
import { useMemo, useState } from "react";

interface LandingPanelProps {
  nickname: string;
  setNickname: (value: string) => void;
  advancedOpen: boolean;
  onToggleAdvanced: () => void;
  serverUrl: string;
  setServerUrl: (value: string) => void;
  onCreateRoom: (payload: { roomName: string; channelNames: string[] }) => Promise<void>;
  onJoinRoom: (payload: { roomCode: string; clientType: "ios_web" | "android_web_debug" }) => Promise<void>;
  busy: boolean;
  error: string | null;
  statusMessage: string;
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
    <div className="site-shell">
      <section className="hero-panel">
        <div className="hero-copy">
          <span className="eyebrow">Walkie LAN open source</span>
          <h1>Android protagonista. Navegador como apoio leve.</h1>
          <p>
            O caminho oficial agora e local-first: o APK Android cria a sala, anuncia na LAN e
            compartilha codigo curto e QR para o console auxiliar.
          </p>
          <div className="cta-row">
            <button className="primary-button" onClick={props.onToggleAdvanced}>
              {props.advancedOpen ? "Fechar laboratorio avancado" : "Abrir laboratorio avancado"}
            </button>
            <a className="secondary-link" href="https://github.com/rodovalhofs/walkie-lan/wiki">
              Ler a wiki do projeto
            </a>
          </div>
        </div>

        <div className="hero-grid">
          <article className="info-card feature-card">
            <strong>1. Crie no Android</strong>
            <p>O Android vira o host oficial da sala, mostra o codigo e gera o QR.</p>
          </article>
          <article className="info-card feature-card">
            <strong>2. Entre na rede local</strong>
            <p>Outro Android descobre a sala por LAN. O navegador acompanha por QR ou link local.</p>
          </article>
          <article className="info-card feature-card">
            <strong>3. Use web como console</strong>
            <p>A web mostra status, participantes e eventos. Voz web fica marcada como experimental.</p>
          </article>
        </div>
      </section>

      <section className="public-grid">
        <article className="info-card">
          <h2>Fluxo recomendado</h2>
          <ol className="steps-list">
            <li>Abra o APK Android e toque em <strong>Criar sala</strong>.</li>
            <li>Mostre o codigo ou QR da sala para quem vai acompanhar.</li>
            <li>Use outro Android para entrar por descoberta LAN.</li>
            <li>Use iPhone ou desktop para abrir o console auxiliar no navegador.</li>
          </ol>
        </article>

        <article className="info-card">
          <h2>Perfil local</h2>
          <label>
            <span>Apelido do operador</span>
            <input
              value={props.nickname}
              onChange={(event) => props.setNickname(event.target.value)}
              placeholder="Operador"
            />
          </label>
          <p className="status-line">{props.statusMessage}</p>
          <p className="microcopy">
            No fluxo simples, voce nao precisa digitar URL de servidor. Isso ficou isolado no modo
            avancado.
          </p>
        </article>

        <article className="info-card">
          <h2>Console auxiliar</h2>
          <p>
            O console local e servido pelo proprio Android host. O jeito ideal de abrir e por QR
            code ou pelo link local mostrado no APK.
          </p>
          <p className="microcopy">
            Se voce estiver no iPhone, abra o link local no Safari. Para falar, prefira o APK
            Android. A voz web continua experimental.
          </p>
        </article>
      </section>

      {props.advancedOpen ? (
        <section className="advanced-panel">
          <div className="advanced-header">
            <span className="eyebrow eyebrow-dark">Modo avancado</span>
            <h2>Laboratorio de compatibilidade e debug</h2>
            <p>
              Aqui ficam o fluxo legado com URL manual, o host web experimental e os testes de
              console/voz fora do caminho principal.
            </p>
          </div>

          <div className="advanced-grid">
            <article className="advanced-card">
              <h3>Conexao manual</h3>
              <label>
                <span>URL do servidor</span>
                <input
                  value={props.serverUrl}
                  onChange={(event) => props.setServerUrl(event.target.value)}
                  placeholder="http://192.168.0.15:8787"
                />
              </label>
              <p className="microcopy">
                Use esta URL apenas em debug, integracao remota ou quando nao houver descoberta
                local.
              </p>
            </article>

            <article className="advanced-card">
              <h3>Host web experimental</h3>
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
                className="secondary-button"
                disabled={props.busy}
                onClick={() => props.onCreateRoom({ roomName, channelNames: channelPreview })}
              >
                Host web experimental
              </button>
            </article>

            <article className="advanced-card">
              <h3>Entrar por codigo</h3>
              <label>
                <span>Codigo da sala</span>
                <input
                  value={roomCode}
                  onChange={(event) => setRoomCode(event.target.value.toUpperCase())}
                  placeholder="AB12CD"
                  maxLength={8}
                />
              </label>
              <div className="stack-buttons">
                <button
                  className="primary-button"
                  disabled={props.busy}
                  onClick={() => props.onJoinRoom({ roomCode, clientType: "ios_web" })}
                >
                  Entrar como console
                </button>
                <button
                  className="secondary-button"
                  disabled={props.busy}
                  onClick={() => props.onJoinRoom({ roomCode, clientType: "android_web_debug" })}
                >
                  Entrar como web experimental
                </button>
              </div>
              <p className="microcopy">
                Console acompanha a sala sem prometer fala. O modo web experimental tenta audio via
                WebRTC quando o navegador permitir.
              </p>
            </article>
          </div>

          {props.error ? <p className="error-banner">{props.error}</p> : null}
        </section>
      ) : null}
    </div>
  );
}
