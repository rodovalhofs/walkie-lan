# Estrutura do Repositorio

## Visao geral

```text
apps/
  android-host/
  signaling-server/
  web-client/
packages/
  protocol/
docs/
  wiki/
```

## apps/android-host

App Android em Kotlin com Jetpack Compose.

Arquivos principais:

- `app/src/main/java/com/example/walkielan/MainActivity.kt`
- `app/src/main/java/com/example/walkielan/MainViewModel.kt`
- `app/src/main/java/com/example/walkielan/ui/WalkieScreen.kt`
- `app/src/main/java/com/example/walkielan/ui/UiState.kt`
- `app/src/main/java/com/example/walkielan/network/SignalingApi.kt`
- `app/src/main/java/com/example/walkielan/network/SignalingSocket.kt`
- `app/src/main/java/com/example/walkielan/rtc/WebRtcController.kt`
- `app/src/main/java/com/example/walkielan/data/ProtocolModels.kt`
- `app/src/main/java/com/example/walkielan/service/WalkieSessionService.kt`

## apps/signaling-server

Servidor Node.js para REST e WebSocket.

Arquivos principais:

- `src/index.ts`
- `src/roomRegistry.ts`
- `src/config.ts`
- `tests/roomRegistry.test.ts`

## apps/web-client

Web app em React + Vite.

Arquivos principais:

- `src/main.tsx`
- `src/App.tsx`
- `src/styles.css`
- `src/components/LandingPanel.tsx`
- `src/components/RoomPanel.tsx`
- `src/lib/api.ts`
- `src/lib/signalingClient.ts`
- `src/lib/meshManager.ts`
- `vite.config.ts`

## packages/protocol

Pacote compartilhado entre web e servidor.

Arquivo principal:

- `src/index.ts`

## docs/wiki

Fonte em Markdown da wiki em portugues. O objetivo e manter a documentacao versionada no proprio repositorio e espelhar esse conteudo na Wiki do GitHub.

## Arquivos de raiz importantes

- `README.md`
- `CONTRIBUTING.md`
- `CODE_OF_CONDUCT.md`
- `LICENSE`
- `package.json`
- `tsconfig.base.json`
