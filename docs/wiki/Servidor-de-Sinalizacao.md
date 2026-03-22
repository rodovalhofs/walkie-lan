# Servidor de Sinalizacao

## Objetivo

O servidor atual e pequeno e intencionalmente simples. Ele existe para unir Android e web em torno de:

- criacao de sala
- codigo curto
- autenticacao inicial
- WebSocket de sessao
- retransmissao de sinalizacao

## Arquivos principais

### `src/index.ts`

Ponto de entrada do servidor.

Responsabilidades:

- subir Express
- configurar CORS
- habilitar JSON
- expor endpoints REST
- subir `WebSocketServer`
- validar mensagens de socket com `socketMessageSchema`

### `src/roomRegistry.ts`

Estado em memoria do servidor.

Responsabilidades:

- criar sala
- entrar em sala
- autenticar peers
- anexar e desanexar socket
- manter peers e canais
- manter `activeSpeakerByChannel`
- manter `talkLocks`
- manter log de eventos
- encaminhar mensagens entre peers

### `src/config.ts`

Le variaveis de ambiente:

- `PORT`
- `PUBLIC_HTTP_BASE_URL`
- `PUBLIC_WS_BASE_URL`

## Endpoints REST

### `GET /health`

Retorna:

```json
{ "ok": true }
```

### `POST /api/rooms`

Cria uma sala.

Payload:

- `roomName`
- `channelNames`
- `hostDeviceId`
- `hostNickname`

Retorno:

- `roomId`
- `roomCode`
- `expiresAt`
- `hostSessionToken`
- `hostPeerId`
- `wsUrl`

### `POST /api/rooms/join`

Entra em uma sala existente.

Payload:

- `roomCode`
- `nickname`
- `clientType`
- `deviceId`

Retorno:

- `roomId`
- `peerId`
- `peerToken`
- `wsUrl`
- `snapshot`

### `GET /api/rooms/:roomCode`

Retorna o snapshot atual da sala.

## Fluxo do WebSocket

1. O cliente conecta em `/ws`
2. A primeira mensagem precisa ser `hello`
3. O servidor autentica `roomId`, `peerId` e `token`
4. Depois disso, o servidor aceita mensagens normais da sessao

Se a primeira mensagem nao for `hello`, o servidor responde com erro e fecha o socket.

## Regras de negocio atuais

- o host precisa estar online para alguem entrar
- a sala tem limite maximo definido pelo protocolo
- quando o host desconecta, a sala e encerrada
- participantes comuns sao removidos da sala quando desconectam
- so o host pode enviar mensagens como `talk_granted`, `talk_denied`, `talk_released`, `sync_snapshot` e `room_closed`

## Observacao importante

O estado da sala esta em memoria. Isso significa:

- reiniciar o servidor perde todas as salas
- nao existe persistencia em banco ainda
