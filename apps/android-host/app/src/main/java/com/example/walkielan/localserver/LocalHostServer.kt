package com.example.walkielan.localserver

import com.example.walkielan.data.CreateRoomRequest
import com.example.walkielan.data.ErrorMessage
import com.example.walkielan.data.HelloMessage
import com.example.walkielan.data.HostEndpoint
import com.example.walkielan.data.JoinRoomRequest
import com.example.walkielan.data.LocalPairingPayload
import com.example.walkielan.data.RoomSnapshotMessage
import com.example.walkielan.data.SocketMessage
import com.example.walkielan.data.TransportMode
import com.example.walkielan.data.WalkieJson
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import java.io.IOException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class LocalHostServer(
    port: Int,
    private val hostEndpointProvider: () -> HostEndpoint,
) : NanoWSD(port) {
    private val registry = LocalRoomRegistry()

    @Throws(IOException::class)
    fun startServer() {
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
    }

    override fun serveHttp(session: IHTTPSession): Response {
        if (session.method == Method.OPTIONS) {
            return respond(Response.Status.OK, "application/json", """{"ok":true}""")
        }

        return try {
            when {
                session.method == Method.GET && session.uri == "/health" -> {
                    respond(Response.Status.OK, "application/json", """{"ok":true}""")
                }

                session.method == Method.POST && session.uri == "/api/rooms" -> {
                    val payload = WalkieJson.instance.decodeFromString<CreateRoomRequest>(readBody(session))
                    val endpoint = hostEndpointProvider()
                    val reservation = registry.createRoom(payload, wsUrlFor(session), endpoint)
                    respond(Response.Status.CREATED, "application/json", WalkieJson.instance.encodeToString(reservation))
                }

                session.method == Method.POST && session.uri == "/api/rooms/join" -> {
                    val payload = WalkieJson.instance.decodeFromString<JoinRoomRequest>(readBody(session))
                    val endpoint = hostEndpointProvider()
                    val join = registry.joinRoom(payload, wsUrlFor(session), endpoint)
                    respond(Response.Status.OK, "application/json", WalkieJson.instance.encodeToString(join))
                }

                session.method == Method.GET && session.uri.startsWith("/api/rooms/") -> {
                    val roomCode = session.uri.substringAfterLast('/').uppercase()
                    val snapshot = registry.getSnapshotByCode(roomCode)
                    respond(Response.Status.OK, "application/json", WalkieJson.instance.encodeToString(snapshot))
                }

                session.method == Method.GET && session.uri == "/api/pairing" -> {
                    val roomCode = session.parameters["roomCode"]?.firstOrNull()?.uppercase()
                        ?: return respond(Response.Status.BAD_REQUEST, "application/json", """{"error":"roomCode obrigatorio."}""")
                    val snapshot = registry.getSnapshotByCode(roomCode)
                    val endpoint = snapshot.hostEndpoint ?: hostEndpointProvider()
                    val payload = LocalPairingPayload(
                        roomId = snapshot.roomId,
                        roomCode = snapshot.roomCode,
                        roomName = snapshot.roomName,
                        hostAddress = endpoint.hostAddress,
                        port = endpoint.port,
                        protocolVersion = PROTOCOL_VERSION,
                        transportMode = TransportMode.LOCAL_LAN,
                        roomCapabilities = snapshot.roomCapabilities,
                        consoleUrl = endpoint.consoleUrl ?: "${endpoint.baseUrl}/console?roomCode=${snapshot.roomCode}",
                    )
                    respond(Response.Status.OK, "application/json", WalkieJson.instance.encodeToString(payload))
                }

                session.method == Method.GET && session.uri == "/console" -> {
                    val roomCode = session.parameters["roomCode"]?.firstOrNull()?.uppercase().orEmpty()
                    respond(Response.Status.OK, "text/html; charset=utf-8", consoleHtml(roomCode))
                }

                else -> respond(Response.Status.NOT_FOUND, "application/json", """{"error":"Nao encontrado."}""")
            }
        } catch (error: Throwable) {
            respond(
                Response.Status.BAD_REQUEST,
                "application/json",
                WalkieJson.instance.encodeToString(ErrorMessage(error.message ?: "Erro inesperado.")),
            )
        }
    }

    override fun openWebSocket(handshake: IHTTPSession): WebSocket {
        return object : WebSocket(handshake), LocalSocketLike {
            private var connection: AuthenticatedPeer? = null

            override fun canSend(): Boolean = true

            override fun onOpen() = Unit

            override fun onClose(code: WebSocketFrame.CloseCode?, reason: String?, initiatedByRemote: Boolean) {
                connection?.let { registry.detachSocket(it.roomId, it.peerId) }
            }

            override fun onMessage(message: WebSocketFrame) {
                try {
                    val text = message.textPayload
                    val parsed = WalkieJson.instance.decodeFromString<SocketMessage>(text)
                    if (connection == null) {
                        if (parsed !is HelloMessage) {
                            send(WalkieJson.instance.encodeToString(ErrorMessage("Mensagem hello obrigatoria.")))
                            close(WebSocketFrame.CloseCode.PolicyViolation, "hello obrigatorio", false)
                            return
                        }
                        val auth = registry.authenticate(parsed.roomId, parsed.peerId, parsed.token)
                        connection = auth
                        registry.attachSocket(auth.roomId, auth.peerId, this)
                        return
                    }
                    registry.handleMessage(connection!!.peerId, parsed)
                } catch (error: Throwable) {
                    send(WalkieJson.instance.encodeToString(ErrorMessage(error.message ?: "Erro inesperado.")))
                }
            }

            override fun onPong(frame: WebSocketFrame?) = Unit

            override fun onException(exception: IOException) = Unit
        }
    }

    private fun readBody(session: IHTTPSession): String {
        val files = mutableMapOf<String, String>()
        session.parseBody(files)
        return files["postData"].orEmpty()
    }

    private fun wsUrlFor(session: IHTTPSession): String {
        val host = session.headers["host"] ?: "127.0.0.1:$listeningPort"
        return "ws://$host/ws"
    }

    private fun respond(status: Response.Status, mimeType: String, body: String): Response {
        return newFixedLengthResponse(status, mimeType, body).apply {
            addHeader("Access-Control-Allow-Origin", "*")
            addHeader("Access-Control-Allow-Headers", "origin,accept,content-type")
            addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
            addHeader("Cache-Control", "no-store")
        }
    }

    private fun consoleHtml(roomCode: String): String {
        val safeRoomCode = roomCode.ifBlank { "SALA" }
        return """
            <!doctype html>
            <html lang="pt-BR">
              <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>Walkie LAN Console</title>
                <style>
                  :root { color-scheme: light; font-family: "Segoe UI", sans-serif; background: #f4efe6; color: #112031; }
                  body { margin: 0; background: radial-gradient(circle at top right, rgba(217,125,84,.24), transparent 30%), linear-gradient(180deg, #f5f0e8 0%, #e7dccd 100%); }
                  main { max-width: 920px; margin: 0 auto; padding: 20px; display: grid; gap: 16px; }
                  .card { background: rgba(255,255,255,.88); border-radius: 24px; padding: 20px; box-shadow: 0 20px 40px rgba(17,32,49,.08); }
                  .eyebrow { display: inline-block; padding: 8px 12px; border-radius: 999px; background: #112031; color: #f5f0e8; text-transform: uppercase; letter-spacing: .08em; font-size: 12px; }
                  h1 { margin: 12px 0 8px; font-size: clamp(2rem, 5vw, 3rem); line-height: .95; }
                  .meta { color: rgba(17,32,49,.72); }
                  .grid { display: grid; gap: 12px; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); }
                  .list { display: grid; gap: 10px; }
                  .row { display: flex; justify-content: space-between; gap: 12px; padding: 12px 14px; background: rgba(17,32,49,.05); border-radius: 16px; }
                  .warn { color: #8d411f; }
                </style>
              </head>
              <body>
                <main>
                  <section class="card">
                    <span class="eyebrow">Console Auxiliar</span>
                    <h1 id="roomName">Carregando sala...</h1>
                    <p class="meta" id="meta">Codigo <strong>$safeRoomCode</strong></p>
                    <p class="warn">Use o APK Android para falar. Esta pagina acompanha a sala local em tempo real.</p>
                  </section>
                  <section class="grid">
                    <article class="card">
                      <h2>Status</h2>
                      <div class="list" id="statusList"></div>
                    </article>
                    <article class="card">
                      <h2>Participantes</h2>
                      <div class="list" id="membersList"></div>
                    </article>
                  </section>
                  <section class="card">
                    <h2>Eventos recentes</h2>
                    <div class="list" id="eventsList"></div>
                  </section>
                </main>
                <script>
                  const roomCode = new URLSearchParams(window.location.search).get("roomCode") || "$safeRoomCode";
                  const roomNameEl = document.getElementById("roomName");
                  const metaEl = document.getElementById("meta");
                  const statusList = document.getElementById("statusList");
                  const membersList = document.getElementById("membersList");
                  const eventsList = document.getElementById("eventsList");
                  function row(left, right) {
                    const div = document.createElement("div");
                    div.className = "row";
                    div.innerHTML = `<span>${'$'}{left}</span><strong>${'$'}{right}</strong>`;
                    return div;
                  }
                  async function refresh() {
                    const response = await fetch(`/api/rooms/${'$'}{roomCode}`);
                    if (!response.ok) {
                      roomNameEl.textContent = "Sala indisponivel";
                      metaEl.textContent = "Nao foi possivel encontrar a sala local.";
                      return;
                    }
                    const snapshot = await response.json();
                    roomNameEl.textContent = snapshot.roomName;
                    metaEl.innerHTML = `Codigo <strong>${'$'}{snapshot.roomCode}</strong> | Host ${'$'}{snapshot.hostStatus} | ${'$'}{snapshot.members.length}/${'$'}{snapshot.capacity} participantes`;
                    statusList.innerHTML = "";
                    statusList.appendChild(row("Modo", snapshot.transportMode === "local_lan" ? "Local LAN" : snapshot.transportMode));
                    statusList.appendChild(row("Host", snapshot.hostStatus));
                    statusList.appendChild(row("Endpoint", snapshot.hostEndpoint?.baseUrl || window.location.origin));
                    membersList.innerHTML = "";
                    snapshot.members.forEach((member) => {
                      const channel = snapshot.channels.find((item) => item.channelId === member.selectedChannelId)?.name || "Sem canal";
                      membersList.appendChild(row(member.nickname, `${'$'}{channel} | ${'$'}{member.isHost ? "Host" : member.role}`));
                    });
                    if (snapshot.members.length === 0) {
                      membersList.appendChild(row("Nenhum participante", "Aguardando"));
                    }
                    eventsList.innerHTML = "";
                    snapshot.eventLog.slice().reverse().slice(0, 10).forEach((event) => {
                      eventsList.appendChild(row(event.summary, new Date(event.occurredAt).toLocaleTimeString("pt-BR")));
                    });
                    if (snapshot.eventLog.length === 0) {
                      eventsList.appendChild(row("Sem eventos ainda", "Aguardando"));
                    }
                  }
                  refresh();
                  setInterval(refresh, 1500);
                </script>
              </body>
            </html>
        """.trimIndent()
    }
}
