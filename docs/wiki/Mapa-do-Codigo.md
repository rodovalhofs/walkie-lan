# Mapa do Codigo

Esta pagina resume o papel dos arquivos mais importantes do projeto.

## Android

- `apps/android-host/app/src/main/java/com/example/walkielan/MainActivity.kt`
  Ponto de entrada da app Android.
- `apps/android-host/app/src/main/java/com/example/walkielan/MainViewModel.kt`
  Estado principal da app, rede, socket, PTT e WebRTC.
- `apps/android-host/app/src/main/java/com/example/walkielan/ui/WalkieScreen.kt`
  UI Compose de setup e sala ativa.
- `apps/android-host/app/src/main/java/com/example/walkielan/ui/UiState.kt`
  Modelo de estado observado pela UI.
- `apps/android-host/app/src/main/java/com/example/walkielan/network/SignalingApi.kt`
  Chamadas REST de criar sala e entrar.
- `apps/android-host/app/src/main/java/com/example/walkielan/network/SignalingSocket.kt`
  Conexao WebSocket e envio da mensagem `hello`.
- `apps/android-host/app/src/main/java/com/example/walkielan/data/ProtocolModels.kt`
  Modelos serializaveis equivalentes ao protocolo.
- `apps/android-host/app/src/main/java/com/example/walkielan/rtc/WebRtcController.kt`
  Peer connections, trilha local e renegociacao.
- `apps/android-host/app/src/main/java/com/example/walkielan/service/WalkieSessionService.kt`
  Notificacao persistente de sessao ativa.
- `apps/android-host/app/src/main/AndroidManifest.xml`
  Permissoes e declaracao do service.
- `apps/android-host/app/src/main/res/xml/network_security_config.xml`
  Regras de rede para desenvolvimento local.

## Web

- `apps/web-client/src/main.tsx`
  Bootstrap React e registro do service worker.
- `apps/web-client/src/App.tsx`
  Estado central da web app.
- `apps/web-client/src/components/LandingPanel.tsx`
  Tela inicial.
- `apps/web-client/src/components/RoomPanel.tsx`
  Tela de sala.
- `apps/web-client/src/lib/api.ts`
  Cliente REST.
- `apps/web-client/src/lib/signalingClient.ts`
  Cliente WebSocket.
- `apps/web-client/src/lib/meshManager.ts`
  Malha WebRTC com `simple-peer`.
- `apps/web-client/src/styles.css`
  Estilo principal da web app.
- `apps/web-client/vite.config.ts`
  Configuracao Vite e PWA.

## Servidor

- `apps/signaling-server/src/index.ts`
  Express, REST, WebSocket e wiring principal.
- `apps/signaling-server/src/roomRegistry.ts`
  Regras de sessao em memoria.
- `apps/signaling-server/src/config.ts`
  Configuracao via ambiente.
- `apps/signaling-server/tests/roomRegistry.test.ts`
  Testes da logica da sala.

## Protocolo

- `packages/protocol/src/index.ts`
  Schemas Zod, tipos e mensagens.

## Documentacao

- `README.md`
  Entrada principal do repositorio.
- `docs/wiki/*.md`
  Fonte da wiki em portugues.
