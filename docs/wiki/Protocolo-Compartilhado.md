# Protocolo Compartilhado

## Onde ele vive

O protocolo compartilhado fica em `packages/protocol/src/index.ts`.

## Por que ele existe

Ele centraliza os contratos usados por:

- web
- servidor

No Android, os contratos equivalentes ficam em `ProtocolModels.kt`.

## Tipos centrais

### `CreateRoomRequest`

- `roomName`
- `channelNames`
- `hostDeviceId`
- `hostNickname`

### `RoomCodeReservation`

- `roomId`
- `roomCode`
- `expiresAt`
- `hostSessionToken`
- `hostPeerId`
- `wsUrl`

### `JoinRoomRequest`

- `roomCode`
- `nickname`
- `clientType`
- `deviceId`

### `PeerState`

- `peerId`
- `nickname`
- `clientType`
- `deviceId`
- `selectedChannelId`
- `isHost`
- `isConnected`
- `joinedAt`
- `lastSeenAt`

### `ChannelState`

- `channelId`
- `name`
- `activeSpeakerPeerId`
- `queueVersion`

### `EventEntry`

- `eventId`
- `roomId`
- `channelId`
- `peerId`
- `type`
- `occurredAt`
- `summary`

### `RoomSnapshot`

- `roomId`
- `roomName`
- `roomCode`
- `channels`
- `members`
- `activeSpeakerByChannel`
- `hostStatus`
- `eventLog`
- `capacity`

### `SignalEnvelope`

- `roomId`
- `peerId`
- `targetPeerId`
- `type`
- `sdp`
- `iceCandidate`

Os campos `sdp` e `iceCandidate` hoje aceitam `null` ou ausencia, para reduzir incompatibilidades entre implementacoes.

## Mensagens de socket

O protocolo hoje define:

- `hello`
- `room_snapshot`
- `peer_joined`
- `peer_left`
- `channel_select`
- `talk_request`
- `talk_release_request`
- `talk_granted`
- `talk_denied`
- `talk_released`
- `signal`
- `event`
- `room_closed`
- `sync_snapshot`
- `error`

## Defaults

- canais padrao: `Geral`, `Operacao`, `Suporte`
- capacidade da sala: `10`
- limite do log de eventos: `200`

## Beneficios dessa camada

- menos divergencia entre web e servidor
- validacao forte com Zod
- evolucao mais segura do protocolo
- base clara para expandir suporte a outros clientes
